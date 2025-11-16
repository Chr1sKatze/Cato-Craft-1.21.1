package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 🌍 Global toast manager — persistent, smooth, bottom-to-top stacked notifications.
 * - Fade + slide animations.
 * - When a toast expires, the others ease upward smoothly (no jump).
 */
public class CreatorHubToastManager {

    private static final List<ToastEntry> TOASTS = new ArrayList<>();

    /** Add a new toast to the stack (visible across all Creator Hub screens). */
    public static void showToast(String text, int color) {
        TOASTS.add(new ToastEntry(text, color, System.currentTimeMillis()));
    }

    /** Called every frame from BaseCreatorScreen.render(). */
    public static void renderAll(GuiGraphics gfx, int screenHeight, int fontLineHeight) {
        if (TOASTS.isEmpty()) return;

        long now = System.currentTimeMillis();
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int baseY = screenHeight / 2 + 60;
        int spacing = fontLineHeight + 6;

        // Clean up expired toasts early
        Iterator<ToastEntry> cleanup = TOASTS.iterator();
        while (cleanup.hasNext()) {
            ToastEntry t = cleanup.next();
            float elapsed = now - t.startTime;
            if (elapsed >= 4000f) cleanup.remove();
        }

        // Draw oldest → newest (bottommost toast last)
        for (int i = 0; i < TOASTS.size(); i++) {
            ToastEntry toast = TOASTS.get(i);

            float elapsed = now - toast.startTime;
            float duration = 4000f;
            float progress = elapsed / duration;

            // 🔆 Fade in/out timing (10% in, 15% out)
            float alpha;
            if (progress < 0.1f) alpha = progress / 0.1f;
            else if (progress > 0.85f) alpha = (1f - progress) / 0.15f;
            else alpha = 1f;

            // 🔒 Clamp to zero and skip drawing if invisible
            if (alpha <= 0.01f) continue;

            int a = (int) (alpha * 255);
            int colorWithAlpha = (toast.color & 0x00FFFFFF) | (a << 24);

            // Slide-in easing (bottom → up)
            float easedIn = 1f - (float) Math.pow(1f - Math.min(progress / 0.25f, 1f), 3);
            int slideInOffset = (int) (24 * (1f - easedIn));

            // Smooth “settle upward” motion
            float targetY = baseY - (TOASTS.size() - 1 - i) * spacing;
            toast.currentY += (targetY - toast.currentY) * 0.15f;

            // 🧩 Only draw if truly visible
            int drawY = (int) (toast.currentY + slideInOffset);
            gfx.drawCenteredString(
                    Minecraft.getInstance().font,
                    toast.text,
                    screenWidth / 2,
                    drawY,
                    colorWithAlpha
            );
        }
    }

    // ─────────────────────────────
    // Internal entry class
    // ─────────────────────────────
    private static class ToastEntry {
        final String text;
        final int color;
        final long startTime;
        float currentY; // animated position

        ToastEntry(String text, int color, long startTime) {
            this.text = text;
            this.color = color;
            this.startTime = startTime;
            this.currentY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 + 60;
        }
    }
}