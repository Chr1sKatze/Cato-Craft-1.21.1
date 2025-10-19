package net.chriskatze.catocraftmod.manager;

import net.chriskatze.catocraftmod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

import static net.chriskatze.catocraftmod.util.ModMath.*;

/**
 * @author Mao
 * original from True-Darkness-Refabricated forked from grondag/darkness
 * https://github.com/HaXrDEV/True-Darkness-Refabricated
 */
public class Darkness {

    static double darkNetherFogEffective;
    static double darkEndFogEffective;

    private static final float[] BTW_MOON_BRIGHTNESS_BY_PHASE = new float[]{1.25F, 0.875F, 0.75F, 0.5F, 0F, 0.5F, 0.75F, 1.25F};

    private static void computeConfigValues() {
        darkNetherFogEffective = Config.darkNether ? Config.darkNetherFog : 1.0;
        darkEndFogEffective = Config.darkEnd ? Config.darkEndFog : 1.0;
    }

    public static double darkNetherFog() {
        computeConfigValues();
        return darkNetherFogEffective;
    }

    public static double darkEndFog() {
        computeConfigValues();
        return darkEndFogEffective;
    }

    private static boolean isDark(Level world) {
        final ResourceKey<Level> dimType = world.dimension();

        if (dimType == Level.OVERWORLD) {
            return Config.darkOverworld;
        } else if (dimType == Level.NETHER) {
            return Config.darkNether;
        } else if (dimType == Level.END) {
            return Config.darkEnd;
        } else if (world.dimensionType().hasSkyLight()) {
            return Config.darkDefault;
        } else {
            return Config.darkSkyless;
        }
    }

    private static float skyFactor(Level world) {
        if (!Config.blockLightOnly && isDark(world)) {
            if (world.dimensionType().hasSkyLight()) {
                final float angle = world.getTimeOfDay(0);

                if (angle > 0.25f && angle < 0.75f) {
                    final float oldWeight = Math.max(0, (Math.abs(angle - 0.5f) - 0.2f)) * 20;
                    final float moon = Config.ignoreMoonPhase ? 0 : world.getMoonBrightness();

                    // The case values DEFAULT, GRADUAL & BTW will show as not being defined. But I can assure you that they work just fine.
                    float moonBrightness = switch (Config.moonPhaseStyle) {
                        case DEFAULT -> moon * moon;
                        case GRADUAL -> moon;
                        case BTW -> BTW_MOON_BRIGHTNESS_BY_PHASE[world.getMoonPhase()];
                    };
                    return Mth.lerp(oldWeight * oldWeight * oldWeight, moonBrightness, 1.0f);

                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }

    public static boolean enabled = false;
    private static final float[][] LUMINANCE = new float[16][16];

    public static float getLum(int blockIndex, int skyIndex) {
        return Math.min(1, LUMINANCE[blockIndex][skyIndex] / luminance(1, 1, 1));
    }

    public static int darken(int c, int blockIndex, int skyIndex) {
        final float lTarget = LUMINANCE[blockIndex][skyIndex];
        final float r = (c & 0xFF) / 255f;
        final float g = ((c >> 8) & 0xFF) / 255f;
        final float b = ((c >> 16) & 0xFF) / 255f;
        final float l = luminance(r, g, b);
        final float f = l > 0 ? Math.min(1, lTarget / l) : 0;

        return f == 1f ? c
                : 0xFF000000 | Math.round(f * r * 255) | (Math.round(f * g * 255) << 8)
                | (Math.round(f * b * 255) << 16);
    }

    public static void updateLuminance(float tickDelta, Minecraft client, GameRenderer worldRenderer,
                                       float prevFlicker) {
        if (client.level == null) return;

        final ClientLevel world = client.level;
        prevFlicker = prevFlicker * 0.1F + 1.5F;

        if (!isDark(world) || client.player.hasEffect(MobEffects.NIGHT_VISION)
                || (client.player.hasEffect(MobEffects.CONDUIT_POWER) && client.player.getWaterVision() > 0)
                || world.getSkyFlashTime() > 0
        ) {
            enabled = false;
            return;
        } else {
            enabled = true;
        }

        final float dimSkyFactor = Darkness.skyFactor(world);
        final float ambient = world.getSkyDarken(1.0F);
        final DimensionType dim = world.dimensionType();
        final boolean blockAmbient = !Darkness.isDark(world);
        final float gamma = client.options.gamma().get().floatValue();

        for (int skyLightLevel = 0; skyLightLevel < 16; ++skyLightLevel) {
            float skyFactor = 1f - skyLightLevel / 15f;
            skyFactor = 1 - skyFactor * skyFactor * skyFactor * skyFactor;
            skyFactor *= dimSkyFactor;

            final float rawAmbient = ambient * skyFactor;
            final float minAmbient = addMul(rawAmbient, 0.05f * skyFactor, 1);
            final float skyBase = LightTexture.getBrightness(dim, skyLightLevel) * minAmbient;
            final float skyBase2 = skyBase * addMul(rawAmbient, 0.35f * skyFactor, 1);

            Vec3 skyColor = new Vec3(skyBase2,skyBase2,skyBase);

            if (worldRenderer.getDarkenWorldAmount(tickDelta) > 0.0F) {
                final float skyDarkness = worldRenderer.getDarkenWorldAmount(tickDelta);
                skyColor = mul(skyColor, 1.0F - skyDarkness).add(mul(new Vec3(0.7F, 0.6F, 0.6F), skyDarkness));
            }

            for (int blockLightLevel = 0; blockLightLevel < 16; ++blockLightLevel) {
                float blockFactor = 1f;

                if (!blockAmbient) {
                    blockFactor = 1f - blockLightLevel / 15f;
                    blockFactor = 1 - blockFactor * blockFactor * blockFactor * blockFactor;
                }

                final float f = Math.max(skyFactor, blockFactor);
                final float gammaF = gamma * f;
                final float min = 0.4f * blockFactor;

                final float blockBase = blockFactor * LightTexture.getBrightness(dim, blockLightLevel) * prevFlicker;
                final float blockGreen = blockBase * ((blockBase * (1 - min) + min) * (1 - min) + min);
                final float blockBlue = blockBase * (blockBase * blockBase * (1 - min) + min);
                final Vec3 blockBaseFull = new Vec3(blockBase,blockGreen,blockBlue);

                Vec3 color = addMul(skyColor.add(blockBaseFull), 0.03f * f, 0.99F);

                if (world.dimension() == Level.END) {
                    color = mul(new Vec3(0.22F, 0.28F, 0.25F), skyFactor).add(mul(blockBaseFull, 0.75f));
                }

                color = min(color, 1f);
                Vec3 invColor = new Vec3(1,1,1).subtract(pow(new Vec3(1,1,1).subtract(color),4));
                color = clamp(mul(color,1.0F - gammaF).add(mul(invColor, gammaF)), 0f, 1f);

                LUMINANCE[blockLightLevel][skyLightLevel] = luminance(color);
            }
        }
    }
}
