package capi.rnd_block_placer.client.screen;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Screen geometry computed from the window size.
// Split: [Selection | Inventory | Presets]. Narrow windows fall back to [Inventory | tabbed panel].
public record SelectionLayout(
        ScreenRectangle header,
        ScreenRectangle inventoryPanel,
        // Selection panel; empty when not split
        ScreenRectangle leftPanel,
        // Presets panel when split, tabbed Selection/Presets panel otherwise
        ScreenRectangle rightPanel,
        ScreenRectangle footer,
        boolean split,
        int gridX,
        int gridY,
        int slotSize
) {
    // Fits the whole screen in the window, shrinking the slots and side panels before anything overflows
    public static SelectionLayout compute(int screenWidth, int screenHeight) {
        int availableWidth = screenWidth - 2 * SCREEN_MARGIN;
        int availableHeight = screenHeight - 2 * SCREEN_MARGIN;
        int bodyHeight = availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT - 2 * SECTION_GAP;

        int slotByHeight = (bodyHeight - 2 * PANEL_PADDING - HINT_HEIGHT
                - (INVENTORY_ROW - 1) * SLOT_GAP - HOTBAR_GAP) / INVENTORY_ROW;
        int splitSlotByWidth = slotSizeForWidth(availableWidth - 2 * (SIDE_PANEL_MIN_WIDTH + SECTION_GAP));
        boolean split = splitSlotByWidth >= SLOT_MIN_SIZE;
        int sidePanels = split ? 2 : 1;
        int slotByWidth = split ? splitSlotByWidth : slotSizeForWidth(availableWidth - SIDE_PANEL_MIN_WIDTH - SECTION_GAP);
        int slotSize = Mth.clamp(Math.min(slotByWidth, slotByHeight), SLOT_MIN_SIZE, SLOT_MAX_SIZE);

        int gridWidth = INVENTORY_COL * slotSize + (INVENTORY_COL - 1) * SLOT_GAP;
        int gridHeight = INVENTORY_ROW * slotSize + (INVENTORY_ROW - 1) * SLOT_GAP + HOTBAR_GAP;
        int inventoryWidth = gridWidth + 2 * PANEL_PADDING;
        int inventoryHeight = gridHeight + HINT_HEIGHT + 2 * PANEL_PADDING;

        int sideWidth = Mth.clamp((availableWidth - inventoryWidth - sidePanels * SECTION_GAP) / sidePanels,
                SIDE_PANEL_MIN_WIDTH, SIDE_PANEL_MAX_WIDTH);
        int panelHeight = Math.max(inventoryHeight, Math.min(bodyHeight, PANEL_PREFERRED_HEIGHT));

        int totalWidth = inventoryWidth + sidePanels * (SECTION_GAP + sideWidth);
        int totalHeight = HEADER_HEIGHT + SECTION_GAP + panelHeight + SECTION_GAP + FOOTER_HEIGHT;
        int left = Math.max(0, (screenWidth - totalWidth) / 2);
        int top = Math.max(0, (screenHeight - totalHeight) / 2);
        int bodyY = top + HEADER_HEIGHT + SECTION_GAP;

        int inventoryX = split ? left + sideWidth + SECTION_GAP : left;
        ScreenRectangle leftPanel = split
                ? new ScreenRectangle(left, bodyY, sideWidth, panelHeight)
                : ScreenRectangle.empty();

        return new SelectionLayout(
                new ScreenRectangle(left, top, totalWidth, HEADER_HEIGHT),
                new ScreenRectangle(inventoryX, bodyY, inventoryWidth, panelHeight),
                leftPanel,
                new ScreenRectangle(inventoryX + inventoryWidth + SECTION_GAP, bodyY, sideWidth, panelHeight),
                new ScreenRectangle(left, bodyY + panelHeight + SECTION_GAP, totalWidth, FOOTER_HEIGHT),
                split,
                inventoryX + PANEL_PADDING,
                bodyY + PANEL_PADDING,
                slotSize
        );
    }

    // Largest slot size whose inventory panel fits in the given width
    private static int slotSizeForWidth(int inventoryPanelWidth) {
        return (inventoryPanelWidth - 2 * PANEL_PADDING - (INVENTORY_COL - 1) * SLOT_GAP) / INVENTORY_COL;
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

    // The given panel's area inside its padding
    public static ScreenRectangle inner(ScreenRectangle panel) {
        return new ScreenRectangle(
                panel.left() + PANEL_PADDING,
                panel.top() + PANEL_PADDING,
                panel.width() - 2 * PANEL_PADDING,
                panel.height() - 2 * PANEL_PADDING
        );
    }
}
