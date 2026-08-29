package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Inline weight editor opened by Shift+click on a block: an EditBox plus a label describing the edited block
public class WeightEditor {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 16;
    private static final int MAX_LENGTH = 4;

    private final EditBox input;
    private final Font font;
    private final BlockSelectionScreenState state;

    private Identifier editingSlot;
    private Component label = Component.empty();

    public WeightEditor(Font font, BlockSelectionScreenState state, int x, int y) {
        this.font = font;
        this.state = state;
        this.input = new EditBox(font, x, y, WIDTH, HEIGHT, Component.translatable("button.rnd-block-placer.weight"));
        input.setMaxLength(MAX_LENGTH);
        input.setValue(String.valueOf(BlockPlacerConfig.DEFAULT_WEIGHT));
        input.setVisible(false);
        input.setFocused(false);
    }

    // The underlying EditBox, registered and focused by the owning Screen
    public EditBox getInput() {
        return input;
    }

    // Opens the editor for the given block, pre-filled with its current weight
    public void startEditing(Identifier id, ItemStack stack) {
        editingSlot = id;
        input.setValue(String.valueOf(state.getWeightOrElse(id, BlockPlacerConfig.DEFAULT_WEIGHT)));
        input.setVisible(true);
        input.setFocused(true);
        input.setCursorPosition(input.getValue().length());
        label = Component.translatable("label.rnd-block-placer.weight_for", stack.getHoverName().getString());
    }

    // Closes the editor without saving
    public void stopEditing() {
        input.setVisible(false);
        input.setFocused(false);
        editingSlot = null;
    }

    // Applies the typed weight to the working state, then closes the editor
    public void confirm() {
        if (editingSlot == null) return;
        try {
            state.setWeight(editingSlot, Integer.parseInt(input.getValue()));
        } catch (NumberFormatException e) {
            // Invalid input — ignore silently
        }
        stopEditing();
    }

    // Whether the editor is open for some block
    public boolean isEditing() {
        return input.isVisible() && editingSlot != null;
    }

    // Whether the editor is currently open for the given block
    public boolean isEditing(Identifier id) {
        return isEditing() && editingSlot.equals(id);
    }

    // Handles Escape (cancel) and Enter (confirm); returns true if the key was consumed
    public boolean handleKey(KeyEvent event) {
        if (!isEditing()) return false;
        if (event.isEscape()) {
            stopEditing();
            return true;
        }
        if (event.isConfirmation()) {
            confirm();
            return true;
        }
        return false;
    }

    // Draws the label above the input while editing
    public void render(GuiGraphicsExtractor extract, int leftPos, int topPos) {
        if (!isEditing()) return;
        extract.text(font, label, leftPos + SLOT_X, topPos + DISPLAY_IMAGE_H - WEIGHT_LABEL_Y_OFFSET, WEIGHT_LABEL_COLOR);
    }
}
