package net.chriskatze.catocraftmod.client;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.client.screen.EarringMenuScreen;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EARRING_MENU.get(), EarringMenuScreen::new);
    }
}