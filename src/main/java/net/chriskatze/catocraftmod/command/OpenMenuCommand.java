package net.chriskatze.catocraftmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ModMenus;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.chriskatze.catocraftmod.menu.runtime.DynamicMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Set;

/**
 * Command group: /menucreator ...
 *  - /menucreator open <namespace:id>   → Opens a JSON-defined menu
 *  - /menucreator list                  → Lists available layouts
 *  - /menucreator reload                → Reloads all layouts from disk
 */
public class OpenMenuCommand {

    private static final SuggestionProvider<CommandSourceStack> LAYOUT_SUGGESTIONS =
            (ctx, builder) -> {
                for (ResourceLocation loc : MenuLayoutLoader.getAllLayouts()) {
                    builder.suggest(loc.toString());
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("menucreator")
                .requires(src -> src.hasPermission(0))

                // /menucreator open <namespace:id>
                .then(Commands.literal("open")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests(LAYOUT_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");

                                    var layout = MenuLayoutLoader.safeLoad(id);
                                    if (layout == null) {
                                        player.sendSystemMessage(Component.literal("❌ Layout not found: " + id));
                                        return 0;
                                    }

                                    MenuProvider provider = new MenuProvider() {
                                        @Override
                                        public Component getDisplayName() {
                                            return Component.literal("Menu: " + id.getPath());
                                        }

                                        @Override
                                        public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player p) {
                                            return new DynamicMenu(ModMenus.DYNAMIC_MENU.get(), windowId, inv, layout);
                                        }
                                    };

                                    player.openMenu(provider, (RegistryFriendlyByteBuf buf) -> buf.writeResourceLocation(id));
                                    player.sendSystemMessage(Component.literal("✅ Opened menu: " + id));
                                    CatocraftMod.LOGGER.info("[MenuCreatorCommand] Opened layout '{}'", id);
                                    return 1;
                                })))

                // /menucreator list
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Set<ResourceLocation> layouts = MenuLayoutLoader.getAllLayouts();
                            if (layouts.isEmpty()) {
                                player.sendSystemMessage(Component.literal("⚠️ No menu layouts found."));
                                return 0;
                            }
                            player.sendSystemMessage(Component.literal("📜 Available Menus:"));
                            for (ResourceLocation loc : layouts) {
                                player.sendSystemMessage(Component.literal(" - " + loc));
                            }
                            return 1;
                        }))

                // /menucreator reload
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            MenuLayoutLoader.reload();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("🔄 Reloaded all menu layouts."), true);
                            return 1;
                        }))
        );
    }
}