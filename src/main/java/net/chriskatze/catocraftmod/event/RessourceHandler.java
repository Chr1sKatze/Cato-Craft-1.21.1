package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.render.ModRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class RessourceHandler {

    /**
     * Registers a reload listener to automatically clear cached textures when
     * the game's resource packs are reloaded.
     */
    @SubscribeEvent
    public static void registerReloadListener(FMLClientSetupEvent event) {
        // if (!FMLLoader.getDist().isClient()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener(new PreparableReloadListener() {
                @Override
                public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier barrier,
                                                               @NotNull ResourceManager resourceManager,
                                                               @NotNull ProfilerFiller prep,
                                                               @NotNull ProfilerFiller reload,
                                                               @NotNull Executor background,
                                                               @NotNull Executor game) {
                    return CompletableFuture.runAsync(ModRenderer::clearOverlayCache, game)
                            .thenCompose(barrier::wait);
                }
            });
        }
    }

}
