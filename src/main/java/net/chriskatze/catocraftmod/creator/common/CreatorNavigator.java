package net.chriskatze.catocraftmod.creator.common;

import net.chriskatze.catocraftmod.creator.hub.CreatorType;
import net.chriskatze.catocraftmod.creator.menu.MenuCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class CreatorNavigator {

    public static void openEditor(CreatorType type, String name) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = switch (type) {
            case MENU -> new MenuCreatorScreen(name);
            case BLOCK_ENTITY, ENTITY -> null;
            default -> null;
        };
        if (screen != null) mc.setScreen(screen);
    }
}