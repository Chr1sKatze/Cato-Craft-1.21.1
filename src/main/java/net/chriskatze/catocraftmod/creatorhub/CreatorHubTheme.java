package net.chriskatze.catocraftmod.creatorhub;

import net.minecraft.network.chat.Component;

/**
 * 🎨 Centralized color and style definitions for all Creator Hub screens.
 */
public final class CreatorHubTheme {

    private CreatorHubTheme() {}

    // ────────────────────────────────────────────────
    // Overlay & Panels
    // ────────────────────────────────────────────────
    public static final int OVERLAY_COLOR = 0xAA101520;      // dark blue-gray tint
    public static final int OVERLAY_COLOR_DARK = 0xCC0B0F12; // subtle deeper tint

    // These were missing — unify naming with other screens
    public static final int PANEL_BG = 0xFF1A1E24;       // background fill for boxes
    public static final int PANEL_BORDER = 0xFF5A6A7A;   // outline / frame tint
    public static final int PANEL_COLOR = PANEL_BG;      // alias for compatibility
    public static final int PANEL_OUTLINE = PANEL_BORDER;

    public static final int PANEL_DARKER = 0xFF14181E;
    public static final int PANEL_LIGHTER = 0xFF232933;

    // ────────────────────────────────────────────────
    // Text Colors
    // ────────────────────────────────────────────────
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB0B0B0;
    public static final int TEXT_WARNING = 0xFFFF6666;
    public static final int TEXT_MAIN = TEXT_PRIMARY; // alias for compatibility

    // ────────────────────────────────────────────────
    // Accent Colors
    // ────────────────────────────────────────────────
    public static final int ACCENT_BLUE = 0xFF3BA7FF;
    public static final int ACCENT_RED = 0xFFFF4444;
    public static final int COLOR_ACCENT_HOVER = 0x3340A0FF;

    // ────────────────────────────────────────────────
    // Titles
    // ────────────────────────────────────────────────
    public static final Component TITLE_CREATOR_HUB =
            Component.literal("Creator Hub").withStyle(style -> style.withColor(TEXT_PRIMARY));
    public static final Component TITLE_CONFIRM_DELETE =
            Component.literal("Confirm Delete").withStyle(style -> style.withColor(TEXT_PRIMARY));
}