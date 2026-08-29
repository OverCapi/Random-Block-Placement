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

import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Handles all custom rendering for the block selection screen
public class BlockSelectionScreenRenderer {
    private int leftPos;
    private int topPos;

    private Font font;

    private Minecraft mc;

    // Initializes positioning and rendering references
    public void init(int leftPos, int topPos, Font font, Minecraft mc) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.font = font;
        this.mc = mc;
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

        int maxY = topPos + DISPLAY_IMAGE_H - SELECTED_PANEL_BOTTOM_MARGIN;
        int yOff = panelY;
        int shown = 0;
        int maxRows = (maxY - panelY) / SELECTED_PANEL_ROW_HEIGHT;

        for (Map.Entry<Identifier, Integer> entry : workingWeightSorted) {
            Identifier id = entry.getKey();
            int weight = entry.getValue();
            ItemStack st = new ItemStack(BuiltInRegistries.ITEM.getValue(id), 1);

            shown++;
            // Show "+ N more" line if list exceeds available space
            if (shown > maxRows) {
                int rem = workingWeightSorted.size() - shown + 1;
                extract.text(font, Component.translatable("label.rnd-block-placer.more", rem), panelX, yOff, SELECTED_PANEL_MORE_COLOR);
                break;
            }

            int realWeight = weight * 100 / totalWeight;
            Component component = Component.translatable("label.rnd-block-placer.weight_summary", realWeight, weight);

            // Render item icon and percentage
            int x = Math.max(panelX, 0);
            extract.item(st, x, yOff);
            extract.text(font, component, x + SELECTED_PANEL_ITEM_X_OFFSET, yOff + SELECTED_PANEL_TEXT_Y_OFFSET, SELECTED_PANEL_TEXT_COLOR);
            yOff += SELECTED_PANEL_ROW_HEIGHT;
        }
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
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, GREEN_SEL);
                }

                if (!itemStack.isEmpty()) {
                    extract.item(itemStack, x - 1, y - 1);
                    // Show weight number overlay on selected blocks
                    if (selected) {
                        String weightStr = "§l" + state.getWeight(blockId);
                        int tw = font.width(weightStr);
                        extract.text(font, weightStr, x + SLOT_SIZE - tw - 2, y + 2, 0xFFFFFF);
                    }
                }

                // Gray overlay for non-block items (cannot be selected)
                if (!itemStack.isEmpty() && !isBlock) {
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, GRAY_OVERLAY);
                }

                // White hover highlight
                boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
                if (hovered) {
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, HOVER);
                }
            }
        }
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
