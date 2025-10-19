package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.*;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Loads UISchema JSONs from data/<modid>/ui_schemas/
 * Supports multiple UI elements in one schema.
 *
 * Extended to support:
 *  - background_color (solid color hex)
 *  - background_gradient (array of two hex colors)
 */
public class UISchemaLoader extends SimpleJsonResourceReloadListener {

    private static final Map<ResourceLocation, UISchema> SCHEMAS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UIElement.class, new UIElementDeserializer())
            .registerTypeAdapter(UISchema.class, new UISchemaDeserializer())
            .create();

    public UISchemaLoader() {
        super(GSON, "ui_schemas");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        SCHEMAS.clear();

        for (var entry : map.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                UISchema schema = GSON.fromJson(entry.getValue(), UISchema.class);
                SCHEMAS.put(id, schema);
            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[UI] ❌ Failed to load schema {}: {}", id, e.toString());
            }
        }

        CatocraftMod.LOGGER.info("[UI] ✅ Loaded {} UI schemas.", SCHEMAS.size());
    }

    public static UISchema get(ResourceLocation id) {
        return SCHEMAS.get(id);
    }

    public static Map<ResourceLocation, UISchema> getAll() {
        return Collections.unmodifiableMap(SCHEMAS);
    }

    // ────────────────────────────────────────────────
    // Custom polymorphic UI element deserializer
    // ────────────────────────────────────────────────
    private static class UIElementDeserializer implements JsonDeserializer<UIElement> {
        @Override
        public UIElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();

            return switch (type) {
                case "label" -> ctx.deserialize(obj, LabelElement.class);
                case "slot_group" -> ctx.deserialize(obj, SlotGroupElement.class);
                case "character_view" -> ctx.deserialize(obj, CharacterViewElement.class);
                default -> throw new JsonParseException("Unknown UI element type: " + type);
            };
        }
    }

    // ────────────────────────────────────────────────
    // Extended UISchema deserializer (handles new color fields)
    // ────────────────────────────────────────────────
    private static class UISchemaDeserializer implements JsonDeserializer<UISchema> {
        @Override
        public UISchema deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {

            JsonObject obj = json.getAsJsonObject();

            int width = obj.has("width") ? obj.get("width").getAsInt() : 176;
            int height = obj.has("height") ? obj.get("height").getAsInt() : 166;

            ResourceLocation background = null;
            if (obj.has("background")) {
                background = ResourceLocation.tryParse(obj.get("background").getAsString());
            }

            String backgroundColor = obj.has("background_color")
                    ? obj.get("background_color").getAsString()
                    : null;

            List<String> backgroundGradient = null;
            if (obj.has("background_gradient")) {
                try {
                    JsonArray arr = obj.getAsJsonArray("background_gradient");
                    backgroundGradient = new ArrayList<>();
                    for (JsonElement e : arr) backgroundGradient.add(e.getAsString());
                } catch (Exception e) {
                    CatocraftMod.LOGGER.warn("[UI] ⚠ Invalid background_gradient format in UISchema.");
                }
            }

            List<UIElement> elements = new ArrayList<>();
            if (obj.has("elements")) {
                JsonArray arr = obj.getAsJsonArray("elements");
                for (JsonElement el : arr) {
                    try {
                        elements.add(ctx.deserialize(el, UIElement.class));
                    } catch (Exception ex) {
                        CatocraftMod.LOGGER.warn("[UI] ⚠ Skipping invalid UI element in schema: {}", ex.toString());
                    }
                }
            }

            return new UISchema(width, height, background, backgroundColor, backgroundGradient, elements);
        }
    }
}