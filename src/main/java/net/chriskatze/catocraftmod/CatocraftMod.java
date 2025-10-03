package net.chriskatze.catocraftmod;

import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.enchantment.custom.DynamicCustomAnvilHandler;
import net.chriskatze.catocraftmod.item.ModCreativeModeTabs;
import net.chriskatze.catocraftmod.item.ModItems;
import net.chriskatze.catocraftmod.sound.ModSounds;
import net.chriskatze.catocraftmod.villager.ModVillagers;
import net.minecraft.world.item.CreativeModeTabs;
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
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        AnvilConfig.loadConfig();
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