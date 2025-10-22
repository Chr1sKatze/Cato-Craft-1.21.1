package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores all player menu data for the current world.
 * Saved in world/data/catocraft_dynamic_menus.dat
 */
public class DynamicMenuWorldData extends SavedData {

    private static final String FILE_ID = CatocraftMod.MOD_ID + "_dynamic_menus";

    // Each player UUID → CompoundTag with their menus
    private final Map<String, CompoundTag> playerData = new HashMap<>();

    public DynamicMenuWorldData() {}

    // ✅ Load from NBT
    public static DynamicMenuWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        DynamicMenuWorldData data = new DynamicMenuWorldData();
        for (String key : tag.getAllKeys()) {
            data.playerData.put(key, tag.getCompound(key));
        }
        return data;
    }

    // ✅ Save to NBT
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<String, CompoundTag> entry : playerData.entrySet()) {
            tag.put(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    // ────────────────────────────────────────────────
    // Access helpers
    // ────────────────────────────────────────────────
    public static DynamicMenuWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        DynamicMenuWorldData::new,      // create new
                        DynamicMenuWorldData::load      // load from NBT
                ),
                FILE_ID
        );
    }

    public void savePlayer(String uuid, CompoundTag data) {
        playerData.put(uuid, data);
        setDirty();
    }

    public CompoundTag getPlayer(String uuid) {
        return playerData.getOrDefault(uuid, new CompoundTag());
    }

    // ────────────────────────────────────────────────
    // Cleanup utilities
    // ────────────────────────────────────────────────
    /**
     * Removes any empty menu inventories or stale player entries.
     * Call this occasionally (e.g. on world save or server stop).
     */
    public void cleanup(HolderLookup.Provider provider) {
        boolean changed = false;
        Iterator<Map.Entry<String, CompoundTag>> playerIter = playerData.entrySet().iterator();

        while (playerIter.hasNext()) {
            Map.Entry<String, CompoundTag> entry = playerIter.next();
            String playerId = entry.getKey();
            CompoundTag playerTag = entry.getValue();

            CompoundTag menus = playerTag.getCompound("CatoDynamicMenus");

            // remove empty menu inventories
            Iterator<String> menuKeys = menus.getAllKeys().iterator();
            while (menuKeys.hasNext()) {
                String key = menuKeys.next();
                CompoundTag menuTag = menus.getCompound(key);
                var handler = new net.neoforged.neoforge.items.ItemStackHandler(1);
                handler.deserializeNBT(provider, menuTag);

                boolean empty = true;
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (!handler.getStackInSlot(i).isEmpty()) {
                        empty = false;
                        break;
                    }
                }

                if (empty) {
                    menuKeys.remove(); // safely remove while iterating
                    changed = true;
                }
            }

            // if player has no menus left, drop the player entirely
            if (menus.isEmpty()) {
                playerIter.remove();
                changed = true;
            } else {
                playerTag.put("CatoDynamicMenus", menus);
                playerData.put(playerId, playerTag);
            }
        }

        if (changed) setDirty();
    }

    // ────────────────────────────────────────────────
    // Diagnostic access (for /cleandynamicmenus)
    // ────────────────────────────────────────────────
    public CompoundTag getDataTag() {
        CompoundTag out = new CompoundTag();
        for (Map.Entry<String, CompoundTag> e : playerData.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}