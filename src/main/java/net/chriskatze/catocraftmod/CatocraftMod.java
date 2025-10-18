package net.chriskatze.catocraftmod;

import net.chriskatze.catocraftmod.block.ModBlocks;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
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

        ModAttributes.ATTRIBUTES.register(modEventBus);

        // ✅ Correct: register attribute modification event on MOD BUS
        modEventBus.addListener(this::onEntityAttributeModify);

        // ✅ Runtime events go on NeoForge EVENT BUS
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // ------------------- Content registration -----------------------
        ModMenus.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModSounds.register(modEventBus);

        // ------------------- Creative tab setup -------------------------
        modEventBus.addListener(this::addCreative);

        // ------------------- Client-only registrations ------------------
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.register(ClientTooltipHandler.class);
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().getSingleplayerServer() != null) {
                    ModAttributes.cacheHolders(Minecraft.getInstance().getSingleplayerServer());
                    LOGGER.info("[CatocraftMod] Cached ModAttributes on client startup.");
                }
            });
        }
    }

    // ---------------------------------------------------------------------
    // Setup Phase
    // ---------------------------------------------------------------------
    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
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
    // ✅ Attribute Registration for All Living Entities
    // ---------------------------------------------------------------------
    private void onEntityAttributeModify(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            if (type.getCategory() != null) {
                event.add(type, ModAttributes.FIRE_RESIST);
                event.add(type, ModAttributes.FIRE_POWER);
                event.add(type, ModAttributes.FROST_RESIST);
                event.add(type, ModAttributes.FROST_POWER);
                event.add(type, ModAttributes.ARCANE_RESIST);
                event.add(type, ModAttributes.ARCANE_POWER);
                event.add(type, ModAttributes.HEALING_POWER);
                event.add(type, ModAttributes.MANA_REGEN);
            }
        });
        LOGGER.info("[CatocraftMod] Registered elemental attributes for all living entities.");
    }

    private void registerCommands(RegisterCommandsEvent event) {
    }

    // ---------------------------------------------------------------------
    // Server Events
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var server = event.getServer();

        // ============================================================
        // 🔹 1. Initialize Attributes
        // ============================================================
        try {
            ModAttributes.cacheHolders(server);
            LOGGER.info("[CatocraftMod] Cached ModAttributes holders on server start.");
        } catch (Exception e) {
            LOGGER.error("[CatocraftMod] ⚠ Failed to cache ModAttributes: {}", e.toString());
        }

        // ============================================================
        // 🔹 2. Initialize Item and Tag Holders
        // ============================================================
        try {
            var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
            ModTags.initHolderSets(items);
            LOGGER.info("[CatocraftMod] Initialized ModTags HolderSets.");
        } catch (Exception e) {
            LOGGER.warn("[CatocraftMod] ⚠ Failed to initialize ModTags HolderSets: {}", e.toString());
        }

        // ============================================================
        // 🔹 3. Initialize Enchantment Holders
        // ============================================================
        try {
            var enchants = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            ModEnchantments.PROSPERITY.setHolder(
                    enchants.get(ModEnchantments.PROSPERITY.getKey())
                            .orElseGet(() -> {
                                LOGGER.warn("[CatocraftMod] ⚠ Missing enchantment: prosperity");
                                return null;
                            })
            );

            ModEnchantments.ATTRACTION.setHolder(
                    enchants.get(ModEnchantments.ATTRACTION.getKey())
                            .orElseGet(() -> {
                                LOGGER.warn("[CatocraftMod] ⚠ Missing enchantment: attraction");
                                return null;
                            })
            );

            ModEnchantments.GATHERING.setHolder(
                    enchants.get(ModEnchantments.GATHERING.getKey())
                            .orElseGet(() -> {
                                LOGGER.warn("[CatocraftMod] ⚠ Missing enchantment: gathering");
                                return null;
                            })
            );

            ModEnchantments.REINFORCEMENT.setHolder(
                    enchants.get(ModEnchantments.REINFORCEMENT.getKey())
                            .orElseGet(() -> {
                                LOGGER.warn("[CatocraftMod] ⚠ Missing enchantment: reinforcement");
                                return null;
                            })
            );

            ModEnchantments.REVELATION.setHolder(
                    enchants.get(ModEnchantments.REVELATION.getKey())
                            .orElseGet(() -> {
                                LOGGER.warn("[CatocraftMod] ⚠ Missing enchantment: revelation");
                                return null;
                            })
            );

            LOGGER.info("[CatocraftMod] Initialized ModEnchantments Holders.");

        } catch (Exception e) {
            LOGGER.error("[CatocraftMod] ⚠ Failed to initialize enchantment holders: {}", e.toString());
        }

        // ============================================================
        // 🔹 4. Load Configuration
        // ============================================================
        try {
            AnvilConfig.loadConfig();
            LOGGER.info("[CatocraftMod] Reloaded Anvil configuration on server start.");
        } catch (Exception e) {
            LOGGER.warn("[CatocraftMod] ⚠ Failed to reload Anvil configuration: {}", e.toString());
        }
    }
}