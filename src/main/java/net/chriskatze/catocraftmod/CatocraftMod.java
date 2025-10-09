package net.chriskatze.catocraftmod;

import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.item.ModCreativeModeTabs;
import net.chriskatze.catocraftmod.item.ModItems;
import net.chriskatze.catocraftmod.sound.ModSounds;
import net.chriskatze.catocraftmod.tooltip.ClientTooltipHandler;
import net.chriskatze.catocraftmod.villager.ModVillagers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(CatocraftMod.MOD_ID)
public class CatocraftMod {
    public static final String MOD_ID = "catocraftmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final TagKey<Item> GATHERING_TOOLS_TAG = TagKey.create(Registries.ITEM, id("gathering_tools"));

    public static ResourceLocation id(String path) {
        return ResourceLocation.tryParse(MOD_ID + ":" + path);
    }

    public CatocraftMod(IEventBus modEventBus, ModContainer modContainer) {

        // Register events
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        // Register deferred content
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModSounds.register(modEventBus);

        // Creative tab listener
        modEventBus.addListener(this::addCreative);

        // Config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Dependency check
        checkDependencies();

        // ---------------- Client-side registration ----------------
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.register(ClientTooltipHandler.class);
        }
    }

    private void checkDependencies() {
        // Core / common mods
        requireMinVersion("spell_engine", "1.8.2", true);
        requireMinVersion("spell_power", "1.4.0", true);
        requireMinVersion("fabric_api", "0.115.6+2.1.1+1.21.1", true);
        requireMinVersion("accessories", "1.1.0-beta.52", true);

        // Client-only mods
        if (FMLLoader.getDist().isClient()) {
            requireMinVersion("playeranimator", "2.0.1", true);
        }
    }

    /**
     * Checks that the mod is loaded and meets the minimum version.
     *
     * @param modId The exact mod ID.
     * @param minVersion Minimum required version (e.g., "15.0.140").
     * @param mandatory True if the mod must be present, false if optional.
     */
    private void requireMinVersion(String modId, String minVersion, boolean mandatory) {
        ModList mods = ModList.get();

        mods.getModContainerById(modId).ifPresentOrElse(container -> {
            String loadedVersion = container.getModInfo().getVersion().toString();
            if (compareVersions(loadedVersion, minVersion) < 0) {
                throw new RuntimeException(
                        String.format("❌ CatoCraft requires %s %s+, but found %s. Please update the mod.",
                                modId, minVersion, loadedVersion)
                );
            }
        }, () -> {
            if (mandatory) {
                throw new RuntimeException(
                        String.format("❌ CatoCraft requires %s %s+, but it is missing!", modId, minVersion)
                );
            } else {
                LOGGER.warn("⚠ Optional mod {} {}+ is missing, skipping.", modId, minVersion);
            }
        });
    }

    /**
     * Compares two version strings like "15.0.140" or "0.12.15-beta.1".
     * Returns negative if current < required, zero if equal, positive if current > required.
     */
    private int compareVersions(String current, String required) {
        // Strip non-digit prefixes like "+", "-beta" for comparison
        String[] currParts = current.replaceAll("[^0-9.]", "").split("\\.");
        String[] reqParts = required.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(currParts.length, reqParts.length);
        for (int i = 0; i < length; i++) {
            int curr = i < currParts.length ? parseIntSafe(currParts[i]) : 0;
            int req = i < reqParts.length ? parseIntSafe(reqParts[i]) : 0;
            if (curr != req) return curr - req;
        }
        return 0;
    }

    /** Safely parses integers, returns 0 on failure. */
    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        AnvilConfig.loadConfig();

        // Debug check for tag
        if (GATHERING_TOOLS_TAG != null) {
            LOGGER.info("[CatocraftMod] Gathering tools tag is registered: {}", GATHERING_TOOLS_TAG.location());
        } else {
            LOGGER.warn("[CatocraftMod] Gathering tools tag could not be registered!");
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.STEEL_INGOT);
            event.accept(ModItems.STEEL_NUGGET);
            event.accept(ModItems.RAW_STEEL);
            event.accept(ModItems.PLATINUM_INGOT);
            event.accept(ModItems.PLATINUM_NUGGET);
            event.accept(ModItems.RAW_PLATINUM);
        }

        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.STEEL_BLOCK);
            event.accept(ModBlocks.PLATINUM_BLOCK);
        }

        if(event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.PLATINUM_ORE);
            event.accept(ModBlocks.PLATINUM_DEEPSLATE_ORE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // No changes needed
    }
}