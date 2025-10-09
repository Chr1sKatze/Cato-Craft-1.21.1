package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    private static final TagKey<Item> GATHERING_TOOLS_TAG =
            TagKey.create(Registries.ITEM, CatocraftMod.id("gathering_tools"));

    // 🧩 Easy tuning constants
    private static final double BASE_RADIUS = 1.0;
    private static final double MAX_RADIUS = 6.0;
    private static final double BASE_SPEED = 0.03;
    private static final double MAX_SPEED = 0.03;
    private static final double DAMPING = 0.9;
    private static final double SMOOTHING = 0.2;

    // 🌙 How long items hover after attraction stops (in ticks)
    private static final int GRAVITY_FADE_TICKS = 20;

    private static long lastLogTime = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void attractionTick(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long now = level.getGameTime();
        boolean shouldLog = (now - lastLogTime) >= 20;
        if (shouldLog) lastLogTime = now;

        for (Player player : level.players()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || !mainHand.is(GATHERING_TOOLS_TAG)) continue;

            // Get enchantment level
            int enchantmentLevel = 0;
            try {
                Holder<Enchantment> attractionHolder = (Holder<Enchantment>) level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(ModEnchantments.ATTRACTION.getKey());
                enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(attractionHolder, mainHand);
            } catch (Exception e) {
                if (shouldLog)
                    CatocraftMod.LOGGER.warn("[AttractionMixin] Attraction enchantment not found: {}", e.getMessage());
                continue;
            }
            if (enchantmentLevel <= 0) continue;

            double radius = Math.min(BASE_RADIUS + enchantmentLevel * 0.5, MAX_RADIUS);
            double baseSpeed = Math.min(BASE_SPEED + 0.02 * enchantmentLevel, MAX_SPEED);

            AABB searchArea = player.getBoundingBox().inflate(radius + 2.0);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchArea);

            for (ItemEntity item : items) {
                if (!item.getPersistentData().hasUUID("CatocraftMiner")) continue;
                UUID owner = item.getPersistentData().getUUID("CatocraftMiner");
                if (!owner.equals(player.getUUID())) continue;

                Vec3 toPlayer = new Vec3(
                        player.getX() - item.getX(),
                        (player.getY() + 1.0) - item.getY(),
                        player.getZ() - item.getZ()
                );
                double distance = toPlayer.length();

                if (distance > radius || item.isRemoved()) {
                    // Gradual gravity fade
                    int fade = item.getPersistentData().getInt("CatocraftGravityFade");
                    if (fade > 0) {
                        fade--;
                        item.getPersistentData().putInt("CatocraftGravityFade", fade);
                        item.setNoGravity(true);
                    } else {
                        item.setNoGravity(false);
                    }
                    continue;
                }

                // Item is actively being attracted
                double speed = baseSpeed + (distance / radius) * baseSpeed;
                Vec3 direction = toPlayer.normalize();
                Vec3 desiredMotion = direction.scale(speed);
                Vec3 newMotion = item.getDeltaMovement().scale(DAMPING).add(desiredMotion.scale(SMOOTHING));

                item.setDeltaMovement(newMotion);
                item.hasImpulse = true;
                item.setNoGravity(true);
                item.getPersistentData().putInt("CatocraftGravityFade", GRAVITY_FADE_TICKS);

                if (shouldLog) {
                    CatocraftMod.LOGGER.debug("[AttractionMixin] Item {} -> Player {} | dist {:.2f} | motion {}",
                            item, player.getName().getString(), distance, newMotion);
                }
            }
        }

        if (shouldLog)
            CatocraftMod.LOGGER.debug("[AttractionMixin] Processed item attraction tick");
    }
}