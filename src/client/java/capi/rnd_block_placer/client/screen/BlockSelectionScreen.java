package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.screen.widget.CustomButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// GUI screen for selecting blocks and configuring their weights for random placement
public class BlockSelectionScreen extends Screen {
    // State and rendering separated for cleaner architecture
    private final BlockSelectionScreenState blockSelectionScreenState = new BlockSelectionScreenState();
    private final BlockSelectionScreenRenderer blockSelectionScreenRenderer = new BlockSelectionScreenRenderer();

    private WeightEditor weightEditor;
    private PresetPanel presetPanel;

    // Container position (centered on screen)
    private int leftPos;
    private int topPos;

    private CustomButton resetButton;
    private CustomButton saveButton;

    public BlockSelectionScreen() {
        super(Component.translatable("screen.rnd-block-placer.title"));
    }

    // Persists the working state to config and closes the screen
    private void save() {
        BlockPlacer blockPlacer = BlockPlacer.INSTANCE;
        BlockPlacerConfig blockPlacerConfig = BlockPlacerConfig.INSTANCE;

        blockPlacerConfig.setSelectedBlocks(blockSelectionScreenState.copyWeights());
        if (blockSelectionScreenState.isRndPlacementEnabled()) {
            blockPlacer.enable();
        } else {
            blockPlacer.disable();
        }
        blockPlacerConfig.save();

        onClose();
    }

    // Resets the working state (clears selection, disables placement)
    private void reset() {
        blockSelectionScreenState.reset();
    }

    @Override
    protected void init() {
        super.init();

        // Load working state from the current config
        blockSelectionScreenState.init();

        // Center the container on screen
        leftPos = (width - DISPLAY_IMAGE_W) / 2;
        topPos = (height - DISPLAY_IMAGE_H) / 2;

        int buttonSize = 24;
        int buttonY = topPos - buttonSize - buttonSize / 4;
        resetButton = new CustomButton(
                leftPos + DISPLAY_IMAGE_W / 4, buttonY,
                buttonSize, buttonSize,
                RESET_BUTTON_CLOSE,
                this::reset
        );
        saveButton = new CustomButton(
                (leftPos + 3 * DISPLAY_IMAGE_W / 4) - 20, buttonY,
                buttonSize, buttonSize,
                SAVE_BUTTON,
                this::save
        );

        weightEditor = new WeightEditor(
                font,
                blockSelectionScreenState,
                leftPos + SLOT_X,
                topPos + DISPLAY_IMAGE_H - WEIGHT_INPUT_Y_OFFSET
        );
        addRenderableWidget(weightEditor.getInput());

        presetPanel = new PresetPanel(font, minecraft, blockSelectionScreenState, leftPos, topPos);
        addRenderableWidget(presetPanel.getInput());

        // Initialize the custom renderer
        blockSelectionScreenRenderer.init(leftPos, topPos, font, minecraft);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractRenderState(extract, mx, my, delta);

        // Change texture of the reset button on hover
        resetButton.setTexture(resetButton.isHover(mx, my) ? RESET_BUTTON_OPEN : RESET_BUTTON_CLOSE);

        // Delegate rendering to the dedicated renderer
        blockSelectionScreenRenderer.render(extract, blockSelectionScreenState, saveButton, resetButton, mx, my);

        // Draw the weight label when editing is active
        weightEditor.render(extract, leftPos, topPos);

        // Draw the preset management panel
        presetPanel.render(extract, mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mx = event.x();
        double my = event.y();

        if (consumed) {
            return super.mouseClicked(event, consumed);
        }
        // Only handle left-click
        if (event.buttonInfo().button() != 0) {
            return super.mouseClicked(event, consumed);
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return super.mouseClicked(event, consumed);
        }

        if (saveButton.isHover(mx, my)) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), BUTTON_SOUND_VOLUME, BUTTON_SOUND_PITCH);
            saveButton.onClick();
            return super.mouseClicked(event, consumed);
        } else if (resetButton.isHover(mx, my)) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), BUTTON_SOUND_VOLUME, BUTTON_SOUND_PITCH);
            resetButton.onClick();
            return super.mouseClicked(event, consumed);
        }

        // Delegate clicks on the preset panel to it
        if (presetPanel.mouseClicked(mx, my)) {
            return super.mouseClicked(event, consumed);
        }

        // Calculate which inventory slot was clicked
        int col = (int) ((mx - leftPos - SLOT_X) / (SLOT_SIZE + SLOT_PADDING_X));
        int row;
        if (my >= topPos + HOTBAR_Y && my < topPos + HOTBAR_Y + SLOT_SIZE) {
            row = 3; // Hotbar row
        } else {
            row = (int) ((my - topPos - MAIN_Y) / (SLOT_SIZE + SLOT_PADDING_Y));
        }

        // Validate click is within inventory bounds
        if (!(col >= 0 && col < INVENTORY_COL && row >= 0 && row < INVENTORY_ROW)) {
            return super.mouseClicked(event, consumed);
        }

        ItemStack stack = player.getInventory().getItem(slotIndex(row, col));

        // Only block items can be selected
        if (!(stack.getItem() instanceof BlockItem)) {
            return super.mouseClicked(event, consumed);
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // Shift+click opens the weight editor; regular click toggles selection
        if (event.hasShiftDown()) {
            weightEditor.startEditing(id, stack);
            setFocused(weightEditor.getInput());
            return true;
        }

        // Toggle block selection (add with default weight or remove)
        if (blockSelectionScreenState.containsWeight(id)) {
            blockSelectionScreenState.removeWeight(id);
        } else {
            blockSelectionScreenState.setWeight(id, BlockPlacerConfig.DEFAULT_WEIGHT);
        }
        if (weightEditor.isEditing(id)) {
            weightEditor.stopEditing();
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        // Handle Enter (create preset) in the name input
        if (presetPanel.keyPressed(event)) {
            return true;
        }
        // Handle Enter (confirm) and Escape (cancel) when weight editor is open
        if (weightEditor.handleKey(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (presetPanel.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return blockSelectionScreenRenderer.mouseScrolled(mouseX, mouseY, verticalAmount, blockSelectionScreenState);
    }
}
