package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.screen.tab.SidebarTab;
import capi.rnd_block_placer.client.share.PresetShare;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Picks the online players a preset is sent to, then queues the share and returns to the parent screen
public class SharePlayersScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 240;
    private static final int PANEL_MAX_HEIGHT = 220;

    private final Screen parent;
    private final String presetName;
    private final Map<Identifier, Integer> weights;
    // Checked players, kept across resizes
    private final Set<String> checked = new HashSet<>();
    private List<String> players = List.of();
    private PlayerList list;

    private ScreenRectangle header;
    private ScreenRectangle panel;
    private Button selectAllButton;
    private Button sendButton;

    public SharePlayersScreen(Screen parent, String presetName, Map<Identifier, Integer> weights) {
        super(Component.translatable("screen.rnd-block-placer.share", presetName));
        this.parent = parent;
        this.presetName = presetName;
        this.weights = Map.copyOf(weights);
    }

    @Override
    protected void init() {
        players = PresetShare.otherPlayers();
        checked.retainAll(players);

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
            list = new PlayerList(font);
        }
        int chatY = inner.bottom() - SIDEBAR_BUTTON_HEIGHT;
        int listTop = inner.top() + PANEL_TITLE_HEIGHT;
        list.init(new ScreenRectangle(inner.left(), listTop, inner.width(), chatY - SIDEBAR_SPACING - listTop),
                widget -> addRenderableWidget(widget));
        int halfWidth = (inner.width() - 2) / 2;
        addRenderableWidget(Button.builder(Component.translatable("button.rnd-block-placer.share.send_chat"), button -> sendToChat())
                .bounds(inner.left(), chatY, halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.share.send_chat")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.rnd-block-placer.share.copy"), button -> copy())
                .bounds(inner.right() - halfWidth, chatY, halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.share.copy")))
                .build());

        int buttonWidth = (footer.width() - 2 * SIDEBAR_SPACING) / 3;
        selectAllButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleAll())
                .bounds(footer.left(), footer.top(), buttonWidth, FOOTER_HEIGHT)
                .build());
        sendButton = addRenderableWidget(Button.builder(Component.empty(), button -> send())
                .bounds(footer.left() + buttonWidth + SIDEBAR_SPACING, footer.top(), buttonWidth, FOOTER_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(footer.right() - buttonWidth, footer.top(), buttonWidth, FOOTER_HEIGHT)
                .build());
        updateWidgets();
    }

    private void updateWidgets() {
        boolean allChecked = !players.isEmpty() && checked.size() == players.size();
        selectAllButton.active = !players.isEmpty();
        selectAllButton.setMessage(Component.translatable(allChecked
                ? "button.rnd-block-placer.share.select_none"
                : "button.rnd-block-placer.share.select_all"));
        sendButton.active = !checked.isEmpty();
        sendButton.setMessage(Component.translatable("button.rnd-block-placer.share.send", checked.size()));
    }

    private void toggleAll() {
        if (checked.size() == players.size()) {
            checked.clear();
        } else {
            checked.addAll(players);
        }
    }

    // Sends to the checked players in list order
    private void send() {
        List<String> targets = players.stream().filter(checked::contains).toList();
        if (!targets.isEmpty()) {
            PresetShare.send(presetName, weights, targets);
            onClose();
        }
    }

    // Posts the preset in public chat, where every player with the mod can import it
    private void sendToChat() {
        PresetShare.send(presetName, weights, List.of());
        onClose();
    }

    // Copies the preset's code, to paste anywhere and import with the Presets tab's Import button
    private void copy() {
        PresetShare.copyToClipboard(presetName, weights);
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractBackground(extract, mx, my, delta);
        PanelStyle.drawPanel(extract, panel);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        updateWidgets();
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

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    // Scrollable list of online players with a checkbox each
    private class PlayerList extends SidebarTab {
        PlayerList(Font font) {
            super(font);
        }

        @Override
        public Component title() {
            return SharePlayersScreen.this.title;
        }

        @Override
        public void init(ScreenRectangle area, Consumer<AbstractWidget> addWidget) {
            setListArea(area);
        }

        @Override
        public void updateWidgets() {}

        @Override
        protected int rowCount() {
            return players.size();
        }

        @Override
        public void render(GuiGraphicsExtractor extract, int mx, int my) {
            ScreenRectangle area = listArea();
            if (players.isEmpty()) {
                extract.textWithWordWrap(font, Component.translatable("label.rnd-block-placer.share.no_players"),
                        area.left(), area.top() + 2, area.width(), TEXT_MUTED_COLOR);
            }
            int hovered = rowAt(mx, my);
            int rowWidth = rowWidth();
            for (int i = firstVisibleRow(); i < endVisibleRow(); i++) {
                String name = players.get(i);
                boolean on = checked.contains(name);
                int y = rowY(i);
                int x = area.left();
                if (on) {
                    extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_SELECTED_COLOR);
                } else if (i == hovered) {
                    extract.fill(x, y, x + rowWidth, y + SIDEBAR_ROW_HEIGHT, ROW_HOVER_COLOR);
                }
                int textY = y + (SIDEBAR_ROW_HEIGHT - font.lineHeight) / 2 + 1;
                extract.text(font, on ? "☑" : "☐", x + 4, textY, on ? TEXT_ACTIVE_COLOR : TEXT_MUTED_COLOR);
                extract.text(font, truncate(name, rowWidth - 20), x + 16, textY, TEXT_COLOR);
            }
            renderScrollbar(extract);
        }

        @Override
        public boolean mouseClicked(double mx, double my) {
            int index = rowAt(mx, my);
            if (index < 0) {
                return false;
            }
            String name = players.get(index);
            if (!checked.remove(name)) {
                checked.add(name);
            }
            return true;
        }
    }
}
