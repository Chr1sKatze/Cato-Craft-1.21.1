package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ClientMenuOpener {

    public static void open(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Try loading layout
        var layout = MenuLayoutLoader.safeLoad(id);
        if (layout == null) {
            CatocraftMod.LOGGER.error("[ClientMenuOpener] Failed to load layout: {}", id);
            return;
        }

        // Open the menu locally
        mc.setScreen(new DynamicMenuScreen(
                new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), 0, new Inventory(player), layout),
                player.getInventory(),
                Component.literal("Dynamic Menu: " + id.getPath())
        ));

        CatocraftMod.LOGGER.info("[ClientMenuOpener] Opened test menu '{}'", id);
    }
}