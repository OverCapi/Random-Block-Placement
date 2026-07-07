package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstant.*;

// GUI screen for selecting blocks and configuring their weights for random placement
public class BlockSelectionScreen extends Screen {
	// State and renderer separated for cleaner architecture
	private BlockSelectionScreenState blockSelectionScreenState = new BlockSelectionScreenState();
	private BlockSelectionScreenRenderer blockSelectionScreenRenderer = new BlockSelectionScreenRenderer();

	// Container position (centered on screen)
	private int leftPos;
	private int topPos;

	// Weight editing UI components (TODO: extract into dedicated editor component)
	private EditBox weightInput;
	private Identifier editingSlot;
	private boolean editingNewBlock;
	private Component weightLabel;

	public BlockSelectionScreen() {
		super(Component.literal("Random Block Placer"));
	}

	// Builds the toggle button text showing current ENABLE/DISABLE status
	private Component makeToggleText() {
		return Component.literal(
				"STATUS : " + (blockSelectionScreenState.isRndPlacementEnabled() ? "§aENABLE" : "§cDISABLE")
		);
	}

	// Toggles the working placement status and updates the button label
	private void toggleWorkingStatus(Button button) {
		blockSelectionScreenState.toggleRndPlacement();
		button.setMessage(makeToggleText());
	}

	// Creates the enable/disable toggle button above the container
	private void initWorkingStatusButton() {
		addRenderableWidget(Button.builder(
				makeToggleText(),
				this::toggleWorkingStatus
		).pos(leftPos + IMAGE_W / 2 - 80, topPos - 28).size(160, 20).build());
	}

	// Persists the working state to config and closes the screen
	private void save(Button button) {
		BlockPlacer blockPlacer = BlockPlacer.INSTANCE;
		BlockPlacerConfig blockPlacerConfig = blockPlacer.getBlockPlacerConfig();

		blockPlacerConfig.resetSelectedBlocks();
		blockPlacerConfig.setSelectedBlocks(blockSelectionScreenState.getWorkingWeights());
		if (blockSelectionScreenState.isRndPlacementEnabled()) {
			blockPlacer.enable();
		} else {
			blockPlacer.disable();
		}
		blockPlacerConfig.save();

		onClose();
	}

	// Creates the Save button below the container
	private void initSaveButton() {
		addRenderableWidget(Button.builder(
				Component.literal("Save"),
				this::save
		).pos(leftPos + IMAGE_W / 2 - 80, topPos + IMAGE_H + 8).size(70, 20).build());
	}

	// Resets the working state (clears selection, disables placement)
	private void reset(Button button) {
		blockSelectionScreenState.reset();
	}

	// Creates the Reset button below the container
	private void initResetButton() {
		addRenderableWidget(Button.builder(
				Component.literal("Reset"),
				this::reset
		).pos(leftPos + IMAGE_W / 2 + 10, topPos + IMAGE_H + 8).size(60, 20).build());
	}

	// Initializes the weight input field (hidden by default, shown on Shift+click)
	private void initWeightInput() {
		weightInput = new EditBox(
				font,
				leftPos + SLOT_X,
				topPos + IMAGE_H - 4,
				60,
				16,
				Component.literal("Weight")
		);
		weightInput.setMaxLength(4);
		weightInput.setValue("100");
		weightInput.setVisible(false);
		weightInput.setFocused(false);
		addRenderableWidget(weightInput);

		weightLabel = Component.literal("");
		editingSlot = null;
	}

	@Override
	protected void init() {
		super.init();

		// Load working state from the current config
		blockSelectionScreenState.init();

		// Center the container on screen
		leftPos = (width - IMAGE_W) / 2;
		topPos = (height - IMAGE_H) / 2;

		// Build UI components
		initWorkingStatusButton();
		initSaveButton();
		initResetButton();
		initWeightInput();

		// Initialize the custom renderer
		blockSelectionScreenRenderer.init(leftPos, topPos, font, minecraft);
	}

	@Override
	public void onClose() {
		blockSelectionScreenState.setOpen(false);
		super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
		super.extractRenderState(extract, mx, my, delta);
		// Delegate rendering to the dedicated renderer
		blockSelectionScreenRenderer.setCurrentState(blockSelectionScreenState);
		blockSelectionScreenRenderer.render(extract, mx, my, delta);

		// Draw the weight label when editing is active
		if (weightInput.isVisible() && editingSlot != null) {
			extract.text(font, weightLabel, leftPos + SLOT_X, topPos + IMAGE_H - 4 - 10, 0xCCCCCC);
		}
	}

	// Opens the weight editor for a given block slot
	private void startEditingWeight(Identifier id, ItemStack stack) {
		editingSlot = id;
		editingNewBlock = !blockSelectionScreenState.getWorkingWeights().containsKey(id);
		int val = editingNewBlock ? BlockPlacerConfig.DEFAULT_WEIGHT : blockSelectionScreenState.getWorkingWeights().get(id);
		weightInput.setValue(String.valueOf(val));
		weightInput.setVisible(true);
		weightInput.setFocused(true);
		weightInput.setCursorPosition(weightInput.getValue().length());
		setFocused(weightInput);
		String name = stack.getHoverName().getString();
		weightLabel = Component.literal("§7Weights for " + name + ":");
	}

	// Closes the weight editor without saving
	private void stopEditingWeight() {
		if (editingSlot != null && editingNewBlock) {
			editingNewBlock = false;
		}
		weightInput.setVisible(false);
		weightInput.setFocused(false);
		editingSlot = null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
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

		double mx = event.x();
		double my = event.y();

		// Calculate which inventory slot was clicked
		int col = (int) ((mx - leftPos - SLOT_X) / SLOT_SIZE);
		int row;
		if (my >= topPos + HOTBAR_Y && my < topPos + HOTBAR_Y + SLOT_SIZE) {
			row = 3; // Hotbar row
		} else {
			row = (int) ((my - topPos - MAIN_Y) / SLOT_SIZE);
		}

		// Validate click is within inventory bounds
		if (!(col >= 0 && col < INVENTORY_COL && row >= 0 && row < INVENTORY_ROW)) {
			return super.mouseClicked(event, consumed);
		}

		// Map grid position to inventory slot index
		int slotIndex = (row == 3) ? col : 9 + row * INVENTORY_COL + col;
		ItemStack stack = player.getInventory().getItem(slotIndex);

		// Only block items can be selected
		if (!(stack.getItem() instanceof BlockItem)) {
			return super.mouseClicked(event, consumed);
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

		// Shift+click opens the weight editor; regular click toggles selection
		if (event.hasShiftDown()) {
			startEditingWeight(id, stack);
			return true;
		}

		// Toggle block selection (add with default weight or remove)
		if (blockSelectionScreenState.getWorkingWeights().containsKey(id)) {
			blockSelectionScreenState.getWorkingWeights().remove(id);
		} else {
			blockSelectionScreenState.getWorkingWeights().put(id, BlockPlacerConfig.DEFAULT_WEIGHT);
		}
		if (editingSlot == id) {
			stopEditingWeight();
		}
		return true;
	}

	// Parses and applies the weight from the input field, then closes the editor
	private void confirmWeight() {
		if (editingSlot == null) return;
		try {
			int w = Integer.parseInt(weightInput.getValue());
			if (w <= 0) {
				blockSelectionScreenState.getWorkingWeights().remove(editingSlot);
			} else {
				blockSelectionScreenState.getWorkingWeights().put(editingSlot, w);
			}
		} catch (NumberFormatException e) {
			// Invalid input — ignore silently
		}
		stopEditingWeight();
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		// Handle Enter (confirm) and Escape (cancel) when weight editor is open
		if (weightInput.isVisible()) {
			if (event.isEscape()) {
				stopEditingWeight();
				return true;
			}
			if (event.isConfirmation()) {
				confirmWeight();
				return true;
			}
		}
		return super.keyPressed(event);
	}
}
