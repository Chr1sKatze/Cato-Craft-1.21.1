package net.chriskatze.catocraftmod.creator.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> void save(Path path, T data) throws IOException {
        Files.writeString(path, GSON.toJson(data));
    }

    public static <T> T load(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) throw new IOException("File not found: " + path);
        return GSON.fromJson(Files.readString(path), clazz);
    }
}