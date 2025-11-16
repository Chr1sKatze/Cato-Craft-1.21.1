package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.render.ModRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import static net.minecraft.world.item.Items.TORCH;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class ItemChangeHandler {

    public static boolean HAS_TORCH_IN_HAND = false;

    @SubscribeEvent
    public static void onHandItemChange(LivingEquipmentChangeEvent event) {
        // Check if the event is for a player and the slot is a hand slot
        if (event.getEntity() instanceof Player player) {
            EquipmentSlot slot = event.getSlot();
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack oldItem = event.getFrom();
                ItemStack newItem = event.getTo();
                HAS_TORCH_IN_HAND = newItem.is(TORCH);
                if (HAS_TORCH_IN_HAND || oldItem.is(TORCH)) {
                    if (HAS_TORCH_IN_HAND) ModRenderer.updateRender(true, 10, false);
                    else ModRenderer.resetLightLevels();
                }
            }
        }
    }
}
