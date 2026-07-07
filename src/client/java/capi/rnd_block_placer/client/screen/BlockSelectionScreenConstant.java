package capi.rnd_block_placer.client.screen;

import net.minecraft.resources.Identifier;

// Constants for the block selection screen layout, textures, and colors
public class BlockSelectionScreenConstant {
    // Background texture: vanilla inventory container
    public static final Identifier CONTAINER_BG =
            Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    // Slot sprite
    public static final Identifier SLOT =
            Identifier.withDefaultNamespace("container/slot");

    // Container background dimensions
    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 166;

    // Inventory grid: 4 rows (3 main + 1 hotbar) x 9 columns
    public static final int INVENTORY_ROW = 4;
    public static final int INVENTORY_COL = 9;

    // Slot size and positioning within the container
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_X = 8;
    public static final int MAIN_Y = 84;
    public static final int HOTBAR_Y = 142;

    // Selection highlight color (green)
    public static final int GREEN_SEL = 0xFF00CC00;
    // Overlay for non-block items (semi-transparent gray)
    public static final int GRAY_OVERLAY = 0x90808080;
    // Hover highlight color (semi-transparent white)
    public static final int HOVER = 0x50FFFFFF;
}
