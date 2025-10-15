package net.chriskatze.catocraftmod.client;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.network.OpenEarringMenuPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybinding registration and key press detection for opening the earring inventory.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class InventoryOpener {

    // 🔹 The keybind shown in Minecraft's Controls menu
    public static final KeyMapping OPEN_EARRING_MENU_KEY =
            new KeyMapping("key.catocraftmod.open_earrings", GLFW.GLFW_KEY_B, "key.categories.inventory");

    /**
     * 🟢 Registers the keybinding (called automatically on client mod init)
     */
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EARRING_MENU_KEY);
    }

    /**
     * 🟣 Handles key press detection during gameplay
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        if (OPEN_EARRING_MENU_KEY.consumeClick()) {
            // 🧠 Debug Log
            CatocraftMod.LOGGER.info("[Catocraft] Open Earrings key pressed — sending request to server");

            // 🗨️ Show quick client feedback
            player.displayClientMessage(Component.literal("🟣 Requesting Earring Menu..."), true);

            // 🚀 Send packet to the server
            PacketDistributor.sendToServer(new OpenEarringMenuPacket());
        }
    }
}