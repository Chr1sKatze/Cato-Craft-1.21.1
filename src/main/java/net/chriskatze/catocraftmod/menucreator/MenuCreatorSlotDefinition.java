package net.chriskatze.catocraftmod.menucreator;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one editable slot element within the Menu Creator.
 */
public class MenuCreatorSlotDefinition {

    public String id;
    public String displayName;
    public int x;
    public int y;
    public List<String> validTags;
    public String group;
    public List<String> dependencies;
    public List<String> conflicts;

    public MenuCreatorSlotDefinition(String id, int x, int y) {
        this.id = id;
        this.displayName = id;
        this.x = x;
        this.y = y;
        this.validTags = new ArrayList<>();
        this.group = "";
        this.dependencies = new ArrayList<>();
        this.conflicts = new ArrayList<>();
    }
}