package net.chriskatze.catocraftmod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 📘 ItemTypeRegistry
 *
 * Loads and manages custom item type definitions from JSON:
 * {
 *   "jewellery": "net.chriskatze.catocraftmod.item.JewelleryItem"
 * }
 *
 * Used by ItemTypeHelper to match custom logical item groups.
 */
public class ItemTypeRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Class<? extends Item>> TYPES = new LinkedHashMap<>();

    private static final File FILE = new File("data/catocraft/item_types.json");

    // ────────────────────────────────────────────────
    // Load
    // ────────────────────────────────────────────────
    public static void load() {
        TYPES.clear();

        if (!FILE.exists()) {
            CatocraftMod.LOGGER.warn("[ItemTypeRegistry] No item_types.json found, skipping custom type loading.");
            return;
        }

        try (FileReader reader = new FileReader(FILE)) {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> raw = GSON.fromJson(reader, mapType);

            for (Map.Entry<String, String> entry : raw.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                String className = entry.getValue();

                try {
                    Class<?> clazz = Class.forName(className);
                    if (Item.class.isAssignableFrom(clazz)) {
                        TYPES.put(key, (Class<? extends Item>) clazz);
                        CatocraftMod.LOGGER.info("[ItemTypeRegistry] Loaded custom item type '{}': {}", key, className);
                    } else {
                        CatocraftMod.LOGGER.warn("[ItemTypeRegistry] {} is not an Item subclass.", className);
                    }
                } catch (Exception e) {
                    CatocraftMod.LOGGER.error("[ItemTypeRegistry] Failed to load type '{}': {}", key, e.toString());
                }
            }

            CatocraftMod.LOGGER.info("[ItemTypeRegistry] Registered {} custom item types.", TYPES.size());
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[ItemTypeRegistry] Failed to read item_types.json", e);
        }
    }

    // ────────────────────────────────────────────────
    // Save
    // ────────────────────────────────────────────────
    public static void save() {
        try {
            if (!FILE.getParentFile().exists())
                FILE.getParentFile().mkdirs();

            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, Class<? extends Item>> e : TYPES.entrySet()) {
                out.put(e.getKey(), e.getValue().getName());
            }

            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(out, writer);
            }

            CatocraftMod.LOGGER.info("[ItemTypeRegistry] Saved {} custom item types.", TYPES.size());
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[ItemTypeRegistry] ⚠ Failed to save item_types.json: {}", e.toString());
        }
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────
    public static Class<? extends Item> get(String key) {
        return TYPES.get(key.toLowerCase(Locale.ROOT));
    }

    public static boolean has(String key) {
        return TYPES.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public static Map<String, Class<? extends Item>> getAll() {
        return TYPES;
    }

    public static List<String> getAllTypeNames() {
        return new ArrayList<>(TYPES.keySet());
    }

    public static void addType(String name) {
        if (!TYPES.containsKey(name.toLowerCase(Locale.ROOT))) {
            TYPES.put(name.toLowerCase(Locale.ROOT), Item.class); // default dummy value
        }
    }

    public static void removeType(String name) {
        TYPES.remove(name.toLowerCase(Locale.ROOT));
    }
}