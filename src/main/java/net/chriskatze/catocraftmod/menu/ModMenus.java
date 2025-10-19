package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all custom menu (container) types for CatoCraft.
 * Works with NeoForge 21.1+ (DeferredHolder instead of RegistryObject).
 */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CatocraftMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentMenu>> EQUIPMENT_MENU =
            MENUS.register("equipment", () ->
                    IMenuTypeExtension.create((id, inventory, player) -> new EquipmentMenu(id, inventory))
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}