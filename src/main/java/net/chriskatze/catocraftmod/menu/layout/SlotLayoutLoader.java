package net.chriskatze.catocraftmod.menu.layout;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads SlotLayoutDefinitions from data/<modid>/slot_layouts/ JSON files.
 * Supports:
 *  - Single layout files (one group)
 *  - Multi-group layout files with "groups" sections
 *  - Dynamic EquipmentGroup registration from JSON
 *  - Full rule metadata (requires/conflicts/tags)
 */
public class SlotLayoutLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    // 🔹 Parsed layouts for rendering
    private static final Map<ResourceLocation, SlotLayout> LAYOUTS = new HashMap<>();

    // 🔹 Original definitions (for rule validation)
    private static final Map<ResourceLocation, SlotLayoutDefinition> DEFINITIONS = new HashMap<>();

    private static ResourceLocation activeLayoutId = null;

    public SlotLayoutLoader() {
        super(GSON, "slot_layouts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared,
                         ResourceManager manager,
                         ProfilerFiller profiler) {

        LAYOUTS.clear();
        DEFINITIONS.clear();

        // 🟢 Reset base groups before re-registering (ensures clean reload)
        EquipmentGroup.resetBaseGroups();

        prepared.forEach((id, element) -> {
            try {
                JsonObject root = element.getAsJsonObject();

                // ── MULTI-GROUP MODE ──
                if (root.has("groups")) {
                    JsonObject groups = root.getAsJsonObject("groups");
                    for (var entry : groups.entrySet()) {
                        String groupName = entry.getKey();
                        JsonObject groupJson = entry.getValue().getAsJsonObject();

                        ResourceLocation groupId = ResourceLocation.fromNamespaceAndPath(
                                id.getNamespace(),
                                id.getPath() + "/" + groupName
                        );

                        SlotLayoutDefinition def = SlotLayoutDefinition.fromJson(groupId, groupJson);
                        SlotLayout layout = def.toSlotLayout();

                        LAYOUTS.put(groupId, layout);
                        DEFINITIONS.put(groupId, def);

                        // 🟢 NEW: Register a dynamic EquipmentGroup automatically
                        EquipmentGroup.register(groupName);
                        CatocraftMod.LOGGER.debug("[SlotLayoutLoader] Registered dynamic group '{}'", groupName);
                    }
                }
                // ── SINGLE-GROUP MODE ──
                else {
                    SlotLayoutDefinition def = SlotLayoutDefinition.fromJson(id, root);
                    SlotLayout layout = def.toSlotLayout();

                    LAYOUTS.put(id, layout);
                    DEFINITIONS.put(id, def);

                    // 🟢 Register dynamic group
                    String key = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
                    EquipmentGroup.register(key);
                    CatocraftMod.LOGGER.debug("[SlotLayoutLoader] Registered dynamic group '{}'", key);
                }

            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[SlotLayoutLoader] Failed to load slot layout {}: {}", id, e.toString());
            }
        });

        CatocraftMod.LOGGER.info("[SlotLayoutLoader] Loaded {} layout groups and {} definitions.",
                LAYOUTS.size(), DEFINITIONS.size());
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────

    public static SlotLayout get(EquipmentGroup group) {
        if (group == null) return null;
        return LAYOUTS.get(group.getGroupId());
    }

    public static SlotLayoutDefinition getDefinition(EquipmentGroup group) {
        if (group == null) return null;
        return DEFINITIONS.get(group.getGroupId());
    }

    public static SlotLayout get(ResourceLocation id) {
        return LAYOUTS.get(id);
    }

    public static Map<ResourceLocation, SlotLayout> getAll() {
        return Collections.unmodifiableMap(LAYOUTS);
    }

    public static SlotLayoutDefinition getDefinition(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static Map<ResourceLocation, SlotLayoutDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    public static SlotLayout getActiveLayout() {
        if (activeLayoutId != null && LAYOUTS.containsKey(activeLayoutId))
            return LAYOUTS.get(activeLayoutId);
        return LAYOUTS.values().stream().findFirst().orElse(null);
    }

    public static void setActiveLayout(ResourceLocation id) {
        activeLayoutId = id;
    }

    public static ResourceLocation getActiveLayoutId() {
        return activeLayoutId;
    }
}