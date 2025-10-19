package net.chriskatze.catocraftmod.menucreator;

import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable data structure representing one equipment menu layout.
 */
public class MenuCreatorDefinition {

    public String menuId;
    public int width;
    public int height;
    public List<MenuCreatorSlotDefinition> slots;

    public MenuCreatorDefinition(String menuId) {
        this.menuId = menuId;
        this.width = 176;   // Default typical inventory width
        this.height = 166;  // Default typical inventory height
        this.slots = new ArrayList<>();
    }
}