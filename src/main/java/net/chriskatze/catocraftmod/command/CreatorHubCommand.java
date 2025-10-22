package net.chriskatze.catocraftmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.chriskatze.catocraftmod.creatorhub.CreatorHubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CreatorHubCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creatorhub")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.execute(() -> mc.setScreen(new CreatorHubScreen()));
                        mc.player.displayClientMessage(Component.literal("Opened Creator Hub"), true);
                    }
                    return 1;
                })
        );
    }
}