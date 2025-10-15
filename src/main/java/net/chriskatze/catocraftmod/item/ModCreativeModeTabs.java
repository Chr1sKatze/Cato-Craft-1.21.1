package net.chriskatze.catocraftmod.item;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CatocraftMod.MOD_ID);

    public static final Supplier<CreativeModeTab> CATO_CRAFT_ITEMS_TAB = CREATIVE_MODE_TAB.register("cato_craft_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.STEEL_INGOT.get()))
                            .title(Component.translatable("creativetab.catocraftmod.items"))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.STEEL_INGOT);
                                output.accept(ModItems.STEEL_NUGGET);
                                output.accept(ModItems.RAW_STEEL);
                                output.accept(ModItems.PLATINUM_INGOT);
                                output.accept(ModItems.PLATINUM_NUGGET);
                                output.accept(ModItems.RAW_PLATINUM);
                                output.accept(ModItems.GATHERING_CRYSTAL);
                                output.accept(ModItems.REINFORCEMENT_CRYSTAL);
                                output.accept(ModItems.PROSPERITY_CRYSTAL);
                                output.accept(ModItems.ATTRACTION_CRYSTAL);
                                output.accept(ModItems.REVELATION_CRYSTAL);
                                output.accept(ModItems.PRESERVATION_CRYSTAL);
                            }).build());

    public static final Supplier<CreativeModeTab> CATO_CRAFT_BLOCKS_TAB = CREATIVE_MODE_TAB.register("cato_craft_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.STEEL_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "cato_craft_items_tab"))
                            .title(Component.translatable("creativetab.catocraftmod.blocks"))
                            .displayItems((parameters, output) -> {
                                output.accept(ModBlocks.STEEL_BLOCK);
                                output.accept(ModBlocks.PLATINUM_BLOCK);
                                output.accept(ModBlocks.PLATINUM_ORE);
                                output.accept(ModBlocks.PLATINUM_DEEPSLATE_ORE);
                            }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}