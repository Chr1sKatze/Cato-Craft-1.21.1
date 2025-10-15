package net.chriskatze.catocraftmod.util;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public class ModTags {

    // Jewelelry items
    public static final TagKey<Item> EARRINGS = TagKey.create(Registries.ITEM, CatocraftMod.id("earrings"));

    // Item Tags
    public static final TagKey<Item> REVELATION_ITEMS = TagKey.create(Registries.ITEM, CatocraftMod.id("revelation_items"));
    public static final TagKey<Item> REINFORCEMENT_ITEMS = TagKey.create(Registries.ITEM, CatocraftMod.id("reinforcement_items"));
    public static final TagKey<Item> GATHERING_ITEMS = TagKey.create(Registries.ITEM, CatocraftMod.id("gathering_items"));
    public static final TagKey<Item> PROSPERITY_TOOLS = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_tools"));
    public static final TagKey<Item> PROSPERITY_SWORDS = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_swords"));
    public static final TagKey<Item> ATTRACTION_TOOLS = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_tools"));
    public static final TagKey<Item> ATTRACTION_SWORDS = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_swords"));
    public static final TagKey<Item> PROSPERITY_AFFECTED = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_affected"));
    public static final TagKey<Item> ATTRACTION_AFFECTED = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_affected"));

    // Block Tags
    public static final TagKey<Block> REVELATION_ORES = TagKey.create(Registries.BLOCK, CatocraftMod.id("revelation_ores"));


    // HolderSets
    public static HolderSet<Item> EARRINGS_HOLDER;

    public static HolderSet<Item> REVELATION_ITEMS_HOLDER;
    public static HolderSet<Item> REINFORCEMENT_ITEMS_HOLDER;
    public static HolderSet<Item> GATHERING_ITEMS_HOLDER;
    public static HolderSet<Item> PROSPERITY_ITEMS_HOLDER;
    public static HolderSet<Item> ATTRACTION_ITEMS_HOLDER;
    public static HolderSet<Item> PROSPERITY_AFFECTED_HOLDER;
    public static HolderSet<Item> ATTRACTION_AFFECTED_HOLDER;

    // ------------------- Bootstrap (HolderGetter) -------------------
    public static void initHolderSets(HolderGetter<Item> items) {
        EARRINGS_HOLDER = directFromTag(items, EARRINGS);

        REVELATION_ITEMS_HOLDER = directFromTag(items, REVELATION_ITEMS);
        REINFORCEMENT_ITEMS_HOLDER = directFromTag(items, REINFORCEMENT_ITEMS);
        GATHERING_ITEMS_HOLDER = directFromTag(items, GATHERING_ITEMS);
        PROSPERITY_ITEMS_HOLDER = directFromTag(items, PROSPERITY_TOOLS, PROSPERITY_SWORDS);
        ATTRACTION_ITEMS_HOLDER = directFromTag(items, ATTRACTION_TOOLS, ATTRACTION_SWORDS);
        PROSPERITY_AFFECTED_HOLDER = directFromTag(items, PROSPERITY_AFFECTED);
        ATTRACTION_AFFECTED_HOLDER = directFromTag(items, ATTRACTION_AFFECTED);
    }

    // ------------------- Runtime (RegistryLookup) -------------------
    public static void initHolderSets(HolderLookup.RegistryLookup<Item> items) {
        EARRINGS_HOLDER = directFromTag(items, EARRINGS);

        REVELATION_ITEMS_HOLDER = directFromTag(items, REVELATION_ITEMS);
        REINFORCEMENT_ITEMS_HOLDER = directFromTag(items, REINFORCEMENT_ITEMS);
        GATHERING_ITEMS_HOLDER = directFromTag(items, GATHERING_ITEMS);
        PROSPERITY_ITEMS_HOLDER = directFromTag(items, PROSPERITY_TOOLS, PROSPERITY_SWORDS);
        ATTRACTION_ITEMS_HOLDER = directFromTag(items, ATTRACTION_TOOLS, ATTRACTION_SWORDS);
        PROSPERITY_AFFECTED_HOLDER = directFromTag(items, PROSPERITY_AFFECTED);
        ATTRACTION_AFFECTED_HOLDER = directFromTag(items, ATTRACTION_AFFECTED);
    }

    // ------------------- Helper methods -------------------
    private static HolderSet<Item> directFromTag(HolderGetter<Item> items, TagKey<Item>... tags) {
        return HolderSet.direct(
                Stream.of(tags)
                        .flatMap(tag -> items.getOrThrow(tag).stream())
                        .toList()
                        .toArray(new Holder[0])
        );
    }

    private static HolderSet<Item> directFromTag(HolderLookup.RegistryLookup<Item> items, TagKey<Item>... tags) {
        return HolderSet.direct(
                Stream.of(tags)
                        .flatMap(tag -> items.get(tag).orElseThrow().stream())
                        .toList()
                        .toArray(new Holder[0])
        );
    }

    // ------------------- Safe getters -------------------
    public static HolderSet<Item> getRevelationItemsHolder() {
        if (REVELATION_ITEMS_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] REVELATION_ITEMS_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return REVELATION_ITEMS_HOLDER;
    }

    public static HolderSet<Item> getReinforcementItemsHolder() {
        if (REINFORCEMENT_ITEMS_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] REINFORCEMENT_ITEMS_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return REINFORCEMENT_ITEMS_HOLDER;
    }

    public static HolderSet<Item> getGatheringItemsHolder() {
        if (GATHERING_ITEMS_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] GATHERING_ITEMS_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return GATHERING_ITEMS_HOLDER;
    }

    public static HolderSet<Item> getProsperityItemsHolder() {
        if (PROSPERITY_ITEMS_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] PROSPERITY_ITEMS_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return PROSPERITY_ITEMS_HOLDER;
    }

    public static HolderSet<Item> getAttractionItemsHolder() {
        if (ATTRACTION_ITEMS_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] ATTRACTION_ITEMS_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return ATTRACTION_ITEMS_HOLDER;
    }

    public static HolderSet<Item> getProsperityAffectedHolder() {
        if (PROSPERITY_AFFECTED_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] PROSPERITY_AFFECTED_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return PROSPERITY_AFFECTED_HOLDER;
    }

    public static HolderSet<Item> getAttractionAffectedHolder() {
        if (ATTRACTION_AFFECTED_HOLDER == null) {
            CatocraftMod.LOGGER.warn("[Catocraft] ATTRACTION_AFFECTED_HOLDER accessed before initialization!");
            return HolderSet.direct();
        }
        return ATTRACTION_AFFECTED_HOLDER;
    }
}