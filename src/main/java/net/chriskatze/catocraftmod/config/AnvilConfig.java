package net.chriskatze.catocraftmod.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class AnvilConfig {

    // ---------------- CONFIG STRUCTS ----------------
    public static class RepairInfo {
        public final Item repairItem;
        public final double repairPercentage;

        public RepairInfo(Item repairItem, double repairPercentage) {
            this.repairItem = repairItem;
            this.repairPercentage = repairPercentage;
        }
    }

    public static class ItemConfigEntry {
        public String itemId;
        public RepairInfo repairInfo;
        public int maxEnchantments;
    }

    // ---------------- CONFIG DATA ----------------
    public static final Map<Item, RepairInfo> itemRepairMap = new HashMap<>();
    public static final Map<Item, Integer> itemMaxEnchantments = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_ARRAY_TYPE = new TypeToken<ItemConfigEntry[]>() {}.getType();

    // ---------------- INITIALIZATION ----------------
    public static void loadConfig() {
        try {
            File configDir = new File("config/catocraftmod");
            if (!configDir.exists()) configDir.mkdirs();
            File configFile = new File(configDir, "anvil.json");

            if (!configFile.exists()) createDefaultConfig(configFile);

            JsonArray array = JsonParser.parseReader(new FileReader(configFile)).getAsJsonArray();

            itemRepairMap.clear();
            itemMaxEnchantments.clear();

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String itemId = obj.has("itemId") ? obj.get("itemId").getAsString() : null;
                String repairItemId = obj.has("repairItem") ? obj.get("repairItem").getAsString() : null;
                double repairPercentage = obj.has("repairPercentage") ? obj.get("repairPercentage").getAsDouble() : 0.0;
                int maxEnchantments = obj.has("maxEnchantments") ? obj.get("maxEnchantments").getAsInt() : Integer.MAX_VALUE;

                if (itemId == null) continue;
                Item item = getItemById(itemId);
                if (item == null || item == Items.AIR) continue;

                if (repairItemId != null) {
                    Item repairItem = getItemById(repairItemId);
                    if (repairItem != null && repairItem != Items.AIR) {
                        itemRepairMap.put(item, new RepairInfo(repairItem, repairPercentage));
                    }
                }

                if (maxEnchantments > 0) {
                    itemMaxEnchantments.put(item, maxEnchantments);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig(File configFile) {
        try {
            // ---------------- Named helper for readability ----------------
            class DefaultItemData {
                String itemId;
                String repairItemId;
                double repairPercentage;
                int maxEnchantments;

                DefaultItemData(String itemId, String repairItemId, double repairPercentage, int maxEnchantments) {
                    this.itemId = itemId;
                    this.repairItemId = repairItemId;
                    this.repairPercentage = repairPercentage;
                    this.maxEnchantments = maxEnchantments;
                }
            }

            DefaultItemData[] defaultData = new DefaultItemData[]{
                    new DefaultItemData("minecraft:iron_pickaxe", "minecraft:iron_ingot", 0.05, 3),
                    new DefaultItemData("minecraft:diamond_sword", "minecraft:diamond", 0.05, 3),
                    new DefaultItemData("minecraft:netherite_chestplate", "minecraft:netherite_ingot", 0.25, 4),
            };

            // Convert DefaultItemData to fully labeled ItemConfigEntry objects
            List<ItemConfigEntry> defaults = new ArrayList<>();
            for (DefaultItemData data : defaultData) {
                ItemConfigEntry entry = new ItemConfigEntry();
                entry.itemId = data.itemId;
                entry.repairInfo = new RepairInfo(getItemById(data.repairItemId), data.repairPercentage);
                entry.maxEnchantments = data.maxEnchantments;
                defaults.add(entry);
            }

            // Write JSON with pretty printing
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonArray array = new JsonArray();
                for (ItemConfigEntry entry : defaults) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("itemId", entry.itemId);
                    obj.addProperty("repairItem", BuiltInRegistries.ITEM.getKey(entry.repairInfo.repairItem).toString());
                    obj.addProperty("repairPercentage", entry.repairInfo.repairPercentage);
                    obj.addProperty("maxEnchantments", entry.maxEnchantments);
                    array.add(obj);
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(array, writer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- UTILITIES ----------------
    private static Item getItemById(String id) {
        try {
            return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(id));
        } catch (Exception e) {
            return Items.AIR;
        }
    }

    public static RepairInfo getRepairInfo(Item item) {
        return itemRepairMap.get(item);
    }

    public static int getMaxEnchantments(Item item) {
        return itemMaxEnchantments.getOrDefault(item, Integer.MAX_VALUE);
    }
}