package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.screen.tab.SidebarTab;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Picks the preset put on one quick wheel slot; saves right away and returns to the game
public class QuickPresetPickerScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 240;
    private static final int PANEL_MAX_HEIGHT = 220;

    private final int slot;
    private List<String> presets = List.of();
    private PresetList list;

    private ScreenRectangle header;
    private ScreenRectangle panel;

    public QuickPresetPickerScreen(int slot) {
        super(Component.translatable("screen.rnd-block-placer.wheel_picker", slot + 1));
        this.slot = slot;
    }

    @Override
    protected void init() {
        presets = List.copyOf(BlockPlacerConfig.INSTANCE.getPresets().keySet());

        int panelWidth = Math.min(PANEL_MAX_WIDTH, width - 2 * SCREEN_MARGIN);
        int panelHeight = Math.min(PANEL_MAX_HEIGHT, height - 2 * SCREEN_MARGIN - HEADER_HEIGHT - FOOTER_HEIGHT - 2 * SECTION_GAP);
        int totalHeight = HEADER_HEIGHT + SECTION_GAP + panelHeight + SECTION_GAP + FOOTER_HEIGHT;
        int left = (width - panelWidth) / 2;
        int top = Math.max(0, (height - totalHeight) / 2);
        header = new ScreenRectangle(left, top, panelWidth, HEADER_HEIGHT);
        panel = new ScreenRectangle(left, header.bottom() + SECTION_GAP, panelWidth, panelHeight);
        ScreenRectangle footer = new ScreenRectangle(left, panel.bottom() + SECTION_GAP, panelWidth, FOOTER_HEIGHT);

        ScreenRectangle inner = SelectionLayout.inner(panel);
        if (list == null) {
            list = new PresetList(font);
        }
        int listTop = inner.top() + PANEL_TITLE_HEIGHT;
        list.init(new ScreenRectangle(inner.left(), listTop, inner.width(), inner.bottom() - listTop), this::addRenderableWidget);

        int buttonWidth = (footer.width() - SIDEBAR_SPACING) / 2;
        Button clear = addRenderableWidget(Button.builder(Component.translatable("button.rnd-block-placer.wheel.clear"), button -> assign(null))
                .bounds(footer.left(), footer.top(), buttonWidth, FOOTER_HEIGHT)
                .build());
        clear.active = BlockPlacerConfig.INSTANCE.getQuickPreset(slot) != null;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(footer.right() - buttonWidth, footer.top(), buttonWidth, FOOTER_HEIGHT)
                .build());
    }

    // Puts the preset on the slot (or empties it when null), saves, and returns to the game
    private void assign(String name) {
        BlockPlacerConfig.INSTANCE.setQuickPreset(slot, name);
        BlockPlacerConfig.INSTANCE.save();
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractBackground(extract, mx, my, delta);
        PanelStyle.drawPanel(extract, panel);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractRenderState(extract, mx, my, delta);
        PanelStyle.drawHeaderTitle(extract, font, header, Component.translatable("screen.rnd-block-placer.title"));
        PanelStyle.drawPanelTitle(extract, font, panel, title);
        list.render(extract, mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || list.mouseClicked(event.x(), event.y());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return list.mouseScrolled(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // Scrollable list of saved presets; the one already on the slot is highlighted
    private class PresetList extends SidebarTab {
        PresetList(Font font) {
            super(font);
        }

        @Override
        public Component title() {
            return QuickPresetPickerScreen.this.title;
        }

        @Override
        public void init(ScreenRectangle area, Consumer<AbstractWidget> addWidget) {
            setListArea(area);
        }

        @Override
        public void updateWidgets() {}

        @Override
        protected int rowCount() {
            return presets.size();
        }

        @Override
        public void render(GuiGraphicsExtractor extract, int mx, int my) {
            ScreenRectangle area = listArea();
            if (presets.isEmpty()) {
                extract.textWithWordWrap(font, Component.translatable("label.rnd-block-placer.wheel.no_presets"),
                        area.left(), area.top() + 2, area.width(), TEXT_MUTED_COLOR);
            }
            String current = BlockPlacerConfig.INSTANCE.getQuickPreset(slot);
            int hovered = rowAt(mx, my);
            int rowWidth = rowWidth();
            for (int i = firstVisibleRow(); i < endVisibleRow(); i++) {
                String name = presets.get(i);
                int y = rowY(i);
                int x = area.left();
                if (name.equals(current)) {
                    extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_SELECTED_COLOR);
                } else if (i == hovered) {
                    extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_HOVER_COLOR);
                }
                int textY = y + (SIDEBAR_ROW_HEIGHT - font.lineHeight) / 2 + 1;
                extract.text(font, truncate(name, rowWidth - 8), x + 4, textY, TEXT_COLOR);
            }
            renderScrollbar(extract);
        }

        @Override
        public boolean mouseClicked(double mx, double my) {
            int index = rowAt(mx, my);
            if (index < 0) {
                return false;
            }
            assign(presets.get(index));
            return true;
        }
    }
}
