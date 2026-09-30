package capi.rnd_block_placer.client.screen.tab;

import capi.rnd_block_placer.client.screen.BlockSelectionScreenState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// "Selection" tab: the selected blocks with their chance, a percentage editor and a clear button
public class SelectionTab extends SidebarTab {
    private static final int LABEL_HEIGHT = 11;
    private static final int EDITOR_HEIGHT = LABEL_HEIGHT + SIDEBAR_BUTTON_HEIGHT;
    private static final int CONFIRM_BUTTON_WIDTH = 24;
    private static final int REMOVE_WIDTH = 10;
    private static final int PERCENT_MAX_LENGTH = 3;

    private final BlockSelectionScreenState state;
    private final Consumer<GuiEventListener> focus;

    // Block whose chance is being edited, and the percentage typed so far (kept across widget rebuilds)
    private Identifier editingId;
    private String editingText = "";

    private EditBox weightBox;
    private Button confirmButton;
    private Button clearButton;
    private ScreenRectangle editorArea = ScreenRectangle.empty();

    public SelectionTab(Font font, BlockSelectionScreenState state, Consumer<GuiEventListener> focus) {
        super(font);
        this.state = state;
        this.focus = focus;
    }

    @Override
    public Component title() {
        return Component.translatable("tab.rnd-block-placer.selection", state.size());
    }

    @Override
    public void init(ScreenRectangle area, Consumer<AbstractWidget> addWidget) {
        int clearY = area.bottom() - SIDEBAR_BUTTON_HEIGHT;
        int editorY = clearY - SIDEBAR_SPACING - EDITOR_HEIGHT;
        editorArea = new ScreenRectangle(area.left(), editorY, area.width(), EDITOR_HEIGHT);
        setListArea(new ScreenRectangle(area.left(), area.top(), area.width(), editorY - SIDEBAR_SPACING - area.top()));

        int boxY = editorY + LABEL_HEIGHT;
        weightBox = new EditBox(font, area.left(), boxY, area.width() - CONFIRM_BUTTON_WIDTH - 2, SIDEBAR_BUTTON_HEIGHT,
                Component.translatable("button.rnd-block-placer.weight"));
        weightBox.setMaxLength(PERCENT_MAX_LENGTH);
        weightBox.setHint(Component.translatable("button.rnd-block-placer.weight.hint"));
        weightBox.setValue(editingText);
        weightBox.setResponder(this::onWeightTyped);

        confirmButton = Button.builder(Component.translatable("button.rnd-block-placer.weight.confirm"), button -> confirmEdit())
                .bounds(area.right() - CONFIRM_BUTTON_WIDTH, boxY, CONFIRM_BUTTON_WIDTH, SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.weight.confirm")))
                .build();

        clearButton = Button.builder(Component.translatable("button.rnd-block-placer.clear"), button -> clearSelection())
                .bounds(area.left(), clearY, area.width(), SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.clear")))
                .build();

        addWidget.accept(weightBox);
        addWidget.accept(confirmButton);
        addWidget.accept(clearButton);
        updateWidgets();

        if (editingId != null) {
            focus.accept(weightBox);
        }
    }

    @Override
    public void updateWidgets() {
        boolean editing = editingId != null;
        weightBox.visible = editing;
        confirmButton.visible = editing;
        clearButton.active = !state.isEmpty();
    }

    // Opens the chance editor for the given block, pre-filled with its current percentage
    public void startEditing(Identifier id) {
        editingId = id;
        editingText = String.valueOf(state.percentOrDefault(id));
        if (weightBox != null) {
            weightBox.setValue(editingText);
            weightBox.moveCursorToEnd(false);
            weightBox.visible = true;
            focus.accept(weightBox);
        }
        int index = indexOf(id);
        if (index >= 0) {
            ensureVisible(index);
        }
    }

    // Closes the chance editor without applying the typed value
    public void stopEditing() {
        editingId = null;
        editingText = "";
        if (weightBox != null && weightBox.isFocused()) {
            focus.accept(null);
        }
    }

    // Closes the editor if it was editing the given block
    public void stopEditing(Identifier id) {
        if (id.equals(editingId)) {
            stopEditing();
        }
    }

    // Applies the typed percentage (0 removes the block), then closes the editor
    private void confirmEdit() {
        if (editingId == null) {
            return;
        }
        try {
            state.setPercent(editingId, Integer.parseInt(editingText));
        } catch (NumberFormatException e) {
            // Empty input: keep the previous chance
        }
        stopEditing();
        clampScroll();
    }

    // Keeps only digits in the percentage input
    private void onWeightTyped(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (!digits.equals(value)) {
            weightBox.setValue(digits);
            return;
        }
        editingText = digits;
    }

    private void clearSelection() {
        state.clear();
        stopEditing();
        resetScroll();
    }

    @Override
    protected int rowCount() {
        return state.size();
    }

    @Override
    public void render(GuiGraphics extract, int mx, int my) {
        ScreenRectangle list = listArea();
        List<Map.Entry<Identifier, Integer>> entries = state.weightsSortedByDesc();

        if (entries.isEmpty()) {
            extract.drawWordWrap(font, Component.translatable("label.rnd-block-placer.selection.empty"),
                    list.left(), list.top() + 2, list.width(), TEXT_MUTED_COLOR);
        }

        int hovered = rowAt(mx, my);
        int rowWidth = rowWidth();
        for (int i = firstVisibleRow(); i < endVisibleRow(); i++) {
            Map.Entry<Identifier, Integer> entry = entries.get(i);
            Identifier id = entry.getKey();
            int y = rowY(i);
            int x = list.left();

            if (id.equals(editingId)) {
                extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_SELECTED_COLOR);
            } else if (i == hovered) {
                extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_HOVER_COLOR);
            }

            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            extract.renderItem(stack, x + 1, y + 1);

            int textY = y + (SIDEBAR_ROW_HEIGHT - font.lineHeight) / 2 + 1;
            int removeX = x + rowWidth - REMOVE_WIDTH;
            boolean overRemove = i == hovered && mx >= removeX;
            extract.drawString(font, "✕", removeX + 2, textY, overRemove ? TEXT_REMOVE_COLOR : TEXT_MUTED_COLOR);

            Component summary = Component.translatable("label.rnd-block-placer.weight_summary",
                    state.percentOf(entry.getValue()));
            int summaryX = removeX - 2 - font.width(summary);
            extract.drawString(font, summary, summaryX, textY, TEXT_COLOR);

            int nameX = x + 19;
            String name = truncate(stack.getHoverName().getString(), summaryX - 4 - nameX);
            extract.drawString(font, name, nameX, textY, TEXT_MUTED_COLOR);

            if (i == hovered) {
                extract.setTooltipForNextFrame(font, Component.translatable(overRemove
                        ? "tooltip.rnd-block-placer.row.remove"
                        : "tooltip.rnd-block-placer.row.edit"), mx, my);
            }
        }
        renderScrollbar(extract);
        renderEditorLabel(extract);
    }

    // Draws the "Chance: X" label while editing, or a hint on how to edit otherwise
    private void renderEditorLabel(GuiGraphics extract) {
        if (editingId == null) {
            extract.drawWordWrap(font, Component.translatable("hint.rnd-block-placer.edit_weight"),
                    editorArea.left(), editorArea.top() + 2, editorArea.width(), TEXT_MUTED_COLOR);
            return;
        }
        String name = new ItemStack(BuiltInRegistries.ITEM.getValue(editingId)).getHoverName().getString();
        int prefixWidth = font.width(Component.translatable("label.rnd-block-placer.weight_for", ""));
        Component label = Component.translatable("label.rnd-block-placer.weight_for",
                truncate(name, editorArea.width() - prefixWidth));
        extract.drawString(font, label, editorArea.left(), editorArea.top(), TEXT_COLOR);
    }

    @Override
    public boolean mouseClicked(double mx, double my) {
        int index = rowAt(mx, my);
        if (index < 0) {
            return false;
        }
        Identifier id = state.weightsSortedByDesc().get(index).getKey();
        if (mx >= listArea().left() + rowWidth() - REMOVE_WIDTH) {
            state.removeWeight(id);
            stopEditing(id);
            clampScroll();
        } else {
            startEditing(id);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editingId == null) {
            return false;
        }
        if (event.isConfirmation() && weightBox.isFocused()) {
            confirmEdit();
            return true;
        }
        if (event.isEscape()) {
            stopEditing();
            return true;
        }
        return false;
    }

    private int indexOf(Identifier id) {
        List<Map.Entry<Identifier, Integer>> entries = state.weightsSortedByDesc();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
