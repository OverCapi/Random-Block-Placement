package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.SaveMode;
import capi.rnd_block_placer.client.keymapping.KeyBindings;
import capi.rnd_block_placer.client.screen.tab.PresetsTab;
import capi.rnd_block_placer.client.screen.tab.SelectionTab;
import capi.rnd_block_placer.client.screen.tab.SidebarTab;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Block selection screen: Selection panel, clickable inventory and Presets panel side by side
// (on narrow windows the two panels become tabs to the right of the inventory)
public class BlockSelectionScreen extends Screen {
    private static final int SETTINGS_BUTTON_SIZE = 20;

    private enum Tab { SELECTION, PRESETS }

    // Working state lives as long as the screen, so resizing (which re-runs init) keeps unsaved changes
    private final BlockSelectionScreenState state = new BlockSelectionScreenState();
    private final InventoryGrid grid;
    private final SelectionTab selectionTab;
    private final PresetsTab presetsTab;
    private Tab currentTab = Tab.SELECTION;

    private SelectionLayout layout;
    private CycleButton<Boolean> placementToggle;
    private Button selectionTabButton;
    private Button presetsTabButton;
    // Footer buttons of the manual save mode (null in on-close mode)
    private Button cancelButton;
    private Button applyButton;

    public BlockSelectionScreen() {
        super(Component.translatable("screen.rnd-block-placer.title"));
        grid = new InventoryGrid(state, font);
        selectionTab = new SelectionTab(font, state, this::setFocused, this::openShare);
        presetsTab = new PresetsTab(font, state, this::openShare);
    }

    // Opens the player picker; the working state survives because this screen instance is kept as parent
    private void openShare(String name, Map<Identifier, Integer> weights) {
        minecraft.setScreenAndShow(new SharePlayersScreen(this, name, weights));
    }

    private SidebarTab activeTab() {
        return currentTab == Tab.SELECTION ? selectionTab : presetsTab;
    }

    // Panels currently on screen: both when split, only the active tab otherwise
    private List<SidebarTab> visibleTabs() {
        return layout.split() ? List.of(selectionTab, presetsTab) : List.of(activeTab());
    }

    // Switches the sidebar tab, rebuilding the widgets of the newly shown tab
    private void selectTab(Tab tab) {
        if (currentTab != tab) {
            currentTab = tab;
            rebuildWidgets();
        }
    }

    private boolean isManualSave() {
        return BlockPlacerConfig.INSTANCE.getSaveMode() == SaveMode.MANUAL;
    }

    @Override
    protected void init() {
        setFocused(null);
        layout = SelectionLayout.compute(width, height);
        initHeader();
        initSidebar();
        initFooter();
        updateWidgets();
    }

    // Title on the left; placement toggle and settings button on the right
    private void initHeader() {
        ScreenRectangle header = layout.header();
        int settingsX = header.right() - SETTINGS_BUTTON_SIZE;
        addRenderableWidget(Button.builder(Component.literal("⚙"),
                        button -> minecraft.setScreenAndShow(new SettingsScreen(this)))
                .bounds(settingsX, header.top(), SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("button.rnd-block-placer.settings")))
                .build());

        placementToggle = addRenderableWidget(CycleButton.onOffBuilder(BlockPlacer.INSTANCE.isEnabled())
                .withTooltip(value -> Tooltip.create(Component.translatable("tooltip.rnd-block-placer.placement")))
                .create(settingsX - SIDEBAR_SPACING - HEADER_TOGGLE_WIDTH, header.top(), HEADER_TOGGLE_WIDTH, HEADER_HEIGHT,
                        Component.translatable("button.rnd-block-placer.placement"),
                        (button, enabled) -> {
                            if (enabled) {
                                BlockPlacer.INSTANCE.enable();
                            } else {
                                BlockPlacer.INSTANCE.disable();
                            }
                        }));
    }

    // Split: Selection panel on the left, Presets panel on the right, each under a title.
    // Narrow: a single panel with tab buttons on top and the active tab's content below.
    private void initSidebar() {
        selectionTabButton = null;
        presetsTabButton = null;
        if (layout.split()) {
            selectionTab.init(belowTitle(layout.leftPanel()), widget -> addRenderableWidget(widget));
            presetsTab.init(belowTitle(layout.rightPanel()), widget -> addRenderableWidget(widget));
            return;
        }

        ScreenRectangle inner = SelectionLayout.inner(layout.rightPanel());
        int halfWidth = (inner.width() - 2) / 2;
        selectionTabButton = addRenderableWidget(Button.builder(selectionTab.title(), button -> selectTab(Tab.SELECTION))
                .bounds(inner.left(), inner.top(), halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .build());
        presetsTabButton = addRenderableWidget(Button.builder(presetsTab.title(), button -> selectTab(Tab.PRESETS))
                .bounds(inner.right() - halfWidth, inner.top(), halfWidth, SIDEBAR_BUTTON_HEIGHT)
                .build());

        int contentTop = inner.top() + SIDEBAR_BUTTON_HEIGHT + SIDEBAR_SPACING + 2;
        ScreenRectangle content = new ScreenRectangle(inner.left(), contentTop, inner.width(), inner.bottom() - contentTop);
        activeTab().init(content, widget -> addRenderableWidget(widget));
    }

    // A panel's inner area below its title line
    private static ScreenRectangle belowTitle(ScreenRectangle panel) {
        ScreenRectangle inner = SelectionLayout.inner(panel);
        return new ScreenRectangle(inner.left(), inner.top() + PANEL_TITLE_HEIGHT,
                inner.width(), inner.height() - PANEL_TITLE_HEIGHT);
    }

    // "Done" in on-close mode; "Cancel"/"Close" and "Apply" in manual mode
    private void initFooter() {
        ScreenRectangle footer = layout.footer();
        cancelButton = null;
        applyButton = null;
        if (!isManualSave()) {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(footer.left() + (footer.width() - FOOTER_BUTTON_WIDTH) / 2, footer.top(), FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.done")))
                    .build());
            return;
        }
        applyButton = addRenderableWidget(Button.builder(Component.translatable("button.rnd-block-placer.apply"), button -> state.apply())
                .bounds(footer.right() - FOOTER_BUTTON_WIDTH, footer.top(), FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tooltip.rnd-block-placer.apply")))
                .build());
        cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(footer.right() - 2 * FOOTER_BUTTON_WIDTH - SIDEBAR_SPACING, footer.top(), FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT)
                .build());
    }

    // Refreshes labels and enabled state that depend on the working state
    private void updateWidgets() {
        if (selectionTabButton != null) {
            selectionTabButton.setMessage(selectionTab.title());
            selectionTabButton.active = currentTab != Tab.SELECTION;
            presetsTabButton.setMessage(presetsTab.title());
            presetsTabButton.active = currentTab != Tab.PRESETS;
        }

        if (placementToggle.getValue() != BlockPlacer.INSTANCE.isEnabled()) {
            placementToggle.setValue(BlockPlacer.INSTANCE.isEnabled());
        }

        if (applyButton != null) {
            boolean dirty = state.isDirty();
            applyButton.active = dirty;
            cancelButton.setMessage(dirty ? CommonComponents.GUI_CANCEL : Component.translatable("button.rnd-block-placer.close"));
            cancelButton.setTooltip(dirty ? Tooltip.create(Component.translatable("tooltip.rnd-block-placer.cancel")) : null);
        }

        for (SidebarTab tab : visibleTabs()) {
            tab.updateWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractBackground(extract, mx, my, delta);
        PanelStyle.drawPanel(extract, layout.inventoryPanel());
        PanelStyle.drawPanel(extract, layout.rightPanel());
        if (layout.split()) {
            PanelStyle.drawPanel(extract, layout.leftPanel());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        updateWidgets();
        super.extractRenderState(extract, mx, my, delta);

        PanelStyle.drawHeaderTitle(extract, font, layout.header(), title);

        LocalPlayer player = minecraft.player;
        if (player != null) {
            grid.render(extract, layout, player, mx, my);
        }
        if (layout.split()) {
            PanelStyle.drawPanelTitle(extract, font, layout.leftPanel(), selectionTab.title());
            PanelStyle.drawPanelTitle(extract, font, layout.rightPanel(), presetsTab.title());
        }
        for (SidebarTab tab : visibleTabs()) {
            tab.render(extract, mx, my);
        }

        if (applyButton != null && state.isDirty()) {
            ScreenRectangle footer = layout.footer();
            extract.text(font, Component.translatable("label.rnd-block-placer.unsaved"),
                    footer.left() + 2, footer.top() + (FOOTER_HEIGHT - font.lineHeight) / 2 + 1, TEXT_WARNING_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        presetsTab.onAnyClick(mx, my);

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        // Same toggle when the screen key is bound to a mouse button
        if (KeyBindings.INSTANCE.getSelectionScreenKey().matchesMouse(event)) {
            onClose();
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        for (SidebarTab tab : visibleTabs()) {
            if (tab.mouseClicked(mx, my)) {
                return true;
            }
        }
        return clickSlot(mx, my, event.hasShiftDown());
    }

    // Click toggles a block in the selection; Shift+click opens its chance editor
    private boolean clickSlot(double mx, double my, boolean shift) {
        LocalPlayer player = minecraft.player;
        int slot = layout.slotAt(mx, my);
        if (player == null || slot < 0) {
            return false;
        }
        Identifier id = InventoryGrid.blockId(player.getInventory().getItem(slot));
        if (id == null) {
            return true;
        }

        if (shift) {
            if (!layout.split()) {
                selectTab(Tab.SELECTION);
            }
            selectionTab.startEditing(id);
        } else if (state.containsWeight(id)) {
            state.removeWeight(id);
            selectionTab.stopEditing(id);
        } else {
            state.addBlock(id);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // The key that opened the screen also closes it, unless it is being typed into a text field
        if (!(getFocused() instanceof EditBox) && KeyBindings.INSTANCE.getSelectionScreenKey().matches(event)) {
            onClose();
            return true;
        }
        for (SidebarTab tab : visibleTabs()) {
            if (tab.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (SidebarTab tab : visibleTabs()) {
            if (tab.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // In on-close mode closing applies the changes; in manual mode it discards them
    @Override
    public void onClose() {
        if (!isManualSave() && state.isDirty()) {
            state.apply();
        }
        super.onClose();
    }
}
