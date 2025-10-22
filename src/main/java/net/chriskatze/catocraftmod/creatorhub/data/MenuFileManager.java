package net.chriskatze.catocraftmod.creatorhub.data;

import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles filesystem access for creator menu JSONs.
 * Keeps all file I/O logic separate from GUI code.
 */
public class MenuFileManager {

    public static File getMenusFolder() {
        File folder = new File(Minecraft.getInstance().gameDirectory, "catocraftmod_data/menus");
        folder.mkdirs();
        return folder;
    }

    public static List<File> listMenuFiles() {
        File folder = getMenusFolder();
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return Collections.emptyList();

        return Arrays.stream(files)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    public static boolean deleteMenu(File file) {
        if (file != null && file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }
}