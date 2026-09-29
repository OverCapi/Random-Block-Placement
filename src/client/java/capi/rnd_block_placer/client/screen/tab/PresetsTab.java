package capi.rnd_block_placer.client.screen.tab;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.screen.BlockSelectionScreenState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// "Presets" tab: the saved presets, actions on the highlighted one, and creation from the current selection
public class PresetsTab extends SidebarTab {
    private static final int STATUS_HEIGHT = 12;
    private static final int BUTTON_ROW_SPACING = 2;
    private static final int CREATE_BUTTON_WIDTH = 44;
    private static final int NAME_MAX_LENGTH = 20;
    private static final int PREVIEW_MAX_LINES = 8;
    private static final long DOUBLE_CLICK_MS = 400;

    // Destructive action waiting for a confirming second click
    private enum Armed { NONE, UPDATE, DELETE }

    private final BlockSelectionScreenState state;

    // Preset the actions apply to, or null
    private String highlighted;
    private Armed armed = Armed.NONE;
    // Name typed in the create input (kept across widget rebuilds)
    private String nameText = "";
    private long lastClickTime;

    private Button loadButton;
    private Button deleteButton;
    private Button updateButton;
    private EditBox nameBox;
    private Button createButton;
    private String createTooltipKey;
    private ScreenRectangle area = ScreenRectangle.empty();

    public PresetsTab(Font font, BlockSelectionScreenState state) {
        super(font);
        this.state = state;
    }

    @Override
    public Component title() {
        return Component.translatable("tab.rnd-block-placer.presets", BlockPlacerConfig.INSTANCE.getPresets().size());
    }

    @Override
    public void init(ScreenRectangle area, Consumer<AbstractWidget> addWidget) {
        this.area = area;
        int createY = area.bottom() - SIDEBAR_BUTTON_HEIGHT;
        int updateY = createY - BUTTON_ROW_SPACING - SIDEBAR_BUTTON_HEIGHT;
        int loadY = updateY - BUTTON_ROW_SPACING - SIDEBAR_BUTTON_HEIGHT;
        int listTop = area.top() + STATUS_HEIGHT;
        setListArea(new ScreenRectangle(area.left(), listTop, area.width(), loadY - SIDEBAR_SPACING - listTop));

        int halfWidth = (area.width() - BUTTON_ROW_SPACING) / 2;
        loadButton = Button.builder(Component.translatable("button.rnd-block-placer.preset.load"), button -> load())
                .bounds(area.left(), loadY, halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.preset.load")))
                .build();
        deleteButton = Button.builder(Component.translatable("button.rnd-block-placer.preset.delete"), button -> delete())
                .bounds(area.right() - halfWidth, loadY, halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.preset.delete")))
                .build();
        updateButton = Button.builder(Component.translatable("button.rnd-block-placer.preset.update"), button -> update())
                .bounds(area.left(), updateY, area.width(), SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.preset.update")))
                .build();

        nameBox = new EditBox(font, area.left(), createY, area.width() - CREATE_BUTTON_WIDTH - BUTTON_ROW_SPACING,
                SIDEBAR_BUTTON_HEIGHT, Component.translatable("button.rnd-block-placer.preset.name"));
        nameBox.setMaxLength(NAME_MAX_LENGTH);
        nameBox.setHint(Component.translatable("button.rnd-block-placer.preset.name_hint"));
        nameBox.setValue(nameText);
        nameBox.setResponder(value -> nameText = value);
        createButton = Button.builder(Component.translatable("button.rnd-block-placer.preset.create"), button -> create())
                .bounds(area.right() - CREATE_BUTTON_WIDTH, createY, CREATE_BUTTON_WIDTH, SIDEBAR_BUTTON_HEIGHT)
                .build();
        createTooltipKey = null;

        addWidget.accept(loadButton);
        addWidget.accept(deleteButton);
        addWidget.accept(updateButton);
        addWidget.accept(nameBox);
        addWidget.accept(createButton);
        updateWidgets();
    }

    @Override
    public void updateWidgets() {
        if (highlighted != null && BlockPlacerConfig.INSTANCE.getPreset(highlighted) == null) {
            highlighted = null;
        }
        boolean hasTarget = highlighted != null;
        loadButton.active = hasTarget;
        deleteButton.active = hasTarget;
        deleteButton.setMessage(armed == Armed.DELETE
                ? Component.translatable("button.rnd-block-placer.preset.confirm").withStyle(ChatFormatting.RED)
                : Component.translatable("button.rnd-block-placer.preset.delete"));
        updateButton.active = hasTarget && !state.isEmpty();
        updateButton.setMessage(armed == Armed.UPDATE
                ? Component.translatable("button.rnd-block-placer.preset.confirm").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("button.rnd-block-placer.preset.update"));

        String blockReason = createBlockReason();
        createButton.active = blockReason == null;
        String tooltipKey = blockReason != null ? blockReason : "tooltip.rnd-block-placer.preset.create";
        if (!tooltipKey.equals(createTooltipKey)) {
            createTooltipKey = tooltipKey;
            createButton.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        }
    }

    // Returns the translation key explaining why a preset cannot be created, or null if it can
    private String createBlockReason() {
        String name = nameText.trim();
        if (state.isEmpty()) {
            return "tooltip.rnd-block-placer.preset.create.empty_selection";
        }
        if (name.isEmpty()) {
            return "tooltip.rnd-block-placer.preset.create.no_name";
        }
        if (BlockPlacerConfig.INSTANCE.getPresets().containsKey(name)) {
            return "tooltip.rnd-block-placer.preset.create.name_exists";
        }
        return null;
    }

    // Replaces the working selection with the highlighted preset
    private void load() {
        if (highlighted != null) {
            state.loadPreset(highlighted);
        }
        armed = Armed.NONE;
    }

    // Deletes the highlighted preset on the confirming click
    private void delete() {
        if (highlighted == null) {
            return;
        }
        if (armed != Armed.DELETE) {
            armed = Armed.DELETE;
            return;
        }
        BlockPlacerConfig.INSTANCE.removePreset(highlighted);
        BlockPlacerConfig.INSTANCE.save();
        if (highlighted.equals(state.getActivePreset())) {
            state.setActivePreset(null);
        }
        highlighted = null;
        armed = Armed.NONE;
        clampScroll();
    }

    // Overwrites the highlighted preset with the working selection on the confirming click
    private void update() {
        if (highlighted == null || state.isEmpty()) {
            return;
        }
        if (armed != Armed.UPDATE) {
            armed = Armed.UPDATE;
            return;
        }
        BlockPlacerConfig.INSTANCE.putPreset(highlighted, state.copyWeights());
        BlockPlacerConfig.INSTANCE.save();
        state.setActivePreset(highlighted);
        armed = Armed.NONE;
    }

    // Saves the working selection as a new preset under the typed name
    private void create() {
        if (createBlockReason() != null) {
            return;
        }
        String name = nameText.trim();
        BlockPlacerConfig.INSTANCE.putPreset(name, state.copyWeights());
        BlockPlacerConfig.INSTANCE.save();
        state.setActivePreset(name);
        highlighted = name;
        armed = Armed.NONE;
        nameBox.setValue("");
        ensureVisible(presetNames().indexOf(name));
    }

    // Cancels a pending confirmation when the click lands anywhere but the armed button
    public void onAnyClick(double mx, double my) {
        if (armed == Armed.DELETE && !deleteButton.isMouseOver(mx, my)
                || armed == Armed.UPDATE && !updateButton.isMouseOver(mx, my)) {
            armed = Armed.NONE;
        }
    }

    @Override
    protected int rowCount() {
        return BlockPlacerConfig.INSTANCE.getPresets().size();
    }

    @Override
    public void render(GuiGraphicsExtractor extract, int mx, int my) {
        renderStatus(extract);

        ScreenRectangle list = listArea();
        List<String> names = presetNames();
        if (names.isEmpty()) {
            extract.textWithWordWrap(font, Component.translatable("label.rnd-block-placer.preset.none"),
                    list.left(), list.top() + 2, list.width(), TEXT_MUTED_COLOR);
        }

        int hovered = rowAt(mx, my);
        int rowWidth = rowWidth();
        String active = state.getActivePreset();
        for (int i = firstVisibleRow(); i < endVisibleRow(); i++) {
            String name = names.get(i);
            int y = rowY(i);
            int x = list.left();

            if (name.equals(highlighted)) {
                extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_SELECTED_COLOR);
            } else if (i == hovered) {
                extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_HOVER_COLOR);
            }

            int textY = y + (SIDEBAR_ROW_HEIGHT - font.lineHeight) / 2 + 1;
            int color = name.equals(active) ? TEXT_ACTIVE_COLOR : TEXT_COLOR;
            extract.text(font, truncate(name, rowWidth - 8), x + 4, textY, color);

            if (i == hovered) {
                extract.setComponentTooltipForNextFrame(font, previewLines(name), mx, my);
            }
        }
        renderScrollbar(extract);
    }

    // Draws which preset the selection was loaded from, and whether it has been modified since
    private void renderStatus(GuiGraphicsExtractor extract) {
        String active = state.getActivePreset();
        if (active == null || BlockPlacerConfig.INSTANCE.getPreset(active) == null) {
            extract.text(font, Component.translatable("label.rnd-block-placer.preset.not_loaded"),
                    area.left(), area.top(), TEXT_MUTED_COLOR);
            return;
        }
        boolean modified = state.isActivePresetModified();
        String key = modified ? "label.rnd-block-placer.preset.loaded_modified" : "label.rnd-block-placer.preset.loaded";
        int prefixWidth = font.width(Component.translatable(key, ""));
        Component label = Component.translatable(key, truncate(active, area.width() - prefixWidth));
        extract.text(font, label, area.left(), area.top(), modified ? TEXT_WARNING_COLOR : TEXT_ACTIVE_COLOR);
    }

    // Tooltip listing a preset's blocks with their share, heaviest first
    private List<Component> previewLines(String name) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(name).withStyle(ChatFormatting.BOLD));

        Map<Identifier, Integer> blocks = BlockPlacerConfig.INSTANCE.getPreset(name);
        if (blocks == null || blocks.isEmpty()) {
            lines.add(Component.translatable("label.rnd-block-placer.preset.empty").withStyle(ChatFormatting.GRAY));
        } else {
            List<Map.Entry<Identifier, Integer>> sorted = new ArrayList<>(blocks.entrySet());
            sorted.sort(Map.Entry.<Identifier, Integer>comparingByValue().reversed()
                    .thenComparing(entry -> entry.getKey().toString()));
            int total = sorted.stream().mapToInt(Map.Entry::getValue).sum();
            for (int i = 0; i < Math.min(sorted.size(), PREVIEW_MAX_LINES); i++) {
                Map.Entry<Identifier, Integer> entry = sorted.get(i);
                Component itemName = new ItemStack(BuiltInRegistries.ITEM.getValue(entry.getKey())).getHoverName();
                lines.add(Component.translatable("tooltip.rnd-block-placer.preset.entry",
                        itemName, BlockSelectionScreenState.percent(entry.getValue(), total)).withStyle(ChatFormatting.GRAY));
            }
            if (sorted.size() > PREVIEW_MAX_LINES) {
                lines.add(Component.translatable("tooltip.rnd-block-placer.preset.more", sorted.size() - PREVIEW_MAX_LINES)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.translatable("tooltip.rnd-block-placer.preset.row").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mx, double my) {
        int index = rowAt(mx, my);
        if (index < 0) {
            return false;
        }
        String name = presetNames().get(index);
        long now = System.currentTimeMillis();
        // Double-click loads the preset directly
        if (name.equals(highlighted) && now - lastClickTime < DOUBLE_CLICK_MS) {
            load();
        }
        highlighted = name;
        armed = Armed.NONE;
        lastClickTime = now;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox.isFocused() && event.isConfirmation()) {
            create();
            return true;
        }
        return false;
    }

    private List<String> presetNames() {
        return List.copyOf(BlockPlacerConfig.INSTANCE.getPresets().keySet());
    }
}
