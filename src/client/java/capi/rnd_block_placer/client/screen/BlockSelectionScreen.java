package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.RandomBlockPlacer;
import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;

import capi.rnd_block_placer.client.screen.widget.CustomButton;
import capi.rnd_block_placer.client.screen.widget.Texture;
import net.minecraft.client.color.item.CustomModelDataSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstant.*;

// GUI screen for selecting blocks and configuring their weights for random placement
public class BlockSelectionScreen extends Screen {
	// State and renderer separated for cleaner architecture
	private final BlockSelectionScreenState blockSelectionScreenState = new BlockSelectionScreenState();
	private final BlockSelectionScreenRenderer blockSelectionScreenRenderer = new BlockSelectionScreenRenderer();

	// Container position (centered on screen)
	private int leftPos;
	private int topPos;

	private CustomButton resetButton;
	private CustomButton saveButton;

	// Weight editing UI components (TODO: extract into dedicated editor component)
	private EditBox weightInput;
	private Identifier editingSlot;
	private boolean editingNewBlock;
	private Component weightLabel;

	public BlockSelectionScreen() {
		super(Component.literal("Random Block Placer"));
	}

	public CustomButton getSaveButton() {
		return saveButton;
	}

	public CustomButton getResetButton() {
		return resetButton;
	}

	// Persists the working state to config and closes the screen
	private void save() {
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


	// Resets the working state (clears selection, disables placement)
	private void reset() {
		blockSelectionScreenState.reset();
	}

	// Initializes the weight input field (hidden by default, shown on Shift+click)
	private void initWeightInput() {
		weightInput = new EditBox(
				font,
				leftPos + SLOT_X,
				topPos + DISPLAY_IMAGE_H - 4,
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
		leftPos = (width - DISPLAY_IMAGE_W) / 2;
		topPos = (height - DISPLAY_IMAGE_H) / 2;

		resetButton = new CustomButton(
				leftPos + 200, topPos + 20,
				20, 20,
				new Texture(
						Identifier.fromNamespaceAndPath(RandomBlockPlacer.MOD_ID, "textures/gui/reset_button_close.png"),
						32,
						32
				),
				this::reset
		);
		saveButton = new CustomButton(
				leftPos + 200, topPos + 80,
				20, 20,
				new Texture(
						Identifier.fromNamespaceAndPath(RandomBlockPlacer.MOD_ID, "textures/gui/save_button.png"),
						32,
						32
				),
				this::save
		);

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
		blockSelectionScreenRenderer.setSelectionScreen(this);
		blockSelectionScreenRenderer.setCurrentState(blockSelectionScreenState);
		blockSelectionScreenRenderer.render(extract, mx, my, delta);

		// Draw the weight label when editing is active
		if (weightInput.isVisible() && editingSlot != null) {
			extract.text(font, weightLabel, leftPos + SLOT_X, topPos + DISPLAY_IMAGE_H - 4 - 10, 0xCCCCCC);
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

		if (saveButton.isHover(mx, my)) {
			player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 2f, 0.7f);
			saveButton.onClick();
			return super.mouseClicked(event, consumed);
		} else if (resetButton.isHover(mx, my)) {
			player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 2f, 0.7f);
			resetButton.onClick();
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
