package net.chriskatze.catocraftmod.creatorhub.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.*;

/**
 * 🧾 JsonFileHelper — simplified JSON file I/O for Creator Hub modules.
 *
 * Features:
 * - Safe reading/writing with null fallback
 * - Pretty printing for readability
 * - Automatic folder creation
 * - Now supports custom Gson instances (for special adapters)
 */
public class JsonFileHelper {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    // ────────────────────────────────────────────────
    // 🔹 Default read/write (uses built-in GSON)
    // ────────────────────────────────────────────────

    public static <T> T read(File file, Class<T> type) {
        if (file == null || !file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, type);
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Failed to read JSON: " + file.getName()), false);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean write(File file, Object data) {
        if (file == null || data == null) return false;
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
            return true;
        } catch (IOException e) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Failed to save JSON: " + file.getName()), false);
            e.printStackTrace();
            return false;
        }
    }

    // ────────────────────────────────────────────────
    // 🔹 Overloads supporting a custom Gson instance
    // ────────────────────────────────────────────────

    public static <T> T read(File file, Class<T> type, Gson gson) {
        if (file == null || !file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, type);
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Failed to read JSON: " + file.getName()), false);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean write(File file, Object data, Gson gson) {
        if (file == null || data == null) return false;
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            }
            return true;
        } catch (IOException e) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Failed to save JSON: " + file.getName()), false);
            e.printStackTrace();
            return false;
        }
    }

    // ────────────────────────────────────────────────
    // 🔹 Utility: pretty-print any object as JSON
    // ────────────────────────────────────────────────

    public static String toPrettyString(Object data) {
        try {
            return GSON.toJson(data);
        } catch (Exception e) {
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}