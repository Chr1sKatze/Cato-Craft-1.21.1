package net.chriskatze.catocraftmod.creator.hub;

import net.minecraft.client.gui.screens.Screen;
import java.util.function.Supplier;

public enum CreatorType {
    MENU("Menu Creator", "menus", () -> null), // will hook MenuCreatorScreen later
    BLOCK_ENTITY("Block Entity Creator", "blocks", () -> null),
    ENTITY("Entity Creator", "entities", () -> null);

    public final String displayName;
    public final String folderName;
    public final Supplier<Screen> screenFactory;

    CreatorType(String displayName, String folderName, Supplier<Screen> screenFactory) {
        this.displayName = displayName;
        this.folderName = folderName;
        this.screenFactory = screenFactory;
    }
}