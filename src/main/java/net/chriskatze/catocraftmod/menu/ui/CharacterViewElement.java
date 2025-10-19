package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;

public record CharacterViewElement(
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("scale") int scale
) implements UIElement {

}