package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.network.KeyPressPacket;
import net.chriskatze.catocraftmod.render.ModRenderer;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class TickHandler {

    public static final KeyMapping UPDATE_RENDERER_KEY =
            new KeyMapping("key." + CatocraftMod.MOD_ID + ".update_render", GLFW.GLFW_KEY_U, "key.categories.renderer");

    /**
     * 🟢 Registers the keybinding (called automatically on client mod init)
     */
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(UPDATE_RENDERER_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (UPDATE_RENDERER_KEY.consumeClick()) {
            KeyPressPacket.sendToServer(UPDATE_RENDERER_KEY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
       ModRenderer.updateRender();
    }
}
