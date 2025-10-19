package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Marker interface for all UI schema elements.
 */
public sealed interface UIElement permits LabelElement, SlotGroupElement, CharacterViewElement {

}