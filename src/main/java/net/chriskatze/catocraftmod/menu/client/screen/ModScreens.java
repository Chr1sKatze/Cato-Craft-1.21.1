package net.chriskatze.catocraftmod.menu.client.screen;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenuScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.chriskatze.catocraftmod.menu.runtime.MenuCreatorTestScreen;

/**
 * 🎨 ModScreens — client-side GUI registration
 *
 * Registers all menu screen factories used by the mod.
 * Primarily supports the new JSON-driven DynamicMenu system.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        // --- JSON-driven dynamic menus ---
        event.register(ModMenus.DYNAMIC_MENU.get(), DynamicMenuScreen::new);
        CatocraftMod.LOGGER.info("[ModScreens] Registered DynamicMenuScreen for DYNAMIC_MENU");

        // --- 🧪 Menu Creator Test Screen ---
        event.register(ModMenus.TEST_MENU.get(), MenuCreatorTestScreen::new);
        CatocraftMod.LOGGER.info("[ModScreens] Registered MenuCreatorTestScreen for TEST_MENU");

        // 🧩 Optional future creator hub tools
        // event.register(ModMenus.BLOCK_ENTITY_CREATOR.get(), BlockEntityCreatorScreen::new);
        // event.register(ModMenus.ENTITY_CREATOR.get(), EntityCreatorScreen::new);
    }
}