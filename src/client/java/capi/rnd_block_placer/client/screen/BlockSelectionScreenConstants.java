package capi.rnd_block_placer.client.screen;

// Constants for the block selection screen layout and colors
public class BlockSelectionScreenConstants {
    // Inventory grid: 4 rows (3 main + 1 hotbar) x 9 columns
    public static final int INVENTORY_ROW = 4;
    public static final int INVENTORY_COL = 9;

    // Maps a grid position to the inventory slot index (row 3 = hotbar 0-8, rows 0-2 = main 9-35)
    public static int slotIndex(int row, int col) {
        return (row == 3) ? col : 9 + row * INVENTORY_COL + col;
    }

    // Outer layout: margin to the window edge, gap between sections, padding inside panels
    public static final int SCREEN_MARGIN = 4;
    public static final int SECTION_GAP = 6;
    public static final int PANEL_PADDING = 6;
    public static final int HEADER_HEIGHT = 20;
    public static final int FOOTER_HEIGHT = 20;

    // Inventory slots scale between these sizes to fit the window
    public static final int SLOT_MIN_SIZE = 18;
    public static final int SLOT_MAX_SIZE = 28;
    public static final int SLOT_GAP = 2;
    // Extra space separating the hotbar from the main inventory
    public static final int HOTBAR_GAP = 4;
    // Height reserved below the grid for the two help lines
    public static final int HINT_HEIGHT = 22;

    // Side panel width bounds and the height the panels grow to when space allows
    public static final int SIDE_PANEL_MIN_WIDTH = 110;
    public static final int SIDE_PANEL_MAX_WIDTH = 160;
    public static final int PANEL_PREFERRED_HEIGHT = 190;

    // Sidebar contents
    public static final int SIDEBAR_BUTTON_HEIGHT = 16;
    public static final int SIDEBAR_ROW_HEIGHT = 18;
    public static final int SIDEBAR_SPACING = 4;
    public static final int SCROLLBAR_WIDTH = 2;
    // Height of a side panel title line, when panels are shown side by side
    public static final int PANEL_TITLE_HEIGHT = 12;

    // Footer buttons
    public static final int FOOTER_BUTTON_WIDTH = 90;
    public static final int HEADER_TOGGLE_WIDTH = 110;

    // Quick preset wheel: a ring of slices around a center disc; the pointed slice pops outward
    public static final int WHEEL_MAX_RADIUS = 90;
    public static final float WHEEL_INNER_RATIO = 0.42f;
    public static final int WHEEL_GAP = 3;
    public static final int WHEEL_HOVER_GROW = 6;
    public static final int WHEEL_DEAD_ZONE = 16;
    public static final int WHEEL_HOVER_COLOR = 0xE01E4D1E;
    public static final int WHEEL_ACTIVE_COLOR = 0xC0183018;

    // Panel colors
    public static final int PANEL_COLOR = 0xC0101010;
    public static final int PANEL_BORDER_COLOR = 0xFF555555;

    // Slot colors
    public static final int SLOT_COLOR = 0xFF2B2B2B;
    public static final int SLOT_BORDER_COLOR = 0xFF4A4A4A;
    public static final int SLOT_SELECTED_COLOR = 0xFF1E4D1E;
    public static final int SLOT_SELECTED_BORDER_COLOR = 0xFF55FF55;
    public static final int SLOT_DISABLED_OVERLAY = 0xA0202020;
    public static final int SLOT_HOVER_OVERLAY = 0x50FFFFFF;

    // List row colors
    public static final int ROW_HOVER_COLOR = 0x30FFFFFF;
    public static final int ROW_SELECTED_COLOR = 0x5055FF55;
    public static final int SCROLLBAR_COLOR = 0xFF888888;

    // Text colors
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int TEXT_MUTED_COLOR = 0xFFA0A0A0;
    public static final int TEXT_ACTIVE_COLOR = 0xFF55FF55;
    public static final int TEXT_WARNING_COLOR = 0xFFFFD24A;
    public static final int TEXT_REMOVE_COLOR = 0xFFFF5555;
}
