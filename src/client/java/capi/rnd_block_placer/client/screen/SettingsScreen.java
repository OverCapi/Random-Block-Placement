package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.HudPosition;
import capi.rnd_block_placer.client.config.MissingBlockMode;
import capi.rnd_block_placer.client.config.SaveMode;
import capi.rnd_block_placer.client.config.TranslatableOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Mod settings, drawn with the same header / panel / footer look as the selection screen; returns to it when closed
public class SettingsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 300;
    private static final int CONTROL_WIDTH = 90;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_SPACING = 8;
    private static final int LABEL_HEIGHT = 11;

    // One setting: a label and a short description on the left, its control on the right
    private record SettingRow(Component label, Component description, AbstractWidget control) {}

    private final Screen parent;
    private final List<SettingRow> rows = new ArrayList<>();

    private ScreenRectangle header;
    private ScreenRectangle panel;
    private ScreenRectangle footer;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("screen.rnd-block-placer.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        rows.clear();
        addOptionRow(SaveMode.values(), config.getSaveMode(), config::setSaveMode);
        addOptionRow(MissingBlockMode.values(), config.getMissingBlockMode(), config::setMissingBlockMode);
        addOptionRow(HudPosition.values(), config.getHudPosition(), config::setHudPosition);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, width - 2 * SCREEN_MARGIN);
        int textWidth = textWidth(panelWidth);
        int panelHeight = 2 * PANEL_PADDING + PANEL_TITLE_HEIGHT + SIDEBAR_SPACING;
        for (int i = 0; i < rows.size(); i++) {
            panelHeight += rowHeight(rows.get(i), textWidth) + (i > 0 ? ROW_SPACING : 0);
        }

        int totalHeight = HEADER_HEIGHT + SECTION_GAP + panelHeight + SECTION_GAP + FOOTER_HEIGHT;
        int left = (width - panelWidth) / 2;
        int top = Math.max(0, (height - totalHeight) / 2);
        header = new ScreenRectangle(left, top, panelWidth, HEADER_HEIGHT);
        panel = new ScreenRectangle(left, header.bottom() + SECTION_GAP, panelWidth, panelHeight);
        footer = new ScreenRectangle(left, panel.bottom() + SECTION_GAP, panelWidth, FOOTER_HEIGHT);

        // Place each control on the right of its row, vertically centered
        ScreenRectangle inner = SelectionLayout.inner(panel);
        int y = inner.top() + PANEL_TITLE_HEIGHT + SIDEBAR_SPACING;
        for (SettingRow row : rows) {
            int rowHeight = rowHeight(row, textWidth);
            row.control().setPosition(inner.right() - CONTROL_WIDTH, y + (rowHeight - CONTROL_HEIGHT) / 2);
            addRenderableWidget(row.control());
            y += rowHeight + ROW_SPACING;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(footer.left() + (footer.width() - FOOTER_BUTTON_WIDTH) / 2, footer.top(), FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT)
                .build());
    }

    // Adds a row for an enum setting: label and description from its translation prefix, and a cycle
    // button that saves the config on every change
    private <T extends TranslatableOption> void addOptionRow(T[] values, T current, Consumer<T> setter) {
        String prefix = current.translationPrefix();
        Component label = Component.translatable(prefix);
        CycleButton<T> button = CycleButton.builder(TranslatableOption::label, current)
                .withValues(values)
                .withTooltip(value -> Tooltip.create(value.description()))
                .displayOnlyValue()
                .create(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT, label, (cycleButton, value) -> {
                    setter.accept(value);
                    BlockPlacerConfig.INSTANCE.save();
                });
        rows.add(new SettingRow(label, Component.translatable(prefix + ".description"), button));
    }

    // Width left for a row's label and description, beside its control
    private static int textWidth(int panelWidth) {
        return panelWidth - 2 * PANEL_PADDING - CONTROL_WIDTH - SECTION_GAP;
    }

    private int rowHeight(SettingRow row, int textWidth) {
        return Math.max(CONTROL_HEIGHT, LABEL_HEIGHT + font.wordWrapHeight(row.description(), textWidth));
    }

    @Override
    public void renderBackground(GuiGraphics extract, int mx, int my, float delta) {
        super.renderBackground(extract, mx, my, delta);
        PanelStyle.drawPanel(extract, panel);
    }

    @Override
    public void render(GuiGraphics extract, int mx, int my, float delta) {
        super.render(extract, mx, my, delta);
        PanelStyle.drawHeaderTitle(extract, font, header, Component.translatable("screen.rnd-block-placer.title"));
        PanelStyle.drawPanelTitle(extract, font, panel, title);

        ScreenRectangle inner = SelectionLayout.inner(panel);
        int textWidth = textWidth(panel.width());
        int y = inner.top() + PANEL_TITLE_HEIGHT + SIDEBAR_SPACING;
        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            if (i > 0) {
                int lineY = y - ROW_SPACING / 2;
                extract.fill(inner.left(), lineY, inner.right(), lineY + 1, PANEL_BORDER_COLOR);
            }
            int rowHeight = rowHeight(row, textWidth);
            int textTop = y + (rowHeight - LABEL_HEIGHT - font.wordWrapHeight(row.description(), textWidth)) / 2;
            extract.drawString(font, row.label(), inner.left(), textTop, TEXT_COLOR);
            extract.drawWordWrap(font, row.description(), inner.left(), textTop + LABEL_HEIGHT, textWidth, TEXT_MUTED_COLOR);
            y += rowHeight + ROW_SPACING;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }
}
