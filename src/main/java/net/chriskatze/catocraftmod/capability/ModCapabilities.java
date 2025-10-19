package net.chriskatze.catocraftmod.capability;


import net.chriskatze.catocraftmod.CatocraftMod;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Central registry for custom player capabilities.
 */
public class ModCapabilities {

    public static final EntityCapability<PlayerEquipmentCapability, Void> EARRING_CAP =
            EntityCapability.createVoid(CatocraftMod.id("earring_cap"), PlayerEquipmentCapability.class);

    public static final EntityCapability<PlayerEquipmentCapability, Void> SOULSTONE_CAP =
            EntityCapability.createVoid(CatocraftMod.id("soulstone_cap"), PlayerEquipmentCapability.class);
}