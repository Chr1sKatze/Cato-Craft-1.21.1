package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenu;
import net.chriskatze.catocraftmod.menu.runtime.MenuCreatorTestMenu;
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
                        // ✅ Safe default layout ID if none provided
                        ResourceLocation layoutId = CatocraftMod.id("default_layout");

                        if (buf != null) {
                            try {
                                layoutId = buf.readResourceLocation();
                            } catch (Exception e) {
                                CatocraftMod.LOGGER.warn("[ModMenus] Failed to read layout ID from buffer: {}", e.toString());
                            }
                        } else {
                            CatocraftMod.LOGGER.warn("[ModMenus] Received null buffer when opening DynamicMenu; using fallback '{}'", layoutId);
                        }

                        // Load layout client-side (safeLoad handles missing files gracefully)
                        var layout = MenuLayoutLoader.safeLoad(layoutId);

                        return new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), windowId, inv, layout);
                    })
            );

    // ────────────────────────────────────────────────
    // Menu Creator Menu (for testing + editor linkage)
    // ────────────────────────────────────────────────
    public static final DeferredHolder<MenuType<?>, MenuType<MenuCreatorMenu>> MENU_CREATOR =
            MENUS.register("menu_creator", () ->
                    IMenuTypeExtension.create((id, inventory, player) ->
                            new MenuCreatorMenu(id, inventory))
            );

    // ────────────────────────────────────────────────
    // 🧪 Menu Creator Test Menu (runtime test screen)
    // ────────────────────────────────────────────────
    public static final DeferredHolder<MenuType<?>, MenuType<MenuCreatorTestMenu>> TEST_MENU =
            MENUS.register("menu_creator_test", () ->
                    IMenuTypeExtension.create((id, inventory, player) ->
                            new MenuCreatorTestMenu(ModMenus.TEST_MENU.get(), id, inventory)
                    )
            );

    // ────────────────────────────────────────────────
    // Registration entry point
    // ────────────────────────────────────────────────
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}