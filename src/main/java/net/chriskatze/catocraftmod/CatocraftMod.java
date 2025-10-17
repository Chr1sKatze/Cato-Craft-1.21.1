package net.chriskatze.catocraftmod;

import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.client.render.OreGlowRenderer;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.item.ModCreativeModeTabs;
import net.chriskatze.catocraftmod.item.ModItems;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.network.NetworkHandler;
import net.chriskatze.catocraftmod.sound.ModSounds;
import net.chriskatze.catocraftmod.tooltip.ClientTooltipHandler;
import net.chriskatze.catocraftmod.util.ModAttributes;
import net.chriskatze.catocraftmod.util.ModTags;
import net.chriskatze.catocraftmod.villager.ModVillagers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Main mod entrypoint for CatoCraft.
 * Handles registration of content, capabilities, networking, and events.
 */
@Mod(CatocraftMod.MOD_ID)
public class CatocraftMod {
    public static final String MOD_ID = "catocraftmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.tryParse(MOD_ID + ":" + path);
    }

    public CatocraftMod(IEventBus modEventBus, ModContainer modContainer) {

        // ------------------- Core lifecycle listeners -------------------
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);
        NeoForge.EVENT_BUS.register(this);

        // ------------------- Content registration -----------------------
        ModMenus.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModSounds.register(modEventBus);

        ModAttributes.ATTRIBUTES.register(modEventBus);

        // ------------------- Creative tab setup -------------------------
        modEventBus.addListener(this::addCreative);


        // ------------------- Client-only registrations ------------------
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.register(ClientTooltipHandler.class);
            OreGlowRenderer.registerReloadListener();
        }
    }

    // ---------------------------------------------------------------------
    // Setup Phase
    // ---------------------------------------------------------------------
    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Load your custom JSON config
            AnvilConfig.loadConfig();
            LOGGER.info("[CatocraftMod] Loaded Anvil JSON configuration.");
        });
    }

    // ---------------------------------------------------------------------
    // Creative Tab Content
    // ---------------------------------------------------------------------
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.STEEL_INGOT);
            event.accept(ModItems.STEEL_NUGGET);
            event.accept(ModItems.RAW_STEEL);
            event.accept(ModItems.PLATINUM_INGOT);
            event.accept(ModItems.PLATINUM_NUGGET);
            event.accept(ModItems.RAW_PLATINUM);
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.STEEL_BLOCK);
            event.accept(ModBlocks.PLATINUM_BLOCK);
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.PLATINUM_ORE);
            event.accept(ModBlocks.PLATINUM_DEEPSLATE_ORE);
        }
    }

    // ---------------------------------------------------------------------
    // Server Events
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Reload config safely at server start
        AnvilConfig.loadConfig();

        // ------------------- Item Holders -------------------
        HolderLookup.RegistryLookup<Item> items = event.getServer().registryAccess().lookupOrThrow(Registries.ITEM);
        ModTags.initHolderSets(items);
        LOGGER.info("[CatocraftMod] ModTags HolderSets initialized.");

        // ------------------- Enchantments -------------------
        HolderLookup.RegistryLookup<Enchantment> enchants =
                event.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ModEnchantments.PROSPERITY.setHolder(enchants.get(ModEnchantments.PROSPERITY.getKey()).orElseThrow());
        ModEnchantments.ATTRACTION.setHolder(enchants.get(ModEnchantments.ATTRACTION.getKey()).orElseThrow());
        ModEnchantments.GATHERING.setHolder(enchants.get(ModEnchantments.GATHERING.getKey()).orElseThrow());
        ModEnchantments.REINFORCEMENT.setHolder(enchants.get(ModEnchantments.REINFORCEMENT.getKey()).orElseThrow());
        ModEnchantments.REVELATION.setHolder(enchants.get(ModEnchantments.REVELATION.getKey()).orElseThrow());

        LOGGER.info("[CatocraftMod] ModEnchantments Holders initialized.");
    }
}