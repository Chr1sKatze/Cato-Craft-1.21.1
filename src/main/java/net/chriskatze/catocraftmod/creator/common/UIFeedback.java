package net.chriskatze.catocraftmod.creator.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class UIFeedback {

    // Each toast type just needs a unique numeric id.
    private static final SystemToast.SystemToastId INFO_ID = new SystemToast.SystemToastId(1L);
    private static final SystemToast.SystemToastId ERROR_ID = new SystemToast.SystemToastId(2L);

    public static void showInfo(String title, String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(new SystemToast(
                INFO_ID,
                Component.literal(title),
                Component.literal(message)
        ));
    }

    public static void showError(String title, String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(new SystemToast(
                ERROR_ID,
                Component.literal(title),
                Component.literal(message)
        ));
    }
}