package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record SlotGroupElement(
        @SerializedName("id") String id,
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("slots") int slots,
        @SerializedName("direction") String direction,
        @SerializedName("valid_items") List<String> validItems
) implements UIElement {

}