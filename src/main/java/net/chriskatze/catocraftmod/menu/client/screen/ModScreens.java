package net.chriskatze.catocraftmod.menu.client.screen;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Registers client-side GUI screens for menus.
 * Ensures EquipmentMenu opens the EquipmentScreen.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EQUIPMENT_MENU.get(), EquipmentScreen::new);
        CatocraftMod.LOGGER.info("[ModScreens] Registered EquipmentScreen for EquipmentMenu");
    }
}