package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Reopens the player's last dynamic menu when they log in.
 * Gracefully handles missing or invalid layouts.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class DynamicMenuEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String lastOpened = DynamicMenuStorage.getLastOpened(player);
        if (lastOpened == null || lastOpened.isEmpty()) return;

        try {
            // Try to load that menu layout JSON
            ResourceLocation id = ResourceLocation.tryParse(lastOpened);
            var layout = MenuLayoutLoader.safeLoad(id);

            // if we only got the fallback 9x3 layout, don't reopen it automatically
            if (layout == null || layout.getSlots().isEmpty()) {
                CatocraftMod.LOGGER.warn("[DynamicMenu] Layout '{}' not found, clearing reference for {}", lastOpened, player.getName().getString());
                DynamicMenuStorage.setLastOpened(player, "");
                return;
            }

            // Reopen
            player.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Dynamic Menu: " + id.getPath());
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int windowId,
                        net.minecraft.world.entity.player.Inventory inv,
                        net.minecraft.world.entity.player.Player p) {
                    return new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), windowId, inv, layout);
                }
            });

            CatocraftMod.LOGGER.info("[DynamicMenu] ✅ Reopened layout '{}' for {}", lastOpened, player.getName().getString());

        } catch (Exception e) {
            CatocraftMod.LOGGER.warn("[DynamicMenu] Failed to reopen last layout '{}': {}", lastOpened, e.getMessage());
            DynamicMenuStorage.setLastOpened(player, ""); // clear invalid reference
        }
    }
}