package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.screen.widget.CustomButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Handles all custom rendering for the block selection screen
public class BlockSelectionScreenRenderer {
    // Enlarged inventory item icon size (rendered from the 16px baked sprite)
    private static final int INVENTORY_ICON_SIZE = 24;
    private static final float INVENTORY_ICON_SCALE = (float) INVENTORY_ICON_SIZE / 16.0f;

    private int leftPos;
    private int topPos;

    private Font font;

    private Minecraft mc;

    // Scroll offset for the selected blocks list (left panel)
    private int selectedListScrollOffset = 0;

    // Initializes positioning and rendering references
    public void init(int leftPos, int topPos, Font font, Minecraft mc) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.font = font;
        this.mc = mc;
        selectedListScrollOffset = 0;
    }

    // Scrolls the selected blocks list when the mouse wheel is used over the left panel
    public boolean mouseScrolled(double mx, double my, double verticalAmount, BlockSelectionScreenState state) {
        if (!isOverSelectedListArea(mx, my) || maxSelectedListScrollOffset(state) == 0) {
            return false;
        }
        selectedListScrollOffset = Math.max(0, Math.min(maxSelectedListScrollOffset(state), selectedListScrollOffset - (int) verticalAmount));
        return true;
    }

    // Draws the vanilla inventory container background
    private void drawBackground(GuiGraphicsExtractor extract) {
        extract.blit(
                RenderPipelines.GUI_TEXTURED,
                SELECTION_MENU_TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                DISPLAY_IMAGE_W,
                DISPLAY_IMAGE_H,
                IMAGE_W,
                IMAGE_H,
                256,
                256
        );
    }

    // Draws the selected blocks panel on the left side with their weight percentages
    private void drawSelectedList(GuiGraphicsExtractor extract, BlockSelectionScreenState state) {
        if (state.isEmpty()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        int panelX = leftPos - SELECTED_PANEL_X_OFFSET;
        int panelY = topPos + SELECTED_PANEL_Y_OFFSET;

        extract.text(font, Component.translatable("label.rnd-block-placer.selected"), panelX, panelY - SELECTED_PANEL_HEADER_Y_OFFSET, SELECTED_PANEL_HEADER_COLOR);

        // Calculate total weight for percentage display
        int totalWeight = state.totalWeight();

        List<Map.Entry<Identifier, Integer>> workingWeightSorted = state.weightsSortedByDesc();

        int maxRows = maxSelectedRows();
        int start = Math.min(selectedListScrollOffset, maxSelectedListScrollOffset(state));
        int yOff = panelY;
        for (int i = start; i < workingWeightSorted.size() && i < start + maxRows; i++) {
            Map.Entry<Identifier, Integer> entry = workingWeightSorted.get(i);
            Identifier id = entry.getKey();
            int weight = entry.getValue();
            ItemStack st = new ItemStack(BuiltInRegistries.ITEM.getValue(id), 1);

            int realWeight = weight * 100 / totalWeight;
            Component component = Component.translatable("label.rnd-block-placer.weight_summary", realWeight, weight);

            // Render item icon and percentage
            int x = Math.max(panelX, 0);
            extract.item(st, x, yOff);
            extract.text(font, component, x + SELECTED_PANEL_ITEM_X_OFFSET, yOff + SELECTED_PANEL_TEXT_Y_OFFSET, SELECTED_PANEL_TEXT_COLOR);
            yOff += SELECTED_PANEL_ROW_HEIGHT;
        }

        // Hint that the list can be scrolled when it exceeds the visible area
        if (workingWeightSorted.size() > maxRows) {
            int remaining = workingWeightSorted.size() - maxRows;
            extract.text(font, Component.translatable("label.rnd-block-placer.more_scroll", remaining),
                    panelX, yOff, SELECTED_PANEL_MORE_COLOR);
        }
    }

    // Maximum number of selected list rows that fit in the panel
    private int maxSelectedRows() {
        int maxY = topPos + DISPLAY_IMAGE_H - SELECTED_PANEL_BOTTOM_MARGIN;
        int panelY = topPos + SELECTED_PANEL_Y_OFFSET;
        return (maxY - panelY) / SELECTED_PANEL_ROW_HEIGHT;
    }

    // Maximum scroll offset for the selected list (0 when it fits entirely)
    private int maxSelectedListScrollOffset(BlockSelectionScreenState state) {
        return Math.max(0, state.weightsSortedByDesc().size() - maxSelectedRows());
    }

    // Whether the cursor is over the selected blocks list area
    private boolean isOverSelectedListArea(double mx, double my) {
        int panelX = Math.max(leftPos - SELECTED_PANEL_X_OFFSET, 0);
        int panelY = topPos + SELECTED_PANEL_Y_OFFSET;
        return mx >= panelX
                && mx < panelX + SELECTED_PANEL_MAX_WIDTH
                && my >= panelY
                && my < panelY + maxSelectedRows() * SELECTED_PANEL_ROW_HEIGHT;
    }

    // Draws the enlarged item icon centered on its slot, via pose scaling about the slot center
    private void drawInventoryIcon(GuiGraphicsExtractor extract, ItemStack stack, int slotX, int slotY) {
        float cx = slotX + SLOT_SIZE / 2.0f;
        float cy = slotY + SLOT_SIZE / 2.0f;
        Matrix3x2fStack pose = extract.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(INVENTORY_ICON_SCALE);
        pose.translate(-cx, -cy);
        extract.item(stack, slotX + (SLOT_SIZE - 16) / 2, slotY + (SLOT_SIZE - 16) / 2);
        pose.popMatrix();
    }

    // Renders all inventory slots with selection highlights, non-block overlays, and hover effects
    private void drawInventorySlots(GuiGraphicsExtractor extract, BlockSelectionScreenState state, int mx, int my) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        for (int row = 0; row < INVENTORY_ROW; row++) {

            for (int col = 0; col < INVENTORY_COL; col++) {
                int slotIndex = slotIndex(row, col);
                ItemStack itemStack = player.getInventory().getItem(slotIndex);

                boolean isBlock = itemStack.getItem() instanceof BlockItem;
                Identifier blockId = isBlock ? BuiltInRegistries.ITEM.getKey(itemStack.getItem()) : null;

                boolean selected = blockId != null && state.containsWeight(blockId);

                int x = leftPos + SLOT_X + col * (SLOT_SIZE + SLOT_PADDING_X);
                int yOff = (row == 3) ? HOTBAR_Y : MAIN_Y + row * (SLOT_SIZE + SLOT_PADDING_Y);
                int y = topPos + yOff;

                // Draw slot background: green if selected, default sprite otherwise
                if (selected) {
                    extract.fill(x, y, x + 28, y + 28, GREEN_SEL);
                }

                if (!itemStack.isEmpty()) {
                    drawInventoryIcon(extract, itemStack, x, y);
                    // Show weight number overlay on selected blocks
                    if (selected) {
                        String weightStr = "§l" + state.getWeight(blockId);
                        int tw = font.width(weightStr);
                        extract.text(font, weightStr, x + SLOT_SIZE - tw - 2, y + 2, 0xFFFFFF);
                    }
                }

                // Gray overlay for non-block items (cannot be selected)
                if (!itemStack.isEmpty() && !isBlock) {
                    extract.fill(x, y, x + 28, y + 28, GRAY_OVERLAY);
                }

                // White hover highlight
                boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
                if (hovered) {
                    extract.fill(x, y, x + 28, y + 28, HOVER);
                    if (!itemStack.isEmpty()) {
                        drawHoverName(extract, itemStack, x, y);
                    }
                }
            }
        }
    }

    // Draws the item name above the hovered slot, without a background
    private void drawHoverName(GuiGraphicsExtractor extract, ItemStack itemStack, int slotX, int slotY) {
        String name = itemStack.getHoverName().getString();
        int nameWidth = font.width(name);
        int textX = Math.max(0, Math.min(slotX + (SLOT_SIZE - nameWidth) / 2,
                mc.getWindow().getGuiScaledWidth() - nameWidth));
        int textY = slotY - font.lineHeight - 1;
        if (textY < 0) {
            textY = slotY + SLOT_SIZE + 1;
        }
        extract.text(font, name, textX, textY, 0xFFFFFFFF);
    }

    // Main render method: draws background, buttons, selected list, and inventory slots
    public void render(GuiGraphicsExtractor extract, BlockSelectionScreenState state, CustomButton saveButton, CustomButton resetButton, int mx, int my) {
        drawBackground(extract);
        saveButton.render(extract);
        resetButton.render(extract);
        drawSelectedList(extract, state);
        drawInventorySlots(extract, state, mx, my);
    }
}
