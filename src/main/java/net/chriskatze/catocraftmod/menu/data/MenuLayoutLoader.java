package net.chriskatze.catocraftmod.menu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Loads menu layout JSONs exported from the Creator Hub.
 */
public class MenuLayoutLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static MenuLayout load(ResourceLocation id) throws IOException {
        // 1️⃣ Try from run/data directory (Creator Hub overrides)
        Path path = Path.of("data", id.getNamespace(), "menus", id.getPath() + ".json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                MenuLayout layout = GSON.fromJson(reader, MenuLayout.class);
                CatocraftMod.LOGGER.info("[Catocraft] ✅ Loaded menu layout from disk: {}", path);
                return layout;
            }
        }

        // 2️⃣ Try from mod resources (src/main/resources/data/.../menus/...)
        String resourcePath = "/data/" + id.getNamespace() + "/menus/" + id.getPath() + ".json";
        try (InputStream stream = MenuLayoutLoader.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    MenuLayout layout = GSON.fromJson(reader, MenuLayout.class);
                    CatocraftMod.LOGGER.info("[Catocraft] ✅ Loaded menu layout from resources: {}", resourcePath);
                    return layout;
                }
            }
        }

        throw new FileNotFoundException(resourcePath);
    }

    public static MenuLayout safeLoad(ResourceLocation id) {
        try {
            return load(id);
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[Catocraft] ❌ Failed to load menu layout {}: {}", id, e.getMessage());
            return new MenuLayout(id.getPath(), 9, 3);
        }
    }
}