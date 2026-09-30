package capi.rnd_block_placer.client.screen.tab;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// A sidebar tab: owns its widgets and a scrollable list of fixed-height rows
public abstract class SidebarTab {
    protected final Font font;

    private ScreenRectangle listArea = ScreenRectangle.empty();
    private int scrollOffset;

    protected SidebarTab(Font font) {
        this.font = font;
    }

    // Label of the tab button
    public abstract Component title();

    // Creates this tab's widgets inside the given area and registers them with the screen
    public abstract void init(ScreenRectangle area, Consumer<AbstractWidget> addWidget);

    // Refreshes widget labels and enabled state; called every frame
    public abstract void updateWidgets();

    // Draws the list and any custom content
    public abstract void render(GuiGraphics extract, int mx, int my);

    // Handles a click on the list; returns true if consumed
    public abstract boolean mouseClicked(double mx, double my);

    // Handles a key press; returns true if consumed
    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    // Number of rows in the list
    protected abstract int rowCount();

    // Scrolls the list by one row per wheel notch when the cursor is over it
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!listArea.containsPoint((int) mx, (int) my) || maxScroll() == 0 || amount == 0) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    protected void setListArea(ScreenRectangle area) {
        listArea = area;
        clampScroll();
    }

    protected ScreenRectangle listArea() {
        return listArea;
    }

    protected int visibleRows() {
        return Math.max(0, listArea.height() / SIDEBAR_ROW_HEIGHT);
    }

    protected int maxScroll() {
        return Math.max(0, rowCount() - visibleRows());
    }

    protected void clampScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    protected void resetScroll() {
        scrollOffset = 0;
    }

    // Scrolls just enough for the given row to be visible
    protected void ensureVisible(int index) {
        if (index < scrollOffset) {
            scrollOffset = index;
        } else if (index >= scrollOffset + visibleRows()) {
            scrollOffset = index - visibleRows() + 1;
        }
        clampScroll();
    }

    protected int firstVisibleRow() {
        return scrollOffset;
    }

    // Index one past the last visible row
    protected int endVisibleRow() {
        return Math.min(rowCount(), scrollOffset + visibleRows());
    }

    protected int rowY(int index) {
        return listArea.top() + (index - scrollOffset) * SIDEBAR_ROW_HEIGHT;
    }

    // Row width, leaving room for the scrollbar when the list overflows
    protected int rowWidth() {
        return listArea.width() - (maxScroll() > 0 ? SCROLLBAR_WIDTH + 2 : 0);
    }

    // Returns the row index under the cursor, or -1
    protected int rowAt(double mx, double my) {
        if (mx < listArea.left() || mx >= listArea.left() + rowWidth() || my < listArea.top()) {
            return -1;
        }
        int index = scrollOffset + (int) ((my - listArea.top()) / SIDEBAR_ROW_HEIGHT);
        return index < endVisibleRow() ? index : -1;
    }

    // Draws a thin scrollbar on the right edge of the list when it overflows
    protected void renderScrollbar(GuiGraphics extract) {
        if (maxScroll() == 0) {
            return;
        }
        int trackHeight = visibleRows() * SIDEBAR_ROW_HEIGHT;
        int thumbHeight = Math.max(8, trackHeight * visibleRows() / rowCount());
        int thumbY = listArea.top() + (trackHeight - thumbHeight) * scrollOffset / maxScroll();
        extract.fill(listArea.right() - SCROLLBAR_WIDTH, thumbY, listArea.right(), thumbY + thumbHeight, SCROLLBAR_COLOR);
    }

    // Shortens a string to the given pixel width, appending an ellipsis
    protected String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }
}
