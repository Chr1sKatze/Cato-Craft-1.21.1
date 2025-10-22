package net.chriskatze.catocraftmod.menu.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Persistent storage for dynamic menus.
 * Uses world SavedData per player on server side.
 */
public class DynamicMenuStorage {

    private static final String ROOT_TAG = "CatoDynamicMenus";
    private static final String LAST_OPENED = "LastOpened";

    public static ItemStackHandler getOrCreate(Player player, String layoutName, int size) {
        // ⚡ Handle server vs client safely
        if (player.level().isClientSide()) {
            // Client: temporary handler only
            return new ItemStackHandler(size);
        }

        ServerLevel level = (ServerLevel) player.level();
        DynamicMenuWorldData data = DynamicMenuWorldData.get(level);

        CompoundTag playerTag = data.getPlayer(player.getStringUUID());
        CompoundTag root = playerTag.getCompound(ROOT_TAG);

        ItemStackHandler handler = new ItemStackHandler(size);

        if (root.contains(layoutName)) {
            handler.deserializeNBT(level.registryAccess(), root.getCompound(layoutName));
        } else {
            save(player, layoutName, handler);
        }

        return handler;
    }

    public static void save(Player player, String layoutName, ItemStackHandler handler) {
        if (player.level().isClientSide()) return; // no-op on client

        ServerLevel level = (ServerLevel) player.level();
        DynamicMenuWorldData data = DynamicMenuWorldData.get(level);

        CompoundTag playerTag = data.getPlayer(player.getStringUUID());
        CompoundTag root = playerTag.getCompound(ROOT_TAG);

        root.put(layoutName, handler.serializeNBT(level.registryAccess()));
        playerTag.put(ROOT_TAG, root);

        data.savePlayer(player.getStringUUID(), playerTag);
    }

    public static void setLastOpened(Player player, String layoutName) {
        if (player.level().isClientSide()) return; // no-op on client

        ServerLevel level = (ServerLevel) player.level();
        DynamicMenuWorldData data = DynamicMenuWorldData.get(level);

        CompoundTag playerTag = data.getPlayer(player.getStringUUID());
        playerTag.putString(LAST_OPENED, layoutName);

        data.savePlayer(player.getStringUUID(), playerTag);
    }

    public static String getLastOpened(Player player) {
        if (player.level().isClientSide()) return ""; // client doesn’t store persistent menus

        ServerLevel level = (ServerLevel) player.level();
        DynamicMenuWorldData data = DynamicMenuWorldData.get(level);

        CompoundTag playerTag = data.getPlayer(player.getStringUUID());
        return playerTag.getString(LAST_OPENED);
    }
}