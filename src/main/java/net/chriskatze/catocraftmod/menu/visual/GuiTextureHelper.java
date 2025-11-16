package net.chriskatze.catocraftmod.menu.visual;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for resolving logical GUI texture paths into valid {@link ResourceLocation}s
 * that Minecraft's texture manager can load safely.
 *
 * Used by both MenuEditorScreen and DynamicMenuScreen.
 *
 * Example:
 *  - "catocraftmod:gui/backgrounds/test_bg"
 *  → "catocraftmod:textures/gui/backgrounds/test_bg.png"
 */
public class GuiTextureHelper {

    /**
     * Converts a logical GUI texture path (like "catocraftmod:gui/...") to
     * a real texture resource ("textures/gui/....png").
     */
    @Nullable
    public static ResourceLocation toGuiTexture(@Nullable String logical) {
        if (logical == null || logical.isBlank()) return null;

        ResourceLocation rl = ResourceLocation.tryParse(logical);
        if (rl == null || rl.getPath().isBlank()) return null;

        String path = rl.getPath();
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png")) path += ".png";

        return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path);
    }
}