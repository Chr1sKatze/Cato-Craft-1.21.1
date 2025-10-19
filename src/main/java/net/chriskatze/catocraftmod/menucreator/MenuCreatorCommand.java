package net.chriskatze.catocraftmod.menucreator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MenuCreatorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("menucreator")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.argument("menu_id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String menuId = StringArgumentType.getString(ctx, "menu_id");
                                    MenuCreatorManager.open(menuId);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Opened Menu Creator for: " + menuId), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("save")
                        .executes(ctx -> {
                            MenuCreatorManager.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("Menu saved."), false);
                            return 1;
                        })
                )
                .then(Commands.literal("load")
                        .then(Commands.argument("menu_id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String menuId = StringArgumentType.getString(ctx, "menu_id");
                                    MenuCreatorManager.load(menuId);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Loaded menu: " + menuId), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}