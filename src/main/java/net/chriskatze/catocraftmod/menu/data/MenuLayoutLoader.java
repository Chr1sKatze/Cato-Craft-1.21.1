package net.chriskatze.catocraftmod.menu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads menu layout JSONs exported from the Creator Hub.
 * Supports runtime listing and reloading for in-game testing.
 */
public class MenuLayoutLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 🧩 Cache of all loaded layouts by ID
    private static final Map<ResourceLocation, MenuLayout> LAYOUTS = new HashMap<>();

    // 🔧 Default search directories
    private static final Path USER_DATA_PATH = Path.of("catocraftmod_data/menus");
    private static final String RESOURCE_BASE = "/data/";

    // ────────────────────────────────────────────────
    // 📦 Load single layout
    // ────────────────────────────────────────────────
    public static MenuLayout load(ResourceLocation id) throws IOException {
        // 1️⃣ Try from run/catocraftmod_data/menus directory (Creator Hub saves here)
        Path path = USER_DATA_PATH.resolve(id.getPath() + ".json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                MenuLayout layout = GSON.fromJson(reader, MenuLayout.class);
                LAYOUTS.put(id, layout);
                CatocraftMod.LOGGER.info("[Catocraft] ✅ Loaded menu layout from disk: {}", path);
                return layout;
            }
        }

        // 2️⃣ Try from mod resources (src/main/resources/data/.../menus/...)
        String resourcePath = RESOURCE_BASE + id.getNamespace() + "/menus/" + id.getPath() + ".json";
        try (InputStream stream = MenuLayoutLoader.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    MenuLayout layout = GSON.fromJson(reader, MenuLayout.class);
                    LAYOUTS.put(id, layout);
                    CatocraftMod.LOGGER.info("[Catocraft] ✅ Loaded menu layout from resources: {}", resourcePath);
                    return layout;
                }
            }
        }

        throw new FileNotFoundException(resourcePath);
    }

    // ────────────────────────────────────────────────
    // 🧩 Safe load with fallback
    // ────────────────────────────────────────────────
    public static MenuLayout safeLoad(ResourceLocation id) {
        try {
            if (LAYOUTS.containsKey(id)) {
                return LAYOUTS.get(id);
            }
            return load(id);
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[Catocraft] ❌ Failed to load menu layout {}: {}", id, e.getMessage());
            return new MenuLayout(id.getPath(), 176, 166);
        }
    }

    // ────────────────────────────────────────────────
    // 📜 Discover all layout files
    // ────────────────────────────────────────────────
    public static void discoverLayouts() {
        LAYOUTS.clear();

        try {
            Path menusDir = Path.of("catocraftmod_data/menus");
            if (Files.exists(menusDir)) {
                Files.walk(menusDir)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(path -> {
                            String rel = menusDir.relativize(path).toString().replace("\\", "/");
                            String idPath = rel.substring(0, rel.length() - 5); // remove .json

                            // ✅ Use safe factory for 1.21+
                            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("catocraftmod", idPath);

                            try (Reader reader = Files.newBufferedReader(path)) {
                                MenuLayout layout = GSON.fromJson(reader, MenuLayout.class);
                                LAYOUTS.put(id, layout);
                            } catch (Exception e) {
                                CatocraftMod.LOGGER.warn("[Catocraft] ⚠️ Failed to parse layout {}: {}", id, e.getMessage());
                            }
                        });
            }

            CatocraftMod.LOGGER.info("[Catocraft] 📜 Discovered {} menu layouts", LAYOUTS.size());
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("[Catocraft] ❌ Failed to scan layouts: {}", e.toString());
        }
    }

    // ────────────────────────────────────────────────
    // 🔄 Public API for Commands
    // ────────────────────────────────────────────────
    public static Set<ResourceLocation> getAllLayouts() {
        if (LAYOUTS.isEmpty()) {
            discoverLayouts();
        }
        return Collections.unmodifiableSet(LAYOUTS.keySet());
    }

    public static void reload() {
        CatocraftMod.LOGGER.info("[Catocraft] 🔄 Reloading all menu layouts...");
        discoverLayouts();
    }
}