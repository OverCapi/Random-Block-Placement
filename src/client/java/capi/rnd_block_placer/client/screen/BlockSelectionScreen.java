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

public class BlockSelectionScreen extends Screen {
	private BlockSelectionScreenState blockSelectionScreenState = new BlockSelectionScreenState();
	private BlockSelectionScreenRenderer blockSelectionScreenRenderer = new BlockSelectionScreenRenderer();

	private int leftPos;
	private int topPos;

	// TODO Abstraction
	private EditBox weightInput;
	private Identifier editingSlot;
	private boolean editingNewBlock;
	private Component weightLabel;

	public BlockSelectionScreen() {
		super(Component.literal("Random Block Placer"));
	}

	private Component makeToggleText() {
		return Component.literal(
				"STATUS : " + (blockSelectionScreenState.isRndPlacementEnabled() ? "§aENABLE" : "§cDISABLE")
		);
	}

	private void toggleWorkingStatus(Button button) {
		blockSelectionScreenState.toggleRndPlacement();
		button.setMessage(makeToggleText());
	}

	private void initWorkingStatusButton() {
		addRenderableWidget(Button.builder(
				makeToggleText(),
				this::toggleWorkingStatus
		).pos(leftPos + IMAGE_W / 2 - 80, topPos - 28).size(160, 20).build());
	}

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

	private void initSaveButton() {
		addRenderableWidget(Button.builder(
				Component.literal("Save"),
				this::save
		).pos(leftPos + IMAGE_W / 2 - 80, topPos + IMAGE_H + 8).size(70, 20).build());
	}

	private void reset(Button button) {
		blockSelectionScreenState.reset();
	}

	private void initResetButton() {
		addRenderableWidget(Button.builder(
				Component.literal("Reset"),
				this::reset
		).pos(leftPos + IMAGE_W / 2 + 10, topPos + IMAGE_H + 8).size(60, 20).build());
	}

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

		blockSelectionScreenState.init();

		leftPos = (width - IMAGE_W) / 2;
		topPos = (height - IMAGE_H) / 2;


		initWorkingStatusButton();
		initSaveButton();
		initResetButton();
		initWeightInput();

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
		blockSelectionScreenRenderer.setCurrentState(blockSelectionScreenState);
		blockSelectionScreenRenderer.render(extract, mx, my, delta);

		if (weightInput.isVisible() && editingSlot != null) {
			extract.text(font, weightLabel, leftPos + SLOT_X, topPos + IMAGE_H - 4 - 10, 0xCCCCCC);
		}
	}

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
		if (event.buttonInfo().button() != 0) {
			return super.mouseClicked(event, consumed);
		}

		LocalPlayer player = minecraft.player;
		if (player == null) {
			return super.mouseClicked(event, consumed);
		}

		double mx = event.x();
		double my = event.y();

		int col = (int) ((mx - leftPos - SLOT_X) / SLOT_SIZE);
		int row;
		if (my >= topPos + HOTBAR_Y && my < topPos + HOTBAR_Y + SLOT_SIZE) {
			row = 3;
		} else {
			row = (int) ((my - topPos - MAIN_Y) / SLOT_SIZE);
		}

		if (!(col >= 0 && col < INVENTORY_COL && row >= 0 && row < INVENTORY_ROW)) {
			return super.mouseClicked(event, consumed);
		}

		int slotIndex = (row == 3) ? col : 9 + row * INVENTORY_COL + col;
		ItemStack stack = player.getInventory().getItem(slotIndex);

		if (!(stack.getItem() instanceof BlockItem)) {
			return super.mouseClicked(event, consumed);
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (event.hasShiftDown()) {
			startEditingWeight(id, stack);
			return true;
		}

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
		}
		stopEditingWeight();
	}


	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
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
