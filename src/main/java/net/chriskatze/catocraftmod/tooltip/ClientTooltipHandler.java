package net.chriskatze.catocraftmod.tooltip;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.HashMap;
import java.util.Map;

public class ClientTooltipHandler {

    /**
     * Info container for each custom book:
     * name, description, color, italic description
     */
    private record BookInfo(String name, String description, ChatFormatting color, boolean descriptionItalic) {}

    /** Map of enchantment IDs to their book display info */
    private static final Map<String, BookInfo> BOOKS = new HashMap<>();

    static {
        // Register custom books here
        BOOKS.put(CatocraftMod.MOD_ID + ":gathering_speed",
                new BookInfo("Gathering Speed Crystal",
                        "Increases Gathering Speed",
                        ChatFormatting.GREEN,
                        true));

        BOOKS.put(CatocraftMod.MOD_ID + ":platinum_power",
                new BookInfo("Platinum Power Tome",
                        "Grants enhanced mining power",
                        ChatFormatting.AQUA,
                        true));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof EnchantedBookItem)) return;

        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        enchantments.keySet().forEach(holder -> {
            String id = holder.unwrapKey()
                    .map(k -> k.location().toString())
                    .orElse("");

            if (!BOOKS.containsKey(id)) return;

            BookInfo info = BOOKS.get(id);
            int level = enchantments.getLevel(holder);

            // Name with level
            MutableComponent nameComponent = styledComponent(info.name() + " " + toRoman(level), info.color(), false);
            stack.set(DataComponents.CUSTOM_NAME, nameComponent);

            // Replace tooltip name line
            event.getToolTip().set(0, nameComponent);

            // Description line above enchantment effects
            MutableComponent descriptionComponent = styledComponent("  " + info.description(), info.color(), info.descriptionItalic());
            event.getToolTip().add(1, descriptionComponent);
        });
    }

    // ---------------- Helper Methods ----------------

    /** Converts numbers 1-10 to Roman numerals, fallback to Arabic */
    private static String toRoman(int number) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number > 0 && number <= romans.length) return romans[number - 1];
        return String.valueOf(number);
    }

    /**
     * Creates a MutableComponent with given text, color, and optional italic style
     */
    private static MutableComponent styledComponent(String text, ChatFormatting color, boolean italic) {
        MutableComponent comp = Component.literal(text).withStyle(color);
        if (italic) comp = comp.withStyle(ChatFormatting.ITALIC);
        return comp;
    }
}