package net.chriskatze.catocraftmod.util;

import net.minecraft.world.phys.Vec3;

public class ModMath {

    public static float luminance(float r, float g, float b) {
        return r * 0.2126f + g * 0.7152f + b * 0.0722f;
    }

    public static float luminance(Vec3 color) {
        return (float) (color.x * 0.2126f + color.y * 0.7152f + color.z * 0.0722f);
    }

    public static Vec3 add(Vec3 color, float value) {
        return color.add(value,value,value);
    }

    public static Vec3 mul(Vec3 color, float value) {
        return color.multiply(value,value,value);
    }

    public static Vec3 addMul(Vec3 color, float value, float subtractFromValue) {
        return add(mul(color, subtractFromValue - value), value);
    }

    public static float addMul(float base, float value, float subtractFromValue) {
        return base * (subtractFromValue - value) + value;
    }

    public static Vec3 pow(Vec3 color, float value) {
        return new Vec3(Math.pow(color.x,value),Math.pow(color.y,value),Math.pow(color.z,value));
    }

    public static Vec3 min(Vec3 color, float min) {
        return new Vec3(
                Math.min(color.x, min),
                Math.min(color.y, min),
                Math.min(color.z, min)
        );
    }

    public static Vec3 clamp(Vec3 color, float min, float max) {
        return new Vec3(
                Math.clamp(color.x, min, max),
                Math.clamp(color.y, min, max),
                Math.clamp(color.z, min, max)
        );
    }

}
