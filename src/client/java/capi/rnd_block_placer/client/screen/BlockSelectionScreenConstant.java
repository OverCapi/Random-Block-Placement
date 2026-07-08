package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.RandomBlockPlacer;
import net.minecraft.resources.Identifier;

// Constants for the block selection screen layout, textures, and colors
public class BlockSelectionScreenConstant {
    // Selection menu asset
    public static final Identifier SELECTION_MENU_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    RandomBlockPlacer.MOD_ID,
                    "textures/gui/selection_menu.png"
            );
    public static final int IMAGE_W = 147;
    public static final int IMAGE_H = 75;
    public static final int DISPLAY_IMAGE_W = 220;
    public static final int DISPLAY_IMAGE_H = 112;

    // Slot sprite
    public static final Identifier SLOT =
            Identifier.withDefaultNamespace("container/slot");

    // Inventory grid: 4 rows (3 main + 1 hotbar) x 9 columns
    public static final int INVENTORY_ROW = 4;
    public static final int INVENTORY_COL = 9;

    // Slot size and positioning within the container
    public static final int SLOT_SIZE = 14;
    public static final int SLOT_X = 10;
    public static final int SLOT_PADDING_X = 7;
    public static final int SLOT_PADDING_Y = 12;
    public static final int MAIN_Y = 12;
    public static final int HOTBAR_Y = 88;

    // Selection highlight color (green)
    public static final int GREEN_SEL = 0xFF00CC00;
    // Overlay for non-block items (semi-transparent gray)
    public static final int GRAY_OVERLAY = 0x90808080;
    // Hover highlight color (semi-transparent white)
    public static final int HOVER = 0x50FFFFFF;
}
