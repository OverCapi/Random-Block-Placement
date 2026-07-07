package capi.rnd_block_placer.client.mixin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

// Mixin that intercepts block placement to swap to a randomly selected block before placement
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	// Whether a swap is currently in progress
	@Unique
	private boolean rbp$swapped = false;

	// The player's original hotbar slot before the swap
	@Unique
	private int rbp$originalSlot;

	// Whether the swap involved an inventory slot (outside hotbar) via ContainerInput
	@Unique
	private boolean rbp$invSwapped = false;

	// The inventory slot that was swapped into the hotbar
	@Unique
	private int rbp$invSlot = -1;

	// Internal record representing an eligible slot with its stack and configured weight
	@Unique
	private record SlotEntry(int slot, ItemStack stack, int weight) {}

	// Scans the inventory for slots containing selected blocks and returns them with their weights
	@Unique
	private HashMap<Identifier, SlotEntry> rbp$getEligibleSlots(Inventory inventory) {
		HashMap<Identifier, SlotEntry> eligibleSlots = new HashMap<Identifier, SlotEntry>();

		BlockPlacerConfig blockPlacerConfig = BlockPlacer.INSTANCE.getBlockPlacerConfig();
		Set<Identifier> selectedBlocks = blockPlacerConfig.getSelectedBlocksKey();

		for (int slotIndex = 0; slotIndex < Inventory.INVENTORY_SIZE; ++slotIndex) {
			ItemStack stack = inventory.getItem(slotIndex);
			if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
				continue;
			}

			Identifier blockItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (selectedBlocks.contains(blockItemId)) {
				int selectedBlockWeight = blockPlacerConfig.getSelectedBlockWeight(blockItemId);
				eligibleSlots.put(blockItemId, new SlotEntry(slotIndex, stack, selectedBlockWeight));
			}

		}

		return eligibleSlots;
	}

	// Weighted random selection from eligible slots using the configured weights
	@Unique
	private SlotEntry rbp$getRandomSlotEntry(HashMap<Identifier, SlotEntry> eligibleSlots, RandomSource randomSource) {
		int totalWeight = 0;
		for (Identifier blockItemId : eligibleSlots.keySet()) {
			totalWeight += eligibleSlots.get(blockItemId).weight;
		}

		int randomInteger = randomSource.nextInt(totalWeight);
		int cumulativeWeight = 0;
		for (Identifier blockItemId : eligibleSlots.keySet()) {
			int eligibleSlotWeight = eligibleSlots.get(blockItemId).weight;
			cumulativeWeight += eligibleSlotWeight;
			if (randomInteger < cumulativeWeight) {
				return eligibleSlots.get(blockItemId);
			}
		}
		return null;
	}

	// Checks that all selected blocks are present in the player's inventory; warns about missing ones
	@Unique
	private boolean rbp$isBlocksMissing(BlockPlacerConfig blockPlacerConfig, LocalPlayer player) {
		Set<Identifier> selectedBlocks = blockPlacerConfig.getSelectedBlocksKey();

		Set<Identifier> missing = new HashSet<>();
		for (Identifier selectedBlock : selectedBlocks) {
			boolean found = false;
			for (int slotIndex = 0; slotIndex < Inventory.INVENTORY_SIZE; ++slotIndex) {
				ItemStack stack = player.getInventory().getItem(slotIndex);
				if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
					continue;
				}

				Identifier blockItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
				if (selectedBlock.equals(blockItemId)) {
					found = true;
					break;
				}
			}
			if (!found) {
				missing.add(selectedBlock);
			}
		}

		// Send a warning message listing missing materials
		if (!missing.isEmpty()) {
			StringBuilder sb = new StringBuilder("§c[RBP] Missing materials : §e");
			boolean first = true;
			for (Identifier id : missing) {
				if (!first) sb.append("§7, §e");
				first = false;
				BuiltInRegistries.ITEM.get(id).ifPresent(ref -> {
					Item item = ref.value();
					sb.append(item.getName(new ItemStack(item)).getString());
				});
			}
			player.sendSystemMessage(Component.literal(sb.toString()));
		}

		return !missing.isEmpty();
	}

	// Before block placement: swap to a randomly selected block based on configured weights
	@Inject(
		method = "useItemOn",
		at = @At("HEAD")
	)
	private void rbp$onUseItemOnHead(
		LocalPlayer player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> cir
	) {
		if (hand != InteractionHand.MAIN_HAND) {
			return;
		}

		if (!BlockPlacer.INSTANCE.isEnabled()) {
			return;
		}

		BlockPlacerConfig blockPlacerConfig = BlockPlacer.INSTANCE.getBlockPlacerConfig();
		if (blockPlacerConfig.getSelectedBlocksKey().isEmpty()) {
			return;
		}

		if (rbp$isBlocksMissing(blockPlacerConfig, player)) {
			BlockPlacer.INSTANCE.disable();
			return;
		}

		HashMap<Identifier, SlotEntry> eligibleSlots = rbp$getEligibleSlots(player.getInventory());

		SlotEntry randomSlotEntry = rbp$getRandomSlotEntry(eligibleSlots, player.getRandom());

		// Save current slot state for restoration after placement
		rbp$originalSlot = player.getInventory().getSelectedSlot();
		rbp$swapped = true;
		rbp$invSwapped = false;
		rbp$invSlot = -1;

		// If the selected block is in the hotbar, just switch to it
		if (randomSlotEntry.slot() < Inventory.SELECTION_SIZE) {
			player.getInventory().setSelectedSlot(randomSlotEntry.slot);
			return;
		}

		// If the block is in the main inventory, swap it with the current hotbar slot
		MultiPlayerGameMode self = (MultiPlayerGameMode) (Object) this;
		self.handleContainerInput(
				InventoryMenu.CONTAINER_ID,
				randomSlotEntry.slot(),
				rbp$originalSlot,
				ContainerInput.SWAP,
				player
		);

		player.getInventory().setSelectedSlot(rbp$originalSlot);
		rbp$invSwapped = true;
		rbp$invSlot = randomSlotEntry.slot();
	}

	// After block placement: restore the original slot and sync with the server
	@Inject(
		method = "useItemOn",
		at = @At("RETURN")
	)
	private void rbp$onUseItemOnReturn(
		LocalPlayer player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!rbp$swapped) {
			return;
		}

		// Swap back the inventory slot if one was used
		if (rbp$invSwapped && rbp$invSlot != -1) {
			MultiPlayerGameMode self = (MultiPlayerGameMode) (Object) this;
			self.handleContainerInput(
				InventoryMenu.CONTAINER_ID,
				rbp$invSlot,
				rbp$originalSlot,
				ContainerInput.SWAP,
				player
			);
		}

		// Restore the original hotbar selection and sync with server
		player.getInventory().setSelectedSlot(rbp$originalSlot);
		((MultiPlayerGameModeAccessor) this).callEnsureHasSentCarriedItem();

		rbp$swapped = false;
	}
}
