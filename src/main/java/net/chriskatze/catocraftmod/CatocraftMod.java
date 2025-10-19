package net.chriskatze.catocraftmod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.client.render.OreGlowRenderer;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.debug.EquipmentDebugCommand;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.item.ModCreativeModeTabs;
import net.chriskatze.catocraftmod.item.ModItems;
import net.chriskatze.catocraftmod.menu.EquipmentMenu;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.ui.UISchemaLoader;
import net.chriskatze.catocraftmod.menu.layout.SlotLayoutLoader;
import net.chriskatze.catocraftmod.network.NetworkHandler;
import net.chriskatze.catocraftmod.sound.ModSounds;
import net.chriskatze.catocraftmod.tooltip.ClientTooltipHandler;
import net.chriskatze.catocraftmod.util.ModAttributes;
import net.chriskatze.catocraftmod.util.ModTags;
import net.chriskatze.catocraftmod.villager.ModVillagers;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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

        // ────────────────────────────────────────────────
        // Common Setup
        // ────────────────────────────────────────────────
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);

        // 🔹 Capabilities
        modEventBus.addListener(EquipmentCapabilityHandler::onRegisterCapabilities);

        // 🔹 Attributes
        ModAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributeModify);

        // 🔹 Content registration
        ModMenus.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModSounds.register(modEventBus);

        // ────────────────────────────────────────────────
        // Global Event Bus
        // ────────────────────────────────────────────────
        NeoForge.EVENT_BUS.register(this);

        // 🔹 Global runtime listeners
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // ────────────────────────────────────────────────
        // Creative Tab Setup
        // ────────────────────────────────────────────────
        modEventBus.addListener(this::addCreative);

        // ────────────────────────────────────────────────
        // Client-only setup
        // ────────────────────────────────────────────────
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.register(ClientTooltipHandler.class);
            OreGlowRenderer.registerReloadListener();

            // register JSON reload listeners for layouts + UI schemas
            NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

            // Pre-cache attributes for singleplayer debugging
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().getSingleplayerServer() != null) {
                    ModAttributes.cacheHolders(Minecraft.getInstance().getSingleplayerServer());
                    LOGGER.info("[CatocraftMod] Cached ModAttributes on client startup.");
                }
            });
        }

        LOGGER.info("[CatocraftMod] Initialization complete.");
    }

    // ────────────────────────────────────────────────
    // Setup Phase
    // ────────────────────────────────────────────────
    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AnvilConfig.loadConfig();
            LOGGER.info("[CatocraftMod] Loaded Anvil JSON configuration.");
        });
    }

    // ────────────────────────────────────────────────
    // Data Reloaders
    // ────────────────────────────────────────────────
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        // Existing SlotLayout loader
        event.addListener(new SlotLayoutLoader());

        // ✅ New UISchema loader for dynamic GUIs
        event.addListener(new UISchemaLoader());

        LOGGER.info("[CatocraftMod] Registered UISchemaLoader and SlotLayoutLoader reload listeners.");
    }

    // ────────────────────────────────────────────────
    // Creative Tab Setup
    // ────────────────────────────────────────────────
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

    // ────────────────────────────────────────────────
    // Entity Attribute Setup
    // ────────────────────────────────────────────────
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
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("open_equipmentmenu")
                        .requires(source -> source.hasPermission(0))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.openMenu(new EquipmentMenu.Provider());
                            CatocraftMod.LOGGER.info("[Debug] Opened EquipmentMenu for {}", player.getName().getString());
                            return 1;
                        })
        );

        // Equipment debug command
        EquipmentDebugCommand.register(event.getDispatcher());
        // Menu Creator command (client-only)
        if (net.neoforged.fml.loading.FMLLoader.getDist().isClient()) {
            net.chriskatze.catocraftmod.menucreator.MenuCreatorCommand.register(event.getDispatcher());
            LOGGER.info("[CatocraftMod] Registered client-only MenuCreatorCommand for generic menu creation.");
        }
    }

    // ────────────────────────────────────────────────
    // Server Events
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var server = event.getServer();

        try {
            ModAttributes.initHolders(server.registryAccess().lookupOrThrow(Registries.ATTRIBUTE));
            LOGGER.info("[CatocraftMod] Initialized ModAttributes holders on server start.");
        } catch (Exception e) {
            LOGGER.error("[CatocraftMod] ⚠ Failed to initialize ModAttributes: {}", e.toString());
        }

        try {
            var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
            ModTags.initHolderSets(items);
            LOGGER.info("[CatocraftMod] Initialized ModTags HolderSets.");
        } catch (Exception e) {
            LOGGER.warn("[CatocraftMod] ⚠ Failed to initialize ModTags HolderSets: {}", e.toString());
        }

        try {
            var enchants = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ModEnchantments.initHolders(enchants);
            LOGGER.info("[CatocraftMod] Initialized ModEnchantments holders.");
        } catch (Exception e) {
            LOGGER.error("[CatocraftMod] ⚠ Failed to initialize enchantment holders: {}", e.toString());
        }

        try {
            AnvilConfig.loadConfig();
            LOGGER.info("[CatocraftMod] Reloaded Anvil configuration on server start.");
        } catch (Exception e) {
            LOGGER.warn("[CatocraftMod] ⚠ Failed to reload Anvil configuration: {}", e.toString());
        }
    }
}