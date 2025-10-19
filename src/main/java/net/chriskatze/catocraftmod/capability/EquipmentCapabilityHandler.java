package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry & lifecycle manager for the unified
 * {@link PlayerEquipmentCapability}.
 *
 * Each player gets one persistent instance containing all
 * {@link net.chriskatze.catocraftmod.menu.layout.EquipmentGroup}s.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class EquipmentCapabilityHandler {

    /** Server-side cached instances of the capability per player UUID. */
    private static final Map<UUID, PlayerEquipmentCapability> INSTANCES = new ConcurrentHashMap<>();

    /** Capability key for unified equipment. */
    public static final EntityCapability<PlayerEquipmentCapability, Void> EQUIPMENT_CAP =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "equipment_cap"),
                    PlayerEquipmentCapability.class
            );

    // ────────────────────────────────────────────────
    // Registration
    // ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                EQUIPMENT_CAP,
                EntityType.PLAYER,
                (player, ctx) -> INSTANCES.computeIfAbsent(player.getUUID(), id -> {
                    var cap = new PlayerEquipmentCapability();
                    if (player instanceof ServerPlayer sp) {
                        cap.setOwner(sp);
                        cap.initializeGroupsIfMissing(); // initializes all EquipmentGroups
                    }
                    return cap;
                })
        );

        CatocraftMod.LOGGER.info("[EquipmentCap] Registered unified PlayerEquipmentCapability.");
    }

    // ────────────────────────────────────────────────
    // Lifecycle Events
    // ────────────────────────────────────────────────

    /** Clone capability data on respawn or dimension change. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        Player oldPlayer = event.getOriginal();

        var oldCap = oldPlayer.getCapability(EQUIPMENT_CAP);
        var newCap = newPlayer.getCapability(EQUIPMENT_CAP);

        if (oldCap != null && newCap != null) {
            newCap.deserializeNBT(newPlayer.registryAccess(), oldCap.serializeNBT(oldPlayer.registryAccess()));
            newCap.setOwner(newPlayer);
            newCap.reapplyAttributesOnLogin();
            CatocraftMod.LOGGER.debug("[EquipmentCap] Cloned equipment capability for {}", newPlayer.getName().getString());
        }
    }

    /** Clean up cache on logout to avoid leaks. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        INSTANCES.remove(player.getUUID());
        CatocraftMod.LOGGER.debug("[EquipmentCap] Removed cached capability for {}", player.getName().getString());
    }

    /** Ensure capability initialized and active after login. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EQUIPMENT_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EquipmentCap] Missing capability for {} on login!", player.getName().getString());
            return;
        }

        cap.setOwner(player);
        cap.initializeGroupsIfMissing();
        cap.reapplyAttributesOnLogin();
        cap.scheduleHealthNormalization(5);

        CatocraftMod.LOGGER.debug(
                "[EquipmentCap] Player {} initialized with {} equipment groups.",
                player.getName().getString(),
                cap.getAllGroups().size()
        );
    }

    // Convenience accessor
    public static PlayerEquipmentCapability get(Player player) {
        var cap = player.getCapability(EQUIPMENT_CAP);
        if (cap != null && player instanceof ServerPlayer sp) {
            cap.setOwner(sp);
            cap.initializeGroupsIfMissing();
        }
        return cap;
    }
}