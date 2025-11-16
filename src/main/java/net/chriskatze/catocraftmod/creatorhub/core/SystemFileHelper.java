package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * 💻 SystemFileHelper — safely opens folders or files using the OS default app.
 * Works across Windows, macOS, and Linux.
 *
 * Falls back to showing the path in chat if the OS command fails.
 */
public class SystemFileHelper {

    /**
     * Opens the given folder or file in the system’s file explorer.
     * If opening fails, the path is shown in chat.
     */
    public static void openInExplorer(File target) {
        if (target == null) return;

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", target.getAbsolutePath());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", target.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux")) {
                pb = new ProcessBuilder("xdg-open", target.getAbsolutePath());
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            pb.directory(target.isDirectory() ? target : target.getParentFile());
            pb.start();
        } catch (Exception e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("📁 " + target.getAbsolutePath()), false);
            }
        }
    }

    /**
     * Opens the containing folder of a given file and selects it, if supported.
     * (Windows-only behavior by default.)
     */
    public static void revealInExplorer(File file) {
        if (file == null) return;

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath());
            } else {
                // On non-Windows, just open the parent folder
                openInExplorer(file.getParentFile());
                return;
            }

            pb.directory(file.getParentFile());
            pb.start();
        } catch (Exception e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("📄 " + file.getAbsolutePath()), false);
            }
        }
    }
}