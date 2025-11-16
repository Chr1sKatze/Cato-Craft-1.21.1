package net.chriskatze.catocraftmod.creator.menu;

import com.google.gson.*;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creatorhub.core.JsonFileHelper;
import net.chriskatze.catocraftmod.menu.layout.SlotType;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧱 MenuLayoutSerializer
 *
 * Centralized JSON (de)serialization for MenuLayout + MenuSlotDefinition.
 * Fully supports:
 *  - legacy single tag
 *  - new multi-tag system
 *  - groups
 *  - slot textures
 *  - backward compatibility
 */
public class MenuLayoutSerializer {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(MenuLayout.class, new MenuLayoutAdapter())
            .registerTypeAdapter(MenuSlotDefinition.class, new MenuSlotDefinitionAdapter())   // 👈 NEW
            .create();

    // ───────────────────────────────────────────────
    // Load
    // ───────────────────────────────────────────────
    public static MenuLayout load(File file) {
        try {
            MenuLayout layout = JsonFileHelper.read(file, MenuLayout.class, GSON);
            if (layout == null) {
                CatocraftMod.LOGGER.warn("[MenuLayoutSerializer] Failed to load layout: {}", file != null ? file.getName() : "null");
                return null;
            }

            // Normalize slot data (legacy support)
            if (layout.getSlots() != null) {
                for (MenuSlotDefinition s : layout.getSlots()) {
                    if (s == null) continue;
                    if (s.getSize() <= 0) s.setSize(18);
                }
            }

            // Normalize groups (legacy)
            if (layout.getGroups() == null)
                layout.setGroups(new ArrayList<>());

            return layout;

        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuLayoutSerializer] Error loading {}: {}", file.getName(), e.toString());
            return null;
        }
    }

    // ───────────────────────────────────────────────
    // Save
    // ───────────────────────────────────────────────
    public static void save(MenuLayout layout, File file) {
        try {
            if (!JsonFileHelper.write(file, layout, GSON)) {
                CatocraftMod.LOGGER.error("[MenuLayoutSerializer] Failed to save layout: {}", file != null ? file.getName() : "null");
            } else {
                CatocraftMod.LOGGER.info("[MenuLayoutSerializer] Saved layout: {}", file.getName());
            }
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuLayoutSerializer] Exception while saving {}: {}", file.getName(), e.toString());
        }
    }

    // ───────────────────────────────────────────────
    // MenuLayout Adapter
    // ───────────────────────────────────────────────
    private static class MenuLayoutAdapter implements JsonSerializer<MenuLayout>, JsonDeserializer<MenuLayout> {

        @Override
        public JsonElement serialize(MenuLayout layout, Type type, JsonSerializationContext ctx) {
            JsonObject root = new JsonObject();

            root.addProperty("name", layout.getName());
            root.addProperty("width", layout.getWidth());
            root.addProperty("height", layout.getHeight());

            if (layout.getBackgroundTexture() != null)
                root.addProperty("backgroundTexture", layout.getBackgroundTexture());

            // Slot textures
            JsonObject texObj = new JsonObject();
            layout.getSlotTextures().forEach(texObj::addProperty);
            root.add("slotTextures", texObj);

            // Groups
            JsonArray groupArr = new JsonArray();
            for (MenuGroupDefinition g : layout.getGroups()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", g.getName());
                o.addProperty("displayName", g.getDisplayName());
                if (g.getValidItemTag() != null)  o.addProperty("validItemTag",  g.getValidItemTag());
                if (g.getValidItemType() != null) o.addProperty("validItemType", g.getValidItemType());
                o.addProperty("isStrict", g.isStrict());
                groupArr.add(o);
            }
            root.add("groups", groupArr);

            // Slots (MenuSlotDefinitionAdapter handles each entry)
            JsonArray slotArr = ctx.serialize(layout.getSlots()).getAsJsonArray();
            root.add("slots", slotArr);

            return root;
        }

        @Override
        public MenuLayout deserialize(JsonElement el, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {

            JsonObject root = el.getAsJsonObject();
            MenuLayout layout = new MenuLayout(
                    root.get("name").getAsString(),
                    root.get("width").getAsInt(),
                    root.get("height").getAsInt()
            );

            if (root.has("backgroundTexture"))
                layout.setBackgroundTexture(root.get("backgroundTexture").getAsString());

            // Slot textures
            if (root.has("slotTextures")) {
                JsonObject texObj = root.getAsJsonObject("slotTextures");
                texObj.entrySet().forEach(e -> layout.addSlotTexture(e.getKey(), e.getValue().getAsString()));
            }

            // Groups
            if (root.has("groups")) {
                List<MenuGroupDefinition> groups = new ArrayList<>();
                for (JsonElement gEl : root.getAsJsonArray("groups")) {
                    JsonObject gObj = gEl.getAsJsonObject();
                    MenuGroupDefinition g = new MenuGroupDefinition(gObj.get("name").getAsString());
                    if (gObj.has("displayName")) g.setDisplayName(gObj.get("displayName").getAsString());
                    if (gObj.has("validItemTag")) g.setValidItemTag(gObj.get("validItemTag").getAsString());
                    if (gObj.has("validItemType")) g.setValidItemType(gObj.get("validItemType").getAsString());
                    if (gObj.has("isStrict")) g.setStrict(gObj.get("isStrict").getAsBoolean());
                    groups.add(g);
                }
                layout.setGroups(groups);
            }

            // Slots
            if (root.has("slots")) {
                List<MenuSlotDefinition> slots = new ArrayList<>();
                for (JsonElement sEl : root.getAsJsonArray("slots")) {
                    MenuSlotDefinition slot = ctx.deserialize(sEl, MenuSlotDefinition.class);
                    slots.add(slot);
                }
                layout.setSlots(slots);
            }

            return layout;
        }
    }

    // ───────────────────────────────────────────────
    // MenuSlotDefinition Adapter  (NEW)
    // ───────────────────────────────────────────────
    private static class MenuSlotDefinitionAdapter
            implements JsonSerializer<MenuSlotDefinition>, JsonDeserializer<MenuSlotDefinition> {

        @Override
        public JsonElement serialize(MenuSlotDefinition slot, Type type, JsonSerializationContext ctx) {
            JsonObject o = new JsonObject();

            o.addProperty("id", slot.getId());
            o.addProperty("x", slot.getX());
            o.addProperty("y", slot.getY());
            o.addProperty("size", slot.getSize());

            // legacy tag
            if (slot.getTag() != null && !slot.getTag().isEmpty())
                o.addProperty("tag", slot.getTag());

            // new multi-tag
            JsonArray tagArr = new JsonArray();
            for (String t : slot.getTags())
                tagArr.add(t);
            o.add("tags", tagArr);

            o.addProperty("type", slot.getType());
            o.addProperty("optional", slot.isOptional());
            o.addProperty("group", slot.getGroup());

            if (slot.getTextureOverride() != null && !slot.getTextureOverride().isEmpty())
                o.addProperty("textureOverride", slot.getTextureOverride());

            return o;
        }

        @Override
        public MenuSlotDefinition deserialize(JsonElement el, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {

            JsonObject o = el.getAsJsonObject();

            String id       = o.has("id") ? o.get("id").getAsString() : "slot";
            int x           = o.has("x") ? o.get("x").getAsInt() : 0;
            int y           = o.has("y") ? o.get("y").getAsInt() : 0;
            int size        = o.has("size") ? o.get("size").getAsInt() : 18;

            String legacyTag = o.has("tag") ? o.get("tag").getAsString() : "";

            String typeStr   = o.has("type") ? o.get("type").getAsString() : SlotType.INVENTORY.name();
            boolean optional = o.has("optional") && o.get("optional").getAsBoolean();
            String group     = o.has("group") ? o.get("group").getAsString() : "default";

            MenuSlotDefinition slot = new MenuSlotDefinition(id, x, y, size, legacyTag, typeStr, optional, group);

            // texture override
            if (o.has("textureOverride"))
                slot.setTextureOverride(o.get("textureOverride").getAsString());

            // multi-tag
            if (o.has("tags")) {
                JsonArray arr = o.getAsJsonArray("tags");
                List<String> list = new ArrayList<>();
                for (JsonElement e : arr)
                    list.add(e.getAsString());
                slot.setTags(list);
            }

            // if multi-tag missing but legacy tag exists → sync into list
            if (slot.getTags().isEmpty() && legacyTag != null && !legacyTag.isBlank())
                slot.addTag(legacyTag);

            return slot;
        }
    }
}