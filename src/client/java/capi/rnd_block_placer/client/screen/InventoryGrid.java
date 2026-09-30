package capi.rnd_block_placer.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Draws the clickable inventory grid: slots, enlarged icons, selection highlights, help text and hover tooltip
public class InventoryGrid {
    private final BlockSelectionScreenState state;
    private final Font font;

    public InventoryGrid(BlockSelectionScreenState state, Font font) {
        this.state = state;
        this.font = font;
    }

    // Returns the block item identifier of a stack, or null when the stack is not a block
    public static Identifier blockId(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public void render(GuiGraphics extract, SelectionLayout layout, LocalPlayer player, int mx, int my) {
        int size = layout.slotSize();
        int hoveredSlot = layout.slotAt(mx, my);

        for (int row = 0; row < INVENTORY_ROW; row++) {
            for (int col = 0; col < INVENTORY_COL; col++) {
                int slot = slotIndex(row, col);
                int x = layout.slotX(col);
                int y = layout.slotY(row);
                ItemStack stack = player.getInventory().getItem(slot);
                Identifier id = blockId(stack);
                boolean selected = id != null && state.containsWeight(id);

                extract.fill(x, y, x + size, y + size, selected ? SLOT_SELECTED_COLOR : SLOT_COLOR);
                extract.renderOutline(x, y, size, size, selected ? SLOT_SELECTED_BORDER_COLOR : SLOT_BORDER_COLOR);

                if (!stack.isEmpty()) {
                    drawIcon(extract, stack, x, y, size);
                    // Non-block items cannot be selected
                    if (id == null) {
                        extract.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_DISABLED_OVERLAY);
                    }
                }

                if (slot == hoveredSlot) {
                    extract.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_HOVER_OVERLAY);
                    if (!stack.isEmpty()) {
                        extract.setComponentTooltipForNextFrame(font, tooltip(stack, id), mx, my);
                    }
                }
            }
        }

        int hintY = layout.hintY();
        extract.drawString(font, Component.translatable("hint.rnd-block-placer.click"), layout.gridX(), hintY, TEXT_MUTED_COLOR);
        extract.drawString(font, Component.translatable("hint.rnd-block-placer.shift_click"), layout.gridX(), hintY + 11, TEXT_MUTED_COLOR);
    }

    // Draws the item icon scaled up to fill the slot, centered on it
    private void drawIcon(GuiGraphics extract, ItemStack stack, int x, int y, int size) {
        float scale = Math.max(1.0f, (size - 4) / 16.0f);
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        Matrix3x2fStack pose = extract.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale);
        pose.translate(-cx, -cy);
        extract.renderItem(stack, x + (size - 16) / 2, y + (size - 16) / 2);
        pose.popMatrix();
    }

    // Item name plus what clicking the slot does
    private List<Component> tooltip(ItemStack stack, Identifier id) {
        Component name = stack.getHoverName();
        if (id == null) {
            return List.of(name, Component.translatable("tooltip.rnd-block-placer.not_a_block").withStyle(ChatFormatting.GRAY));
        }
        if (state.containsWeight(id)) {
            return List.of(
                    name,
                    Component.translatable("tooltip.rnd-block-placer.weight", state.percentOf(state.getWeight(id))).withStyle(ChatFormatting.GREEN),
                    Component.translatable("tooltip.rnd-block-placer.click_remove").withStyle(ChatFormatting.GRAY)
            );
        }
        return List.of(name, Component.translatable("tooltip.rnd-block-placer.click_add").withStyle(ChatFormatting.GRAY));
    }
}
