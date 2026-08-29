package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.RandomBlockPlacer;
import capi.rnd_block_placer.client.screen.widget.Texture;
import net.minecraft.resources.Identifier;

// Constants for the block selection screen layout, textures, colors, and sound
public class BlockSelectionScreenConstants {
    // Selection menu asset
    public static final Identifier SELECTION_MENU_TEXTURE = RandomBlockPlacer.id("textures/gui/selection_menu.png");
    public static final int IMAGE_W = 160;
    public static final int IMAGE_H = 77;
    public static final int DISPLAY_IMAGE_W = 320;
    public static final int DISPLAY_IMAGE_H = 154;

    // Inventory grid: 4 rows (3 main + 1 hotbar) x 9 columns
    public static final int INVENTORY_ROW = 4;
    public static final int INVENTORY_COL = 9;

    // Maps a grid position to the inventory slot index (row 3 = hotbar 0-8, rows 0-2 = main 9-35)
    public static int slotIndex(int row, int col) {
        return (row == 3) ? col : 9 + row * INVENTORY_COL + col;
    }

    // Trash Button Texture
    public static final Texture RESET_BUTTON_CLOSE = new Texture(
            RandomBlockPlacer.id("textures/gui/reset_button_close.png"),
            32,
            32
    );
    public static final Texture RESET_BUTTON_OPEN = new Texture(
            RandomBlockPlacer.id("textures/gui/reset_button_open.png"),
            32,
            32
    );

    // Save Button Texture
    public static final Texture SAVE_BUTTON = new Texture(
            RandomBlockPlacer.id("textures/gui/save_button.png"),
            32,
            32
    );

    // Slot size and positioning within the container
    public static final int SLOT_SIZE = 28;
    public static final int SLOT_X = 10;
    public static final int SLOT_PADDING_X = 6;
    public static final int SLOT_PADDING_Y = 6;
    public static final int MAIN_Y = 10;
    public static final int HOTBAR_Y = 116;

    // Selected blocks panel positioning (relative to the container)
    public static final int SELECTED_PANEL_X_OFFSET = 115;
    public static final int SELECTED_PANEL_Y_OFFSET = 20;
    public static final int SELECTED_PANEL_HEADER_Y_OFFSET = 14;
    public static final int SELECTED_PANEL_ROW_HEIGHT = 20;
    public static final int SELECTED_PANEL_BOTTOM_MARGIN = 10;
    public static final int SELECTED_PANEL_ITEM_X_OFFSET = 18;
    public static final int SELECTED_PANEL_TEXT_Y_OFFSET = 4;
    public static final int SELECTED_PANEL_MAX_WIDTH = 120;

    // Selected blocks panel text colors
    public static final int SELECTED_PANEL_HEADER_COLOR = 0xFFCCCCCC;
    public static final int SELECTED_PANEL_TEXT_COLOR = 0xFFFFFFFF;
    public static final int SELECTED_PANEL_MORE_COLOR = 0xFF888888;

    // Weight editor positioning (relative to the container)
    public static final int WEIGHT_INPUT_Y_OFFSET = 4;
    public static final int WEIGHT_LABEL_Y_OFFSET = 14;

    // Preset panel positioning (to the right of the container)
    public static final int PRESET_PANEL_X_OFFSET = 336;
    public static final int PRESET_PANEL_Y_OFFSET = 20;
    public static final int PRESET_HEADER_COLOR = 0xFFCCCCCC;
    public static final int PRESET_ACTIVE_COLOR = 0xFF00CC00;
    public static final int PRESET_TEXT_COLOR = 0xFFFFFFFF;
    public static final int PRESET_ACTIVE_Y_OFFSET = 16;
    public static final int PRESET_ROW_START_Y_OFFSET = 30;
    public static final int PRESET_ROW_HEIGHT = 18;
    public static final int PRESET_MAX_ROWS = 4;
    public static final int PRESET_NAME_MAX_WIDTH = 88;
    public static final int PRESET_BUTTON_WIDTH = 44;
    public static final int PRESET_BUTTON_HEIGHT = 12;
    public static final int PRESET_BUTTON_SPACING = 4;
    public static final int PRESET_CREATE_BUTTON_WIDTH = 44;
    public static final int PRESET_CREATE_BUTTON_HEIGHT = 14;
    public static final int PRESET_NAME_INPUT_WIDTH = 90;
    public static final int PRESET_NAME_INPUT_HEIGHT = 14;
    public static final int PRESET_NAME_MAX_LENGTH = 20;
    public static final int PRESET_BUTTON_ROW_SPACING = 8;
    public static final int PRESET_MESSAGE_COLOR = 0xFFFF5555;
    // Preset name input and buttons (centered below the container)
    public static final int PRESET_BOTTOM_Y_OFFSET = 14;
    public static final int PRESET_SAVE_BUTTON_WIDTH = 150;

    // Button click sound
    public static final float BUTTON_SOUND_VOLUME = 2.0f;
    public static final float BUTTON_SOUND_PITCH = 0.7f;

    // Weight label text color
    public static final int WEIGHT_LABEL_COLOR = 0xCCCCCC;

    // Selection highlight color (green)
    public static final int GREEN_SEL = 0xFF00CC00;
    // Overlay for non-block items (semi-transparent gray)
    public static final int GRAY_OVERLAY = 0x90808080;
    // Hover highlight color (semi-transparent white)
    public static final int HOVER = 0x50FFFFFF;
}
