package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;

public record LabelElement(
        @SerializedName("text") String text,
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("color") String color,
        @SerializedName("shadow") boolean shadow
) implements UIElement {

}