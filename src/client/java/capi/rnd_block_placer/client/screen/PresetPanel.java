package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.screen.widget.TextButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Preset management: scrollable preset list on the right, and a centered name input / create / save-to-preset section below the container
public class PresetPanel {
    private static final long MESSAGE_DURATION_MS = 3000;

    private final BlockSelectionScreenState state;
    private final Minecraft mc;
    private final Font font;

    private final int leftPos;
    private final int topPos;

    private final EditBox nameInput;
    private final TextButton createButton;
    private final TextButton saveButton;

    private int scrollOffset;
    private String armedDelete;
    private boolean armedSaveToPreset;

    private Component message = Component.empty();
    private long messageUntil;

    public PresetPanel(Font font, Minecraft mc, BlockSelectionScreenState state, int leftPos, int topPos) {
        this.font = font;
        this.mc = mc;
        this.state = state;
        this.leftPos = leftPos;
        this.topPos = topPos;

        int centerX = leftPos + DISPLAY_IMAGE_W / 2;
        int inputRowWidth = PRESET_NAME_INPUT_WIDTH + PRESET_BUTTON_SPACING + PRESET_CREATE_BUTTON_WIDTH;
        int inputY = topPos + DISPLAY_IMAGE_H + PRESET_BOTTOM_Y_OFFSET;
        int inputX = centerX - inputRowWidth / 2;

        this.nameInput = new EditBox(font, inputX, inputY, PRESET_NAME_INPUT_WIDTH, PRESET_NAME_INPUT_HEIGHT,
                Component.translatable("button.rnd-block-placer.preset.name"));
        nameInput.setMaxLength(PRESET_NAME_MAX_LENGTH);
        nameInput.setHint(Component.translatable("button.rnd-block-placer.preset.name_hint"));

        this.createButton = new TextButton(
                inputX + PRESET_NAME_INPUT_WIDTH + PRESET_BUTTON_SPACING,
                inputY,
                PRESET_CREATE_BUTTON_WIDTH,
                PRESET_CREATE_BUTTON_HEIGHT,
                Component.translatable("button.rnd-block-placer.preset.create"),
                this::handleCreate
        );
        this.saveButton = new TextButton(
                centerX - PRESET_SAVE_BUTTON_WIDTH / 2,
                inputY + PRESET_CREATE_BUTTON_HEIGHT + PRESET_BUTTON_ROW_SPACING,
                PRESET_SAVE_BUTTON_WIDTH,
                PRESET_CREATE_BUTTON_HEIGHT,
                Component.translatable("button.rnd-block-placer.preset.save"),
                this::handleSaveToPreset
        );
    }

    // The name input EditBox, registered and focused by the owning Screen
    public EditBox getInput() {
        return nameInput;
    }

    // Handles clicks on the panel controls; returns true if the click was consumed
    public boolean mouseClicked(double mx, double my) {
        if (createButton.isHover(mx, my)) {
            clickSound();
            handleCreate();
            return true;
        }
        if (saveButton.isHover(mx, my)) {
            clickSound();
            handleSaveToPreset();
            return true;
        }
        if (rowButtonHit(mx, my)) {
            return true;
        }
        cancelArmed();
        return false;
    }

    // Scrolls the preset list when the mouse wheel is used over the list area
    public boolean mouseScrolled(double mx, double my, double verticalAmount) {
        if (!isOverRowsArea(mx, my) || maxScrollOffset() == 0) {
            return false;
        }
        scrollOffset = Math.max(0, Math.min(maxScrollOffset(), scrollOffset - (int) verticalAmount));
        return true;
    }

    // Handles Enter in the name input to create a preset; returns true if the key was consumed
    public boolean keyPressed(KeyEvent event) {
        if (nameInput.isFocused() && event.isConfirmation()) {
            handleCreate();
            return true;
        }
        return false;
    }

    // Draws the preset header, scrolled list, bottom section, and any transient message
    public void render(GuiGraphicsExtractor extract, int mx, int my) {
        int panelY = topPos + PRESET_PANEL_Y_OFFSET;

        extract.text(font, Component.translatable("label.rnd-block-placer.presets"), panelX(), panelY, PRESET_HEADER_COLOR);

        String active = BlockPlacerConfig.INSTANCE.getActivePreset();
        if (active != null) {
            extract.text(font, Component.translatable("label.rnd-block-placer.preset.active", active),
                    panelX(), panelY + PRESET_ACTIVE_Y_OFFSET, PRESET_ACTIVE_COLOR);
        }

        renderRows(extract);
        renderBottomSection(extract, mx, my);
        renderMessage(extract);
    }

    // Loads the preset into the working and live selection, marking it as active
    private void handleSelect(String name) {
        Map<Identifier, Integer> preset = BlockPlacerConfig.INSTANCE.getPreset(name);
        if (preset == null) {
            return;
        }
        state.setWeights(preset);
        BlockPlacerConfig.INSTANCE.setSelectedBlocks(preset);
        BlockPlacerConfig.INSTANCE.setActivePreset(name);
        BlockPlacerConfig.INSTANCE.save();
        clearMessage();
        cancelArmed();
    }

    // Creates a preset from the working selection under the typed name
    private void handleCreate() {
        String name = nameInput.getValue().trim();
        if (name.isEmpty()) {
            showMessage(Component.translatable("message.rnd-block-placer.preset.name_empty"));
            return;
        }
        if (BlockPlacerConfig.INSTANCE.getPresets().containsKey(name)) {
            showMessage(Component.translatable("message.rnd-block-placer.preset.name_exists", name));
            return;
        }
        BlockPlacerConfig.INSTANCE.putPreset(name, state.copyWeights());
        BlockPlacerConfig.INSTANCE.setActivePreset(name);
        BlockPlacerConfig.INSTANCE.save();
        nameInput.setValue("");
        clearMessage();
        cancelArmed();
    }

    // Deletes a preset on the confirming click, arming the confirmation on the first click
    private void handleDelete(String name) {
        if (name.equals(armedDelete)) {
            BlockPlacerConfig.INSTANCE.removePreset(name);
            BlockPlacerConfig.INSTANCE.save();
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
            clearMessage();
            cancelArmed();
        } else {
            cancelArmed();
            armedDelete = name;
        }
    }

    // Overwrites the active preset with the working selection on the confirming click
    private void handleSaveToPreset() {
        String active = BlockPlacerConfig.INSTANCE.getActivePreset();
        if (active == null) {
            showMessage(Component.translatable("message.rnd-block-placer.preset.no_active"));
            return;
        }
        if (armedSaveToPreset) {
            BlockPlacerConfig.INSTANCE.putPreset(active, state.copyWeights());
            BlockPlacerConfig.INSTANCE.save();
            clearMessage();
            cancelArmed();
        } else {
            cancelArmed();
            armedSaveToPreset = true;
        }
    }

    private boolean rowButtonHit(double mx, double my) {
        List<String> names = presetNames();
        int start = scrollOffset;
        int y = rowsStartY();
        for (int i = start; i < names.size() && i < start + PRESET_MAX_ROWS; i++) {
            String name = names.get(i);
            if (hit(mx, my, selectButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT)) {
                clickSound();
                handleSelect(name);
                return true;
            }
            if (hit(mx, my, deleteButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT)) {
                clickSound();
                handleDelete(name);
                return true;
            }
            y += PRESET_ROW_HEIGHT;
        }
        return false;
    }

    private void renderRows(GuiGraphicsExtractor extract) {
        List<String> names = presetNames();
        int start = scrollOffset;
        int y = rowsStartY();
        for (int i = start; i < names.size() && i < start + PRESET_MAX_ROWS; i++) {
            String name = names.get(i);
            Component nameText = Component.literal(truncate(name, PRESET_NAME_MAX_WIDTH));
            extract.text(font, nameText, panelX(), y + buttonAlignY(), PRESET_TEXT_COLOR);
            renderCenteredLabel(extract, Component.translatable("button.rnd-block-placer.preset.select"),
                    selectButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT);
            boolean armed = name.equals(armedDelete);
            Component deleteLabel = armed
                    ? Component.translatable("button.rnd-block-placer.preset.confirm")
                    : Component.translatable("button.rnd-block-placer.preset.delete");
            renderCenteredLabel(extract, deleteLabel, deleteButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT);
            y += PRESET_ROW_HEIGHT;
        }

        // Hint that the list can be scrolled when it exceeds the visible area
        if (names.size() > PRESET_MAX_ROWS) {
            int remaining = names.size() - PRESET_MAX_ROWS;
            extract.text(font, Component.translatable("label.rnd-block-placer.preset.more", remaining),
                    panelX(), rowsStartY() + PRESET_MAX_ROWS * PRESET_ROW_HEIGHT, SELECTED_PANEL_MORE_COLOR);
        }
    }

    private void renderBottomSection(GuiGraphicsExtractor extract, int mx, int my) {
        saveButton.setLabel(armedSaveToPreset
                ? Component.translatable("button.rnd-block-placer.preset.confirm")
                : Component.translatable("button.rnd-block-placer.preset.save"));
        createButton.render(extract, font, mx, my);
        saveButton.render(extract, font, mx, my);
    }

    private void renderMessage(GuiGraphicsExtractor extract) {
        if (System.currentTimeMillis() >= messageUntil) {
            return;
        }
        int centerX = leftPos + DISPLAY_IMAGE_W / 2;
        int y = saveButtonBottomY() + PRESET_BUTTON_ROW_SPACING;
        extract.text(font, message, centerX - font.width(message) / 2, y, PRESET_MESSAGE_COLOR);
    }

    // Renders a label centered within the given button bounds
    private void renderCenteredLabel(GuiGraphicsExtractor extract, Component label, int bx, int by, int bw, int bh) {
        int tx = bx + (bw - font.width(label)) / 2;
        int ty = by + (bh - font.lineHeight) / 2;
        extract.text(font, label, tx, ty, PRESET_TEXT_COLOR);
    }

    // Shortens a name to the given pixel width, appending an ellipsis
    private String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - 6) + "...";
    }

    private List<String> presetNames() {
        return List.copyOf(BlockPlacerConfig.INSTANCE.getPresets().keySet());
    }

    private int maxScrollOffset() {
        return Math.max(0, BlockPlacerConfig.INSTANCE.getPresets().size() - PRESET_MAX_ROWS);
    }

    private boolean isOverRowsArea(double mx, double my) {
        return mx >= panelX()
                && mx < deleteButtonX() + PRESET_BUTTON_WIDTH
                && my >= rowsStartY()
                && my < rowsStartY() + PRESET_MAX_ROWS * PRESET_ROW_HEIGHT;
    }

    private int panelX() {
        return leftPos + PRESET_PANEL_X_OFFSET;
    }

    private int rowsStartY() {
        return topPos + PRESET_PANEL_Y_OFFSET + PRESET_ROW_START_Y_OFFSET;
    }

    private int selectButtonX() {
        return panelX() + PRESET_NAME_MAX_WIDTH + PRESET_BUTTON_SPACING;
    }

    private int deleteButtonX() {
        return selectButtonX() + PRESET_BUTTON_WIDTH + PRESET_BUTTON_SPACING;
    }

    private int saveButtonBottomY() {
        return topPos + DISPLAY_IMAGE_H + PRESET_BOTTOM_Y_OFFSET
                + PRESET_CREATE_BUTTON_HEIGHT + PRESET_BUTTON_ROW_SPACING + PRESET_CREATE_BUTTON_HEIGHT;
    }

    private int buttonAlignY() {
        return (PRESET_ROW_HEIGHT - font.lineHeight) / 2;
    }

    private void showMessage(Component newMessage) {
        message = newMessage;
        messageUntil = System.currentTimeMillis() + MESSAGE_DURATION_MS;
    }

    private void clearMessage() {
        message = Component.empty();
        messageUntil = 0;
    }

    private void cancelArmed() {
        armedDelete = null;
        armedSaveToPreset = false;
    }

    private static boolean hit(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    private void clickSound() {
        LocalPlayer player = mc.player;
        if (player != null) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), BUTTON_SOUND_VOLUME, BUTTON_SOUND_PITCH);
        }
    }
}