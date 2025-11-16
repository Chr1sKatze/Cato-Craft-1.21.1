package net.chriskatze.catocraftmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.runtime.MenuCreatorTestMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Command: /menucreator test
 * Opens the MenuCreatorTestMenu (safe on server + client).
 */
public class MenuCreatorTestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("menucreator")
                        .requires(src -> src.hasPermission(0))
                        .then(Commands.literal("test")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                                    // MenuProvider builds the test container
                                    MenuProvider provider = new MenuProvider() {
                                        @Override
                                        public Component getDisplayName() {
                                            return Component.literal("Menu Creator Test");
                                        }

                                        @Override
                                        public AbstractContainerMenu createMenu(
                                                int windowId, Inventory inv, Player ply) {
                                            // ✅ Pass the registered MenuType reference
                                            return new MenuCreatorTestMenu(ModMenus.TEST_MENU.get(), windowId, inv);
                                        }
                                    };

                                    // Proper NeoForge openMenu call
                                    player.openMenu(provider);

                                    CatocraftMod.LOGGER.info("[MenuCreatorTestCommand] Opened MenuCreatorTestMenu for {}", player.getName().getString());
                                    return 1;
                                })
                        )
        );
    }
}