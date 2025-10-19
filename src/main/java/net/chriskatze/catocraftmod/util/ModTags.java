package net.chriskatze.catocraftmod.util;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.client.Minecraft;
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

    // ─────────────────────────────────────────────
    // Item Tags
    // ─────────────────────────────────────────────
    public static final TagKey<Item> EARRINGS   = TagKey.create(Registries.ITEM, CatocraftMod.id("earrings"));
    public static final TagKey<Item> RINGS      = TagKey.create(Registries.ITEM, CatocraftMod.id("rings"));
    public static final TagKey<Item> NECKLACES  = TagKey.create(Registries.ITEM, CatocraftMod.id("necklaces"));
    public static final TagKey<Item> SOUL_STONES = TagKey.create(Registries.ITEM, CatocraftMod.id("soul_stones"));

    public static final TagKey<Item> REVELATION_ITEMS     = TagKey.create(Registries.ITEM, CatocraftMod.id("revelation_items"));
    public static final TagKey<Item> REINFORCEMENT_ITEMS  = TagKey.create(Registries.ITEM, CatocraftMod.id("reinforcement_items"));
    public static final TagKey<Item> GATHERING_ITEMS      = TagKey.create(Registries.ITEM, CatocraftMod.id("gathering_items"));
    public static final TagKey<Item> PROSPERITY_TOOLS     = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_tools"));
    public static final TagKey<Item> PROSPERITY_SWORDS    = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_swords"));
    public static final TagKey<Item> ATTRACTION_TOOLS     = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_tools"));
    public static final TagKey<Item> ATTRACTION_SWORDS    = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_swords"));
    public static final TagKey<Item> PROSPERITY_AFFECTED  = TagKey.create(Registries.ITEM, CatocraftMod.id("prosperity_affected"));
    public static final TagKey<Item> ATTRACTION_AFFECTED  = TagKey.create(Registries.ITEM, CatocraftMod.id("attraction_affected"));

    // ─────────────────────────────────────────────
    // Block Tags
    // ─────────────────────────────────────────────
    public static final TagKey<Block> REVELATION_ORES = TagKey.create(Registries.BLOCK, CatocraftMod.id("revelation_ores"));

    // ─────────────────────────────────────────────
    // HolderSets (runtime)
    // ─────────────────────────────────────────────
    public static HolderSet<Item> EARRINGS_HOLDER;
    public static HolderSet<Item> RINGS_HOLDER;
    public static HolderSet<Item> NECKLACES_HOLDER;
    public static HolderSet<Item> SOUL_STONES_HOLDER;
    public static HolderSet<Item> REVELATION_ITEMS_HOLDER;
    public static HolderSet<Item> REINFORCEMENT_ITEMS_HOLDER;
    public static HolderSet<Item> GATHERING_ITEMS_HOLDER;
    public static HolderSet<Item> PROSPERITY_ITEMS_HOLDER;
    public static HolderSet<Item> ATTRACTION_ITEMS_HOLDER;
    public static HolderSet<Item> PROSPERITY_AFFECTED_HOLDER;
    public static HolderSet<Item> ATTRACTION_AFFECTED_HOLDER;

    // ─────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────
    public static void initHolderSets(HolderGetter<Item> items) {
        EARRINGS_HOLDER    = directFromTag(items, EARRINGS);
        RINGS_HOLDER       = directFromTag(items, RINGS);
        NECKLACES_HOLDER   = directFromTag(items, NECKLACES);
        SOUL_STONES_HOLDER = directFromTag(items, SOUL_STONES);

        REVELATION_ITEMS_HOLDER    = directFromTag(items, REVELATION_ITEMS);
        REINFORCEMENT_ITEMS_HOLDER = directFromTag(items, REINFORCEMENT_ITEMS);
        GATHERING_ITEMS_HOLDER     = directFromTag(items, GATHERING_ITEMS);
        PROSPERITY_ITEMS_HOLDER    = directFromTag(items, PROSPERITY_TOOLS, PROSPERITY_SWORDS);
        ATTRACTION_ITEMS_HOLDER    = directFromTag(items, ATTRACTION_TOOLS, ATTRACTION_SWORDS);
        PROSPERITY_AFFECTED_HOLDER = directFromTag(items, PROSPERITY_AFFECTED);
        ATTRACTION_AFFECTED_HOLDER = directFromTag(items, ATTRACTION_AFFECTED);
    }

    public static void initHolderSets(HolderLookup.RegistryLookup<Item> items) {
        EARRINGS_HOLDER    = directFromTag(items, EARRINGS);
        RINGS_HOLDER       = directFromTag(items, RINGS);
        NECKLACES_HOLDER   = directFromTag(items, NECKLACES);
        SOUL_STONES_HOLDER = directFromTag(items, SOUL_STONES);

        REVELATION_ITEMS_HOLDER    = directFromTag(items, REVELATION_ITEMS);
        REINFORCEMENT_ITEMS_HOLDER = directFromTag(items, REINFORCEMENT_ITEMS);
        GATHERING_ITEMS_HOLDER     = directFromTag(items, GATHERING_ITEMS);
        PROSPERITY_ITEMS_HOLDER    = directFromTag(items, PROSPERITY_TOOLS, PROSPERITY_SWORDS);
        ATTRACTION_ITEMS_HOLDER    = directFromTag(items, ATTRACTION_TOOLS, ATTRACTION_SWORDS);
        PROSPERITY_AFFECTED_HOLDER = directFromTag(items, PROSPERITY_AFFECTED);
        ATTRACTION_AFFECTED_HOLDER = directFromTag(items, ATTRACTION_AFFECTED);
    }

    // ─────────────────────────────────────────────
    // Lazy fallback
    // ─────────────────────────────────────────────
    private static boolean tryLazyInit() {
        // Only runs on the physical client; skip if dedicated server
        if (!net.neoforged.fml.loading.FMLLoader.getDist().isClient()) {
            return false;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ITEM);
                initHolderSets(lookup);
                CatocraftMod.LOGGER.info("[Catocraft] Lazy-initialized ModTags via client registry access.");
                return true;
            }
        } catch (Throwable ignored) {
            // Avoid crashes if called too early during bootstrap
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static HolderSet<Item> directFromTag(HolderGetter<Item> items, TagKey<Item>... tags) {
        Holder<Item>[] holders = Stream.of(tags)
                .flatMap(tag -> items.getOrThrow(tag).stream())
                .toArray(Holder[]::new);
        return HolderSet.direct(holders);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static HolderSet<Item> directFromTag(HolderLookup.RegistryLookup<Item> items, TagKey<Item>... tags) {
        Holder<Item>[] holders = Stream.of(tags)
                .flatMap(tag -> items.get(tag).orElseThrow().stream())
                .toArray(Holder[]::new);
        return HolderSet.direct(holders);
    }

    // ─────────────────────────────────────────────
    // Safe getters (with lazy init)
    // ─────────────────────────────────────────────
    private static HolderSet<Item> safe(HolderSet<Item> holder, String name) {
        if (holder == null && !tryLazyInit()) {
            return HolderSet.direct();
        }
        return holder;
    }

    public static HolderSet<Item> getAttractionItemsHolder() {
        return safe(ATTRACTION_ITEMS_HOLDER, "ATTRACTION_ITEMS_HOLDER");
    }
    public static HolderSet<Item> getProsperityItemsHolder() {
        return safe(PROSPERITY_ITEMS_HOLDER, "PROSPERITY_ITEMS_HOLDER");
    }
    public static HolderSet<Item> getGatheringItemsHolder() {
        return safe(GATHERING_ITEMS_HOLDER, "GATHERING_ITEMS_HOLDER");
    }
    public static HolderSet<Item> getReinforcementItemsHolder() {
        return safe(REINFORCEMENT_ITEMS_HOLDER, "REINFORCEMENT_ITEMS_HOLDER");
    }
    public static HolderSet<Item> getRevelationItemsHolder() {
        return safe(REVELATION_ITEMS_HOLDER, "REVELATION_ITEMS_HOLDER");
    }
    public static HolderSet<Item> getEarringsHolder() {
        return safe(EARRINGS_HOLDER, "EARRINGS_HOLDER");
    }
    public static HolderSet<Item> getRingsHolder() {
        return safe(RINGS_HOLDER, "RINGS_HOLDER");
    }
    public static HolderSet<Item> getNecklacesHolder() {
        return safe(NECKLACES_HOLDER, "NECKLACES_HOLDER");
    }
    public static HolderSet<Item> getSoulStonesHolder() {
        return safe(SOUL_STONES_HOLDER, "SOUL_STONES_HOLDER");
    }
    public static HolderSet<Item> getProsperityAffectedHolder() {
        return safe(PROSPERITY_AFFECTED_HOLDER, "PROSPERITY_AFFECTED_HOLDER");
    }
    public static HolderSet<Item> getAttractionAffectedHolder() {
        return safe(ATTRACTION_AFFECTED_HOLDER, "ATTRACTION_AFFECTED_HOLDER");
    }

    public static class Items {
        public static final TagKey<Item> EARRINGS = ModTags.EARRINGS;
        public static final TagKey<Item> RINGS = ModTags.RINGS;
        public static final TagKey<Item> NECKLACES = ModTags.NECKLACES;
        public static final TagKey<Item> SOUL_STONES = ModTags.SOUL_STONES;
    }
}