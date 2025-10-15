package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all custom menu (container) types for CatoCraft.
 * This ensures that your custom inventories work on both server and client.
 */
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CatocraftMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EarringMenu>> EARRING_MENU =
            MENUS.register("earring_menu",
                    () -> new MenuType<>(
                            (id, inv) -> new EarringMenu(id, inv, inv.player),
                            FeatureFlags.VANILLA_SET
                    ));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}