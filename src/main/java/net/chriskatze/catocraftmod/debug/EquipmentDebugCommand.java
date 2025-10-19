package net.chriskatze.catocraftmod.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.network.EquipmentSyncHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Unified developer command for inspecting player equipment, groups, attributes, and capability state.
 *
 * Usage:
 *   /equipmentdebug                  → logs sender’s equipment
 *   /equipmentdebug <player>         → logs target’s equipment
 *   /equipmentdebug all              → logs all players’ equipment
 *   /equipmentdebug groups           → logs all registered EquipmentGroups
 *   /equipmentdebug attributes       → lists current attribute modifiers in chat
 *   /equipmentdebug reload           → forces capability & layout reload for all players
 */
public class EquipmentDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("equipmentdebug")
                .requires(source -> source.hasPermission(2)) // ops only
                // Default: sender’s info
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player != null) {
                        EquipmentDebugHelper.logPlayerEquipment(player);
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("✅ Logged equipment info for " + player.getName().getString()), false);
                    }
                    return 1;
                })
                // Argument: target player or "all"
                .then(Commands.argument("target", StringArgumentType.string())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "target");
                            var server = ctx.getSource().getServer();

                            if (arg.equalsIgnoreCase("all")) {
                                EquipmentDebugHelper.logAllPlayers(server);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("✅ Logged all players’ equipment states."), false);
                                return 1;
                            }

                            ServerPlayer target = server.getPlayerList().getPlayerByName(arg);
                            if (target != null) {
                                EquipmentDebugHelper.logPlayerEquipment(target);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("✅ Logged equipment info for " + target.getName().getString()), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("❌ Player not found."));
                            }
                            return 1;
                        })
                )
                // 🔹 Logs all EquipmentGroups
                .then(Commands.literal("groups")
                        .executes(ctx -> {
                            EquipmentGroup.logRegistry();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("✅ Logged all EquipmentGroups to console."), false);
                            return 1;
                        })
                )
                // 🔹 Lists active attribute modifiers
                .then(Commands.literal("attributes")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!(src.getEntity() instanceof ServerPlayer player)) {
                                src.sendFailure(Component.literal("❌ Must be run by a player."));
                                return 0;
                            }

                            src.sendSuccess(() -> Component.literal("⚙ Active Attribute Modifiers:"), false);

                            int count = 0;
                            for (AttributeInstance attr : player.getAttributes().getSyncableAttributes()) {
                                for (AttributeModifier mod : attr.getModifiers()) {
                                    if (!mod.id().getNamespace().equals(CatocraftMod.MOD_ID)) continue;

                                    double amount = mod.amount();
                                    ChatFormatting color = amount > 0 ? ChatFormatting.BLUE : ChatFormatting.RED;
                                    String sign = amount > 0 ? "+" : "";

                                    Component line = Component.literal(" • ")
                                            .append(Component.translatable(attr.getAttribute().value().getDescriptionId())
                                                    .withStyle(ChatFormatting.GRAY))
                                            .append(Component.literal(" "))
                                            .append(Component.literal(sign + String.format("%.2f", amount))
                                                    .withStyle(color))
                                            .append(Component.literal(" (" + mod.operation().name() + ")")
                                                    .withStyle(ChatFormatting.DARK_GRAY));

                                    src.sendSuccess(() -> line, false);
                                    count++;
                                }
                            }

                            if (count == 0) {
                                src.sendSuccess(() ->
                                        Component.literal("No active Catocraft modifiers.")
                                                .withStyle(ChatFormatting.GRAY), false);
                            }
                            return 1;
                        })
                )
                // 🔹 Forces capability & layout reload
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            int reloaded = 0;

                            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                var cap = EquipmentCapabilityHandler.get(player);
                                if (cap != null) {
                                    cap.initializeGroupsIfMissing();
                                    cap.onLayoutsReloaded();
                                    cap.applyAllAttributes();
                                    cap.scheduleHealthNormalization(20);
                                    EquipmentSyncHelper.syncToClient(player);
                                    reloaded++;
                                }
                            }

                            final int count = reloaded; // <-- make it effectively final for the lambda
                            ctx.getSource().sendSuccess(
                                    () -> net.minecraft.network.chat.Component.literal(
                                            "🔁 Reloaded equipment layouts & attributes for " + count + " player(s)."),
                                    false
                            );

                            CatocraftMod.LOGGER.info("[EquipmentDebug] Reloaded equipment layouts for {} players.", reloaded);
                            return 1;
                        })
                )
        );

        CatocraftMod.LOGGER.info("[Catocraft] Registered /equipmentdebug command.");
    }
}