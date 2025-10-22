package net.chriskatze.catocraftmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenuWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class CleanDynamicMenusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cleandynamicmenus")
                .requires(source -> source.hasPermission(2)) // OP-only
                .executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    DynamicMenuWorldData data = DynamicMenuWorldData.get(level);

                    CompoundTag before = data.getDataTag();
                    int beforeCount = before.getAllKeys().size();

                    // Build per-player log
                    List<String> playerSummaries = new ArrayList<>();
                    for (String playerId : before.getAllKeys()) {
                        CompoundTag playerTag = before.getCompound(playerId);
                        CompoundTag menus = playerTag.getCompound("CatoDynamicMenus");
                        int count = menus.getAllKeys().size();
                        playerSummaries.add(" • " + playerId + " → " + count + " menu(s) before cleanup");
                    }

                    data.cleanup(level.registryAccess());

                    CompoundTag after = data.getDataTag();
                    int afterCount = after.getAllKeys().size();
                    int removed = beforeCount - afterCount;

                    // Send message to operator
                    StringBuilder msg = new StringBuilder("🧹 Cleaned DynamicMenu data:");
                    for (String s : playerSummaries) msg.append("\n").append(s);
                    msg.append("\n → Removed ").append(removed).append(" entries total.");

                    ctx.getSource().sendSuccess(() -> Component.literal(msg.toString()), true);

                    CatocraftMod.LOGGER.info("[DynamicMenu] Manual cleanup by {} → removed {} entries",
                            ctx.getSource().getTextName(), removed);

                    return removed;
                })
        );
    }
}