package net.chriskatze.catocraftmod.util;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/**
 * 🔹 Central registry for all CatoCraft custom attributes.
 * Works with NeoForge 21.1.209 and Minecraft 1.21.1.
 *
 * - DeferredHolders (public static final) are safe during registration.
 * - Cached Holders (runtime) are only valid after the server starts.
 */
public class ModAttributes {

    // ============================================================
    // 🔹 Utility Helpers
    // ============================================================
    public static ResourceLocation id(String key) {
        return ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, key);
    }

    public static AttributeModifier add(String key, double value) {
        return new AttributeModifier(id(key), value, AttributeModifier.Operation.ADD_VALUE);
    }

    public static AttributeModifier multiply(String key, double factor) {
        return new AttributeModifier(id(key), factor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ============================================================
    // 🔹 Deferred Registration (safe for early phase)
    // ============================================================
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, CatocraftMod.MOD_ID);

    // --- Vanilla shortcuts ---
    public static final Holder<Attribute> MAX_HEALTH = Attributes.MAX_HEALTH;
    public static final Holder<Attribute> MOVEMENT_SPEED = Attributes.MOVEMENT_SPEED;
    public static final Holder<Attribute> ATTACK_DAMAGE = Attributes.ATTACK_DAMAGE;
    public static final Holder<Attribute> ARMOR = Attributes.ARMOR;
    public static final Holder<Attribute> ARMOR_TOUGHNESS = Attributes.ARMOR_TOUGHNESS;

    // ============================================================
    // 🔹 Custom Combat Attributes (DeferredHolders)
    // ============================================================

    // --- Offensive Power ---
    public static final DeferredHolder<Attribute, Attribute> FIRE_POWER =
            ATTRIBUTES.register("fire_power", () ->
                    new RangedAttribute("attribute.name.catocraftmod.fire_power", 0.0, -10.0, 100.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> FROST_POWER =
            ATTRIBUTES.register("frost_power", () ->
                    new RangedAttribute("attribute.name.catocraftmod.frost_power", 0.0, -10.0, 100.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ARCANE_POWER =
            ATTRIBUTES.register("arcane_power", () ->
                    new RangedAttribute("attribute.name.catocraftmod.arcane_power", 0.0, -10.0, 100.0)
                            .setSyncable(true));

    // --- Resistances ---
    public static final DeferredHolder<Attribute, Attribute> FIRE_RESIST =
            ATTRIBUTES.register("fire_resist", () ->
                    new RangedAttribute("attribute.name.catocraftmod.fire_resist", 0.0, -1.0, 1.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> FROST_RESIST =
            ATTRIBUTES.register("frost_resist", () ->
                    new RangedAttribute("attribute.name.catocraftmod.frost_resist", 0.0, -1.0, 1.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ARCANE_RESIST =
            ATTRIBUTES.register("arcane_resist", () ->
                    new RangedAttribute("attribute.name.catocraftmod.arcane_resist", 0.0, -1.0, 1.0)
                            .setSyncable(true));

    // --- Utility ---
    public static final DeferredHolder<Attribute, Attribute> HEALING_POWER =
            ATTRIBUTES.register("healing_power", () ->
                    new RangedAttribute("attribute.name.catocraftmod.healing_power", 0.0, -10.0, 100.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MANA_REGEN =
            ATTRIBUTES.register("mana_regen", () ->
                    new RangedAttribute("attribute.name.catocraftmod.mana_regen", 0.0, 0.0, 100.0)
                            .setSyncable(true));

    // ============================================================
    // 🔹 Runtime Cached Holders (valid only after server starts)
    // ============================================================
    public static Holder<Attribute> FIRE_POWER_HOLDER;
    public static Holder<Attribute> FROST_POWER_HOLDER;
    public static Holder<Attribute> ARCANE_POWER_HOLDER;

    public static Holder<Attribute> FIRE_RESIST_HOLDER;
    public static Holder<Attribute> FROST_RESIST_HOLDER;
    public static Holder<Attribute> ARCANE_RESIST_HOLDER;

    public static Holder<Attribute> HEALING_POWER_HOLDER;
    public static Holder<Attribute> MANA_REGEN_HOLDER;

    /**
     * Called once on server start to populate runtime holders.
     * Safe to call only when registries are fully loaded.
     */
    public static void cacheHolders(MinecraftServer server) {
        HolderLookup.RegistryLookup<Attribute> lookup =
                server.registryAccess().lookupOrThrow(Registries.ATTRIBUTE);

        Function<DeferredHolder<Attribute, Attribute>, Holder<Attribute>> safeGet = def -> {
            try {
                return lookup.get(def.getKey()).orElseThrow(() ->
                        new IllegalStateException("Missing attribute: " + def.getId()));
            } catch (Exception e) {
                CatocraftMod.LOGGER.warn("[ModAttributes] ⚠ Failed to resolve attribute {}: {}", def.getId(), e.toString());
                return null;
            }
        };

        FIRE_POWER_HOLDER   = safeGet.apply(FIRE_POWER);
        FROST_POWER_HOLDER  = safeGet.apply(FROST_POWER);
        ARCANE_POWER_HOLDER = safeGet.apply(ARCANE_POWER);

        FIRE_RESIST_HOLDER  = safeGet.apply(FIRE_RESIST);
        FROST_RESIST_HOLDER = safeGet.apply(FROST_RESIST);
        ARCANE_RESIST_HOLDER= safeGet.apply(ARCANE_RESIST);

        HEALING_POWER_HOLDER= safeGet.apply(HEALING_POWER);
        MANA_REGEN_HOLDER   = safeGet.apply(MANA_REGEN);

        CatocraftMod.LOGGER.info(
                "[ModAttributes] Cached runtime holders: fire={}, frost={}, arcane={}, healing={}, mana={}",
                FIRE_POWER_HOLDER != null, FROST_POWER_HOLDER != null, ARCANE_POWER_HOLDER != null,
                HEALING_POWER_HOLDER != null, MANA_REGEN_HOLDER != null
        );
    }
}