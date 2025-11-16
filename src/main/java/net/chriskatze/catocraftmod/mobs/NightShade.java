package net.chriskatze.catocraftmod.mobs;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.function.Supplier;

public class NightShade extends Mob {

    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "nightshade");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "textures/entity/nightshade/nightshade.png");

    public NightShade(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        CatocraftMod.LOGGER.info("nightshade spawned");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 200) { // 200 ticks = 10 seconds
            this.discard(); // Removes entity without drops
            CatocraftMod.LOGGER.info("nightshade despawned");
        }
    }

    // server
    public static void spawnExample(Player player) {
        NightShade.spawn(player, player.blockPosition());
    }

    public static void spawn(Player player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.isClientSide()) return;
        player.sendSystemMessage(Component.literal("Trying to spawn NightShade..."));
        CatocraftMod.LOGGER.info("spawning nightshade...");
        EntityType<NightShade> type = ENTITY.get();
        NightShade entity = type.spawn(
                level,
                null, // Optional: NBT data for the entity
                null, // Optional: custom spawn position (uses default if null)
                pos, // spawn position
                MobSpawnType.COMMAND, // spawn type (e.g., NATURAL, SPAWNER, COMMAND)
                true, // should send feedback (e.g., particle effects)
                false // should respect chunk boundaries
        );
        if (entity != null) {
            player.sendSystemMessage(Component.literal("Spawned NightShade successfully!"));
            CatocraftMod.LOGGER.info("successfully spawned nightshade");
            level.addFreshEntity(entity);
        }
    }

    public static void registerSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(NightShade.ENTITY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.AND);
    }

    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        // NightShadeRenderer::new // fails
        event.registerEntityRenderer(NightShade.ENTITY.get(), NightShadeRenderer::new);
    }

    public static EntityType<NightShade> entityType() {
        return EntityType.Builder.of(NightShade::new, MobCategory.CREATURE)
                .sized(1.0f, 1.0f)
                .eyeHeight(1.0f) // Set eye height
                .updateInterval(5) // Update every 5 ticks
                .clientTrackingRange(8) // Track within 8 chunks
                .spawnDimensionsScale(1.0f) // Default scale
                .noSummon() // Prevent /summon usage
                .noSave() // Do not save to disk
                .fireImmune() // Immune to fire
                .immuneTo(Blocks.POWDER_SNOW) // Immune to powder snow
                .canSpawnFarFromPlayer() // Can spawn far from player
                .build(LOCATION.toString());
    }

    //public static EntityType<NightShade> ENTITY_TYPE = entityType();

    public static final Supplier<EntityType<NightShade>> ENTITY = ModEntities.ENTITY_TYPES.register("nightshade", NightShade::entityType);
}
