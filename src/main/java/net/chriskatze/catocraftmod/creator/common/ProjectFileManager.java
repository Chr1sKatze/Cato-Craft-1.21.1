package net.chriskatze.catocraftmod.creator.common;

import net.chriskatze.catocraftmod.creatorhub.CreatorType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ProjectFileManager {

    public static Path getBaseFolder() {
        return Paths.get("data/catocraft");
    }

    public static Path getFolderFor(CreatorType type) {
        return getBaseFolder().resolve(type.folderName);
    }

    public static Path getFile(CreatorType type, String name) {
        return getFolderFor(type).resolve(name + ".json");
    }

    public static boolean exists(CreatorType type, String name) {
        return Files.exists(getFile(type, name));
    }

    public static void create(CreatorType type, String name, String json) throws IOException {
        Path file = getFile(type, name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void openFolder(@Nullable CreatorType type) throws IOException {
        Path folder = (type == null) ? getBaseFolder() : getFolderFor(type);
        Files.createDirectories(folder);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(folder.toFile());
        }
    }

}