package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.SaveMode;
import capi.rnd_block_placer.client.screen.tab.PresetsTab;
import capi.rnd_block_placer.client.screen.tab.SelectionTab;
import capi.rnd_block_placer.client.screen.tab.SidebarTab;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Block selection screen: clickable inventory on the left, Selection/Presets tabs on the right
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
        selectionTab = new SelectionTab(font, state, this::setFocused);
        presetsTab = new PresetsTab(font, state);
    }

    private SidebarTab activeTab() {
        return currentTab == Tab.SELECTION ? selectionTab : presetsTab;
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

    // Tab buttons on top, then the active tab's content below
    private void initSidebar() {
        ScreenRectangle inner = layout.sidebarInner();
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
        selectionTabButton.setMessage(selectionTab.title());
        selectionTabButton.active = currentTab != Tab.SELECTION;
        presetsTabButton.setMessage(presetsTab.title());
        presetsTabButton.active = currentTab != Tab.PRESETS;

        if (placementToggle.getValue() != BlockPlacer.INSTANCE.isEnabled()) {
            placementToggle.setValue(BlockPlacer.INSTANCE.isEnabled());
        }

        if (applyButton != null) {
            boolean dirty = state.isDirty();
            applyButton.active = dirty;
            cancelButton.setMessage(dirty ? CommonComponents.GUI_CANCEL : Component.translatable("button.rnd-block-placer.close"));
            cancelButton.setTooltip(dirty ? Tooltip.create(Component.translatable("tooltip.rnd-block-placer.cancel")) : null);
        }

        activeTab().updateWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractBackground(extract, mx, my, delta);
        drawPanel(extract, layout.inventoryPanel());
        drawPanel(extract, layout.sidebar());
    }

    private static void drawPanel(GuiGraphicsExtractor extract, ScreenRectangle panel) {
        extract.fill(panel.left(), panel.top(), panel.right(), panel.bottom(), PANEL_COLOR);
        extract.outline(panel.left(), panel.top(), panel.width(), panel.height(), PANEL_BORDER_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        updateWidgets();
        super.extractRenderState(extract, mx, my, delta);

        ScreenRectangle header = layout.header();
        extract.text(font, title, header.left() + 2, header.top() + (HEADER_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR);

        LocalPlayer player = minecraft.player;
        if (player != null) {
            grid.render(extract, layout, player, mx, my);
        }
        activeTab().render(extract, mx, my);

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
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (activeTab().mouseClicked(mx, my)) {
            return true;
        }
        return clickSlot(mx, my, event.hasShiftDown());
    }

    // Click toggles a block in the selection; Shift+click opens its weight editor
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
            selectTab(Tab.SELECTION);
            selectionTab.startEditing(id);
        } else if (state.containsWeight(id)) {
            state.removeWeight(id);
            selectionTab.stopEditing(id);
        } else {
            state.setWeight(id, BlockPlacerConfig.DEFAULT_WEIGHT);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (activeTab().keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
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
