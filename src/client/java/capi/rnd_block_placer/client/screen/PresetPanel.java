package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.screen.widget.TextButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Preset management: scrollable preset list on the right, and a centered name input / create / save-to-preset section below the container
public class PresetPanel {
    private static final long MESSAGE_DURATION_MS = 3000;
    // Tooltip box sizing and colors
    private static final int TOOLTIP_PADDING = 3;
    private static final int TOOLTIP_ROW_HEIGHT = 18;
    private static final int TOOLTIP_ICON_SIZE = 16;
    private static final int TOOLTIP_ICON_GAP = 2;
    private static final int TOOLTIP_MAX_TEXT_WIDTH = 120;
    private static final int TOOLTIP_OFFSET_X = 8;
    private static final int TOOLTIP_BACKGROUND_COLOR = 0xF0100010;
    private static final int TOOLTIP_BORDER_COLOR = 0xFF1F001F;

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
        extract.text(font, Component.translatable("label.rnd-block-placer.presets"),
                panelX(), topPos + PANEL_HEADER_Y_OFFSET, PRESET_HEADER_COLOR);

        String active = BlockPlacerConfig.INSTANCE.getActivePreset();
        if (active != null) {
            extract.text(font, Component.translatable("label.rnd-block-placer.preset.active", active),
                    panelX(), topPos + PANEL_ACTIVE_LABEL_Y_OFFSET, PRESET_ACTIVE_COLOR);
        }

        renderRows(extract);
        renderBottomSection(extract, mx, my);
        renderMessage(extract);
        renderTooltip(extract, mx, my);
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
            y += PANEL_ROW_HEIGHT;
        }
        return false;
    }

    private void renderRows(GuiGraphicsExtractor extract) {
        List<String> names = presetNames();
        int start = scrollOffset;
        int y = rowsStartY();
        for (int i = start; i < names.size() && i < start + PRESET_MAX_ROWS; i++) {
            String name = names.get(i);
            boolean active = name.equals(BlockPlacerConfig.INSTANCE.getActivePreset());
            Component nameText = Component.literal(truncate(name, PRESET_NAME_MAX_WIDTH));
            extract.text(font, nameText, panelX(), y + buttonAlignY(), active ? PRESET_ACTIVE_COLOR : PRESET_TEXT_COLOR);
            renderCenteredLabel(extract, Component.translatable("button.rnd-block-placer.preset.select"),
                    selectButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT);
            boolean armed = name.equals(armedDelete);
            Component deleteLabel = armed
                    ? Component.translatable("button.rnd-block-placer.preset.confirm").withColor(0xFF800700)
                    : Component.translatable("button.rnd-block-placer.preset.delete").withColor(0xFF800700);
            renderCenteredLabel(extract, deleteLabel, deleteButtonX(), y, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT);
            y += PANEL_ROW_HEIGHT;
        }

        // Hint that the list can be scrolled when it exceeds the visible area
        if (names.size() > PRESET_MAX_ROWS) {
            int remaining = names.size() - PRESET_MAX_ROWS;
            extract.text(font, Component.translatable("label.rnd-block-placer.preset.more", remaining),
                    panelX(), rowsStartY() + PRESET_MAX_ROWS * PANEL_ROW_HEIGHT, SELECTED_PANEL_MORE_COLOR);
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

    // Draws a preview box with the hovered preset's block icons and weights
    private void renderTooltip(GuiGraphicsExtractor extract, int mx, int my) {
        String name = hoveredPresetName(mx, my);
        if (name == null) {
            return;
        }
        List<Map.Entry<Identifier, Integer>> entries = presetPreviewEntries(name);
        List<Component> weightLines = new ArrayList<>();
        for (Map.Entry<Identifier, Integer> entry : entries) {
            weightLines.add(weightLine(entry, totalWeight(entries)));
        }
        if (weightLines.isEmpty()) {
            weightLines.add(Component.translatable("label.rnd-block-placer.preset.empty"));
        }

        int iconOffset = entries.isEmpty() ? 0 : TOOLTIP_ICON_SIZE + TOOLTIP_ICON_GAP;
        int maxTextWidth = 0;
        for (Component line : weightLines) {
            maxTextWidth = Math.max(maxTextWidth, Math.min(font.width(line), TOOLTIP_MAX_TEXT_WIDTH));
        }
        int boxWidth = TOOLTIP_PADDING * 2 + iconOffset + maxTextWidth;
        int boxHeight = weightLines.size() * TOOLTIP_ROW_HEIGHT + TOOLTIP_PADDING * 2;
        int boxX = panelX() - boxWidth - TOOLTIP_OFFSET_X;
        if (boxX < 4) {
            boxX = 4;
        }
        int rowY = rowsStartY() + (int) ((my - rowsStartY()) / PANEL_ROW_HEIGHT) * PANEL_ROW_HEIGHT;
        int maxY = mc.getWindow().getGuiScaledHeight() - boxHeight - 4;
        int boxY = Math.max(4, Math.min(maxY, rowY));

        extract.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, TOOLTIP_BORDER_COLOR);
        extract.fill(boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + boxHeight - 1, TOOLTIP_BACKGROUND_COLOR);
        for (int i = 0; i < weightLines.size(); i++) {
            int lineY = boxY + TOOLTIP_PADDING + i * TOOLTIP_ROW_HEIGHT;
            if (!entries.isEmpty()) {
                Identifier id = entries.get(i).getKey();
                extract.item(new ItemStack(BuiltInRegistries.ITEM.getValue(id), 1), boxX + TOOLTIP_PADDING, lineY);
            }
            Component line = weightLines.get(i);
            extract.text(font, line, boxX + TOOLTIP_PADDING + iconOffset, lineY + 4, PRESET_TEXT_COLOR);
        }
    }

    // Returns the preset name under the cursor, or null when not over a preset row
    private String hoveredPresetName(double mx, double my) {
        if (mx < panelX() || mx > deleteButtonX() + PRESET_BUTTON_WIDTH || my < rowsStartY()) {
            return null;
        }
        int row = (int) ((my - rowsStartY()) / PANEL_ROW_HEIGHT);
        if (row < 0 || row >= PRESET_MAX_ROWS) {
            return null;
        }
        List<String> names = presetNames();
        int index = scrollOffset + row;
        return index < names.size() ? names.get(index) : null;
    }

    // Returns a preset's block entries sorted by weight descending, ties broken by identifier
    private List<Map.Entry<Identifier, Integer>> presetPreviewEntries(String name) {
        Map<Identifier, Integer> blocks = BlockPlacerConfig.INSTANCE.getPreset(name);
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<Identifier, Integer>> sorted = new ArrayList<>(blocks.entrySet());
        sorted.sort(Map.Entry.<Identifier, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))));
        return sorted;
    }

    // Builds the "pct% (weight)" summary line for a block entry
    private Component weightLine(Map.Entry<Identifier, Integer> entry, int total) {
        return Component.translatable("label.rnd-block-placer.weight_summary",
                entry.getValue() * 100 / total, entry.getValue());
    }

    // Returns the sum of all entry weights
    private int totalWeight(List<Map.Entry<Identifier, Integer>> entries) {
        int total = 0;
        for (Map.Entry<Identifier, Integer> entry : entries) {
            total += entry.getValue();
        }
        return total;
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
                && my < rowsStartY() + PRESET_MAX_ROWS * PANEL_ROW_HEIGHT;
    }

    private int panelX() {
        return leftPos + PRESET_PANEL_X_OFFSET;
    }

    private int rowsStartY() {
        return topPos + PANEL_ROW_START_Y_OFFSET;
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
        return (PANEL_ROW_HEIGHT - font.lineHeight) / 2;
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