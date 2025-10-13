//package net.chriskatze.catocraftmod.enchantment.custom;
//
//import net.chriskatze.catocraftmod.CatocraftMod;
//import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
//import net.chriskatze.catocraftmod.network.OreSenseGlowPacket;
//import net.chriskatze.catocraftmod.util.ModTags;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.enchantment.EnchantmentHelper;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
//public class OreSenseHandler {
//
//    private static final int BASE_COOLDOWN = 600; // 30 seconds
//    private static final int GLOW_DURATION = 60;  // 3 seconds
//    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 40; // 2 seconds less per level
//    private static final int RADIUS = 8;
//
//    @SubscribeEvent
//    public static void onPlayerTick(PlayerTickEvent.Post event) {
//        Player player = event.getEntity();
//        Level level = player.level();
//        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
//
//        ItemStack held = player.getMainHandItem();
//        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ORE_SENSE.getHolder(), held);
//        if (enchantLevel <= 0) return;
//
//        int timer = player.getPersistentData().getInt("OreSenseCooldown") + 1;
//        player.getPersistentData().putInt("OreSenseCooldown", timer);
//
//        int cooldown = BASE_COOLDOWN - ((enchantLevel - 1) * COOLDOWN_REDUCTION_PER_LEVEL);
//        if (cooldown < 40) cooldown = 40; // minimum safety cap
//
//        if (timer >= cooldown) {
//            player.getPersistentData().putInt("OreSenseCooldown", 0);
//            triggerGlow(serverLevel, player, enchantLevel);
//        }
//    }
//
//    private static void triggerGlow(ServerLevel level, Player player, int enchantLevel) {
//        BlockPos center = player.blockPosition();
//        List<BlockPos> glowBlocks = new ArrayList<>();
//
//        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS),
//                center.offset(RADIUS, RADIUS, RADIUS))) {
//
//            if (!level.hasChunkAt(pos)) continue; // skip unloaded chunks
//            BlockState state = level.getBlockState(pos);
//            if (state.is(ModTags.EMISSIVE_ORES)) {
//                glowBlocks.add(pos.immutable());
//            }
//        }
//
//        if (!glowBlocks.isEmpty() && player instanceof ServerPlayer serverPlayer) {
//            OreSenseGlowPacket.sendToClient(level, serverPlayer, glowBlocks, GLOW_DURATION);
//        }
//    }
//}