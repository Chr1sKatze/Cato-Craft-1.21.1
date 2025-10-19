package net.chriskatze.catocraftmod.debug;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;


/**
 * Developer helper for printing out the current equipment state
 * of one or all players. Helps verify capability contents,
 * slot definitions, and applied attributes at runtime.
 */
public final class EquipmentDebugHelper {

    private EquipmentDebugHelper() {}

    // ────────────────────────────────────────────────
    // MAIN METHODS
    // ────────────────────────────────────────────────

    /** Prints a full debug summary for a single player. */
    public static void logPlayerEquipment(ServerPlayer player) {
        if (player == null) return;

        PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EquipmentDebug] No capability found for player {}", player.getName().getString());
            return;
        }

        CatocraftMod.LOGGER.info("──────────────────────────────────────────────");
        CatocraftMod.LOGGER.info("[EquipmentDebug] Equipment state for player: {}", player.getGameProfile().getName());

        Map<EquipmentGroup, ?> groups = cap.getAllGroups();
        if (groups.isEmpty()) {
            CatocraftMod.LOGGER.info("  No equipment groups initialized!");
            return;
        }

        for (var entry : groups.entrySet()) {
            EquipmentGroup group = entry.getKey();
            var handler = cap.getAllGroups().get(group);

            CatocraftMod.LOGGER.info("  Group: {}  ({} slot{})",
                    group.getKey(), handler.getSlots(), handler.getSlots() > 1 ? "s" : "");

            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                String itemName = stack.isEmpty() ? "<empty>" : stack.getItem().toString();
                CatocraftMod.LOGGER.info("     [{}] {}", i, itemName);
            }
        }

// Attribute overview
        CatocraftMod.LOGGER.info("  Active Modifiers:");
        player.getAttributes().getSyncableAttributes().forEach(attr -> {
            attr.getModifiers().forEach(mod -> {
                if (mod.id().getNamespace().equals(CatocraftMod.MOD_ID)) {
                    String attributeName = attr.getAttribute().isBound()
                            ? attr.getAttribute().value().getDescriptionId()
                            : "<unbound>";

                    CatocraftMod.LOGGER.info("     • {} → {} ({})",
                            attributeName,
                            mod.amount(),
                            mod.operation());
                }
            });
        });

        CatocraftMod.LOGGER.info("──────────────────────────────────────────────");
    }

    /** Prints the state for all online players. */
    public static void logAllPlayers(net.minecraft.server.MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(EquipmentDebugHelper::logPlayerEquipment);
    }
}