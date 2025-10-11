package net.chriskatze.catocraftmod.enchantment.custom;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.network.OreSenseGlowPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class OreSenseHandler {

    private static final int BASE_COOLDOWN = 200; // 10s base
    private static final int RADIUS = 8;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        ItemStack held = player.getMainHandItem();
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ORE_SENSE.getHolder(), held);
        if (enchantLevel <= 0) return;

        int timer = player.getPersistentData().getInt("OreSenseCooldown") + 1;
        player.getPersistentData().putInt("OreSenseCooldown", timer);

        int cooldown = BASE_COOLDOWN - (enchantLevel * 40);
        if (cooldown < 40) cooldown = 40;

        if (timer >= cooldown) {
            player.getPersistentData().putInt("OreSenseCooldown", 0);
            triggerGlow(serverLevel, player);
        }
    }

    private static void triggerGlow(ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> glowBlocks = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (isOreBlock(state)) {
                glowBlocks.add(pos.immutable());
            }
        }

        if (!glowBlocks.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            OreSenseGlowPacket.sendToClient((ServerLevel) level, serverPlayer, glowBlocks);
        }
    }

    private static boolean isOreBlock(BlockState state) {
        return state.is(Blocks.COAL_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE)
                || state.is(Blocks.LAPIS_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(ModBlocks.PLATINUM_ORE);
    }
}