package net.chriskatze.catocraftmod.interfaces;

import org.jetbrains.annotations.Range;

public interface EntityLightSource {
    /**
     * {@return the luminance of the light source}
     * The maximum is 15, values below 1 are ignored.
     */
    @Range(from = 0, to = 15)
    int getLuminance();
}
