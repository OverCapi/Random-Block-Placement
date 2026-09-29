package capi.rnd_block_placer.client.screen;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Screen geometry computed from the window size: header, inventory panel, sidebar, footer and slot grid
public record SelectionLayout(
        ScreenRectangle header,
        ScreenRectangle inventoryPanel,
        ScreenRectangle sidebar,
        ScreenRectangle footer,
        int gridX,
        int gridY,
        int slotSize
) {
    // Fits the whole screen in the window, shrinking the slots and sidebar before anything overflows
    public static SelectionLayout compute(int screenWidth, int screenHeight) {
        int availableWidth = screenWidth - 2 * SCREEN_MARGIN;
        int availableHeight = screenHeight - 2 * SCREEN_MARGIN;
        int bodyHeight = availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT - 2 * SECTION_GAP;

        int slotByWidth = (availableWidth - SIDEBAR_MIN_WIDTH - SECTION_GAP - 2 * PANEL_PADDING
                - (INVENTORY_COL - 1) * SLOT_GAP) / INVENTORY_COL;
        int slotByHeight = (bodyHeight - 2 * PANEL_PADDING - HINT_HEIGHT
                - (INVENTORY_ROW - 1) * SLOT_GAP - HOTBAR_GAP) / INVENTORY_ROW;
        int slotSize = Mth.clamp(Math.min(slotByWidth, slotByHeight), SLOT_MIN_SIZE, SLOT_MAX_SIZE);

        int gridWidth = INVENTORY_COL * slotSize + (INVENTORY_COL - 1) * SLOT_GAP;
        int gridHeight = INVENTORY_ROW * slotSize + (INVENTORY_ROW - 1) * SLOT_GAP + HOTBAR_GAP;
        int inventoryWidth = gridWidth + 2 * PANEL_PADDING;
        int inventoryHeight = gridHeight + HINT_HEIGHT + 2 * PANEL_PADDING;

        int sidebarWidth = Mth.clamp(availableWidth - inventoryWidth - SECTION_GAP, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH);
        int panelHeight = Math.max(inventoryHeight, Math.min(bodyHeight, PANEL_PREFERRED_HEIGHT));

        int totalWidth = inventoryWidth + SECTION_GAP + sidebarWidth;
        int totalHeight = HEADER_HEIGHT + SECTION_GAP + panelHeight + SECTION_GAP + FOOTER_HEIGHT;
        int left = Math.max(0, (screenWidth - totalWidth) / 2);
        int top = Math.max(0, (screenHeight - totalHeight) / 2);
        int bodyY = top + HEADER_HEIGHT + SECTION_GAP;

        return new SelectionLayout(
                new ScreenRectangle(left, top, totalWidth, HEADER_HEIGHT),
                new ScreenRectangle(left, bodyY, inventoryWidth, panelHeight),
                new ScreenRectangle(left + inventoryWidth + SECTION_GAP, bodyY, sidebarWidth, panelHeight),
                new ScreenRectangle(left, bodyY + panelHeight + SECTION_GAP, totalWidth, FOOTER_HEIGHT),
                left + PANEL_PADDING,
                bodyY + PANEL_PADDING,
                slotSize
        );
    }

    // Left edge of the slot in the given grid column
    public int slotX(int col) {
        return gridX + col * (slotSize + SLOT_GAP);
    }

    // Top edge of the slot in the given grid row (row 3 is the hotbar, set apart by HOTBAR_GAP)
    public int slotY(int row) {
        int y = gridY + row * (slotSize + SLOT_GAP);
        return row == 3 ? y + HOTBAR_GAP : y;
    }

    // Returns the inventory slot index under the cursor, or -1 when not over a slot
    public int slotAt(double mx, double my) {
        for (int row = 0; row < INVENTORY_ROW; row++) {
            int y = slotY(row);
            if (my < y || my >= y + slotSize) {
                continue;
            }
            for (int col = 0; col < INVENTORY_COL; col++) {
                int x = slotX(col);
                if (mx >= x && mx < x + slotSize) {
                    return slotIndex(row, col);
                }
            }
        }
        return -1;
    }

    // Top of the help text area below the grid
    public int hintY() {
        return slotY(INVENTORY_ROW - 1) + slotSize + PANEL_PADDING;
    }

    // Sidebar area inside its padding
    public ScreenRectangle sidebarInner() {
        return new ScreenRectangle(
                sidebar.left() + PANEL_PADDING,
                sidebar.top() + PANEL_PADDING,
                sidebar.width() - 2 * PANEL_PADDING,
                sidebar.height() - 2 * PANEL_PADDING
        );
    }
}
