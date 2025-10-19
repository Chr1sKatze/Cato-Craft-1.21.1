package net.chriskatze.catocraftmod.menu.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable definition of a SlotLayout loaded from JSON.
 * Converts into your immutable SlotLayout record.
 *
 * JSON compatibility:
 * - Supports both legacy and modern rule keys:
 *   "tags" + "valid_items"  → merged
 *   "conflicts" + "conflicts_with" → merged
 * - Now also supports "linked_with" for linked slot groups.
 */
public class SlotLayoutDefinition {

    // ---- Base layout fields ----
    public final ResourceLocation id;
    public final int originX;
    public final int originY;
    public final int spacing;
    public final int wrapAfter;
    public final String direction;
    public final List<String> pattern;
    public final int groupOffsetX;
    public final int groupOffsetY;

    // Optional canvas size
    public final Integer canvasWidth;
    public final Integer canvasHeight;

    // Rule fields (merged & normalized)
    private final List<String> validItemsMerged; // from "valid_items" + "tags"
    private final List<String> requires;
    private final List<String> conflictsMerged;  // from "conflicts" + "conflicts_with"
    private final List<String> linkedWith;       // NEW: from "linked_with"

    // Keep legacy/raw for backward-compatibility
    public final List<String> conflictsWith;
    public final List<String> tags;

    public SlotLayoutDefinition(ResourceLocation id,
                                int originX, int originY,
                                int spacing, int wrapAfter,
                                String direction, List<String> pattern,
                                int groupOffsetX, int groupOffsetY,
                                Integer canvasWidth, Integer canvasHeight,
                                List<String> conflictsWith,
                                List<String> requires,
                                List<String> linkedWith,
                                List<String> tags,
                                List<String> validItemsMerged,
                                List<String> conflictsMerged) {
        this.id = id;
        this.originX = originX;
        this.originY = originY;
        this.spacing = spacing;
        this.wrapAfter = wrapAfter;
        this.direction = direction;
        this.pattern = pattern;
        this.groupOffsetX = groupOffsetX;
        this.groupOffsetY = groupOffsetY;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        this.conflictsWith = conflictsWith;
        this.requires = requires;
        this.linkedWith = linkedWith;
        this.tags = tags;
        this.validItemsMerged = validItemsMerged;
        this.conflictsMerged = conflictsMerged;
    }

    public static SlotLayoutDefinition fromJson(ResourceLocation id, JsonObject json) {
        JsonArray originArr = GsonHelper.getAsJsonArray(json, "origin");
        int originX = originArr.get(0).getAsInt();
        int originY = originArr.get(1).getAsInt();

        int spacing = GsonHelper.getAsInt(json, "spacing", 18);
        int wrapAfter = GsonHelper.getAsInt(json, "wrap_after", -1);
        String direction = GsonHelper.getAsString(json, "direction", "horizontal");

        int groupOffsetX = 0, groupOffsetY = 0;
        if (json.has("group_offset")) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "group_offset");
            if (arr.size() >= 2) {
                groupOffsetX = arr.get(0).getAsInt();
                groupOffsetY = arr.get(1).getAsInt();
            }
        }

        Integer canvasWidth = null;
        Integer canvasHeight = null;
        if (json.has("canvas")) {
            JsonObject canvas = GsonHelper.getAsJsonObject(json, "canvas");
            if (canvas.has("width")) canvasWidth = GsonHelper.getAsInt(canvas, "width");
            if (canvas.has("height")) canvasHeight = GsonHelper.getAsInt(canvas, "height");
        }

        List<String> pattern = readStringArray(json, "pattern");

        // ── Rule arrays ──────────────────────────────
        List<String> conflictsWith = readStringArray(json, "conflicts_with");
        List<String> conflicts     = readStringArray(json, "conflicts");
        List<String> requires      = readStringArray(json, "requires");
        List<String> linkedWith    = readStringArray(json, "linked_with");

        List<String> tags          = readStringArray(json, "tags");
        List<String> validItems    = readStringArray(json, "valid_items");

        List<String> validItemsMerged = new ArrayList<>();
        validItemsMerged.addAll(validItems);
        validItemsMerged.addAll(tags);

        List<String> conflictsMerged = new ArrayList<>();
        conflictsMerged.addAll(conflicts);
        conflictsMerged.addAll(conflictsWith);

        return new SlotLayoutDefinition(
                id, originX, originY,
                spacing, wrapAfter, direction, pattern,
                groupOffsetX, groupOffsetY,
                canvasWidth, canvasHeight,
                conflictsWith, requires, linkedWith, tags,
                List.copyOf(validItemsMerged),
                List.copyOf(conflictsMerged)
        );
    }

    private static List<String> readStringArray(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        if (json.has(key)) {
            var arr = GsonHelper.getAsJsonArray(json, key);
            for (int i = 0; i < arr.size(); i++) list.add(arr.get(i).getAsString());
        }
        return list;
    }

    /** Converts this definition into your immutable SlotLayout record. */
    public SlotLayout toSlotLayout() {
        if (!pattern.isEmpty()) {
            List<SlotLayout.Point> cells = new ArrayList<>();
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length(); col++) {
                    char c = line.charAt(col);
                    if (c == 'x' || c == 'X')
                        cells.add(new SlotLayout.Point(col, row));
                }
            }
            return new SlotLayout(
                    originX, originY, spacing,
                    LayoutDirection.HORIZONTAL, -1,
                    cells, groupOffsetX, groupOffsetY,
                    canvasWidth, canvasHeight
            );
        } else {
            LayoutDirection dir;
            try {
                dir = LayoutDirection.valueOf(direction.toUpperCase());
            } catch (IllegalArgumentException e) {
                dir = LayoutDirection.HORIZONTAL;
            }
            return new SlotLayout(
                    originX, originY, spacing,
                    dir, wrapAfter,
                    List.of(), groupOffsetX, groupOffsetY,
                    canvasWidth, canvasHeight
            );
        }
    }

    // ────────────────────────────────────────────────
    // Accessors for validator/logic
    // ────────────────────────────────────────────────

    public List<String> valid_items() { return validItemsMerged; }
    public List<String> requires()    { return requires; }
    public List<String> conflicts()   { return conflictsMerged; }
    public List<String> linked_with() { return linkedWith; }
}