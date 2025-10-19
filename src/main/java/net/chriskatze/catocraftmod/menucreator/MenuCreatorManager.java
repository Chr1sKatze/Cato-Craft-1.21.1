package net.chriskatze.catocraftmod.menucreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MenuCreatorManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MenuCreatorDefinition current;

    public static void open(String id) {
        current = loadOrCreate(id);
        Minecraft mc = Minecraft.getInstance();
        // Ensure screen opens on the render thread
        mc.execute(() -> mc.setScreen(new MenuCreatorScreen(current)));
    }

    public static void save() {
        if (current == null) return;
        File file = getFile(current.menuId);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(current, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(String id) {
        MenuCreatorDefinition loaded = loadOrCreate(id);
        current = loaded;
        Minecraft mc = Minecraft.getInstance();
        // Same safety as above
        mc.execute(() -> mc.setScreen(new MenuCreatorScreen(loaded)));
    }

    private static MenuCreatorDefinition loadOrCreate(String id) {
        File file = getFile(id);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return GSON.fromJson(reader, MenuCreatorDefinition.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new MenuCreatorDefinition(id);
    }

    private static File getFile(String id) {
        File dir = new File("config/catocraft/menu_definitions");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, id + ".json");
    }
}
