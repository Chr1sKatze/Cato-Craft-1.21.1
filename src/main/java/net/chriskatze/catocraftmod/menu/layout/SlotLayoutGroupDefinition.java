package net.chriskatze.catocraftmod.menu.layout;

public record SlotLayoutGroupDefinition(String groupName, SlotLayoutDefinition definition) {
    public SlotLayout toSlotLayout() {
        return definition.toSlotLayout();
    }
}