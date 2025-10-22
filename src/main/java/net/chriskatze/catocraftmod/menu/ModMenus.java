package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all custom menu (container) types for CatoCraft.
 * Fully compatible with NeoForge 21.1+.
 */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CatocraftMod.MOD_ID);

    // ────────────────────────────────────────────────
    // Equipment Menu (existing)
    // ────────────────────────────────────────────────
    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentMenu>> EQUIPMENT_MENU =
            MENUS.register("equipment", () ->
                    IMenuTypeExtension.create((id, inventory, player) ->
                            new EquipmentMenu(id, inventory))
            );

    // ────────────────────────────────────────────────
    // Dynamic Menu (data-driven)
    // ────────────────────────────────────────────────
    public static final DeferredHolder<MenuType<?>, MenuType<DynamicMenu>> DYNAMIC_MENU =
            MENUS.register("dynamic", () ->
                    IMenuTypeExtension.create((int windowId, Inventory inv, RegistryFriendlyByteBuf buf) -> {
                        // Read layout ID sent from server
                        ResourceLocation id = buf.readResourceLocation();

                        // Load the same layout on the client side
                        var layout = MenuLayoutLoader.safeLoad(id);

                        return new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), windowId, inv, layout);
                    })
            );

    // ────────────────────────────────────────────────
    // Registration entry point
    // ────────────────────────────────────────────────
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}