package net.chriskatze.catocraftmod.command;


import com.mojang.brigadier.CommandDispatcher;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Command: /openmenu <namespace:id>
 * Opens a data-driven DynamicMenu defined in JSON.
 */
public class OpenMenuCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("openmenu")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");

                            // Load layout JSON (or fallback)
                            var layout = MenuLayoutLoader.safeLoad(id);

                            // Create a MenuProvider to build our DynamicMenu
                            MenuProvider provider = new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("Dynamic Menu: " + id.getPath());
                                }

                                @Override
                                public AbstractContainerMenu createMenu(
                                        int windowId, Inventory inv, Player p) {
                                    return new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), windowId, inv, layout);
                                }
                            };

                            // ✅ Correct type: RegistryFriendlyByteBuf
                            player.openMenu(provider, (RegistryFriendlyByteBuf buf) -> {
                                buf.writeResourceLocation(id);
                            });

                            CatocraftMod.LOGGER.info("[OpenMenuCommand] Opened dynamic menu '{}'", id);
                            return 1;
                        })));
    }
}