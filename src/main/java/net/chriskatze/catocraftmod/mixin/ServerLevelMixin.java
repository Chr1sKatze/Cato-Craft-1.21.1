package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.util.ModTags;
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

    // 🧩 Constants for attraction behavior
    private static final double BASE_RADIUS = 1.0;
    private static final double MAX_RADIUS = 6.0;
    private static final double BASE_SPEED = 0.05;
    private static final double MAX_SPEED = 1.00;
    private static final double DAMPING = 0.9;
    private static final double SMOOTHING = 0.2;
    private static final int GRAVITY_FADE_TICKS = 20;

    private static long lastLogTime = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void attractionTick(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long now = level.getGameTime();
        boolean shouldLogTick = (now - lastLogTime) >= 20;
        if (shouldLogTick) lastLogTime = now;

        // Flattened HolderSet of all attraction items (tools + swords)
        var attractionItems = ModTags.getAttractionItemsHolder();

        for (Player player : level.players()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty()) continue;

            // Skip if player’s held item is not in attraction items
            boolean isAttractionItem = attractionItems.stream()
                    .anyMatch(holder -> holder.value() == mainHand.getItem());
            if (!isAttractionItem) continue;

            // Get the enchantment level
            int enchantmentLevel = 0;
            try {
                Holder<Enchantment> attractionHolder = (Holder<Enchantment>) level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(ModEnchantments.ATTRACTION.getKey());
                enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(attractionHolder, mainHand);
            } catch (Exception e) {
                if (shouldLogTick)
                    CatocraftMod.LOGGER.warn("[AttractionMixin] Attraction enchantment not found: {}", e.getMessage());
                continue;
            }
            if (enchantmentLevel <= 0) continue;

            double radius = Math.min(BASE_RADIUS + enchantmentLevel * 0.5, MAX_RADIUS);
            double baseSpeed = Math.min(BASE_SPEED + 0.05 * enchantmentLevel, MAX_SPEED);

            // Get all items in range
            AABB searchArea = player.getBoundingBox().inflate(radius + 2.0);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchArea);

            int attractedCount = 0;

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

                // Restore gravity if item is outside attraction radius
                if (distance > radius || item.isRemoved()) {
                    item.setNoGravity(false);
                    item.getPersistentData().remove("CatocraftGravityFade");
                    continue;
                }

                // Active attraction
                double speed = baseSpeed + (distance / radius) * baseSpeed;
                Vec3 direction = toPlayer.normalize();
                Vec3 desiredMotion = direction.scale(speed);

                // Smooth horizontal motion, direct vertical motion
                Vec3 oldMotion = item.getDeltaMovement();
                Vec3 smoothedMotion = new Vec3(
                        oldMotion.x * DAMPING + desiredMotion.x * SMOOTHING,
                        desiredMotion.y,
                        oldMotion.z * DAMPING + desiredMotion.z * SMOOTHING
                );

                item.setDeltaMovement(smoothedMotion);
                item.hasImpulse = true;
                item.setNoGravity(true);
                item.getPersistentData().putInt("CatocraftGravityFade", GRAVITY_FADE_TICKS);

                attractedCount++;
            }

            // Debug logging per player
            if (shouldLogTick && attractedCount > 0) {
                CatocraftMod.LOGGER.debug("[AttractionMixin] Player {} attracted {} items with level {}",
                        player.getName().getString(), attractedCount, enchantmentLevel);
            }
        }

        if (shouldLogTick)
            CatocraftMod.LOGGER.debug("[AttractionMixin] Processed item attraction tick");
    }
}