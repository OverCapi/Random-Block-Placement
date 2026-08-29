package capi.rnd_block_placer.client.mixin;

import java.util.Optional;
import java.util.Set;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.blockPlacer.RandomBlockSelector;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
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

		BlockPlacerConfig blockPlacerConfig = BlockPlacerConfig.INSTANCE;
		if (blockPlacerConfig.getSelectedBlocksKey().isEmpty()) {
			return;
		}

		Set<Identifier> missingBlocks = RandomBlockSelector.missingBlocks(player.getInventory(), blockPlacerConfig);
		if (!missingBlocks.isEmpty()) {
			player.sendSystemMessage(RandomBlockSelector.missingBlocksMessage(missingBlocks));
			BlockPlacer.INSTANCE.disable();
			return;
		}

		Optional<RandomBlockSelector.SlotEntry> targetSlotEntry =
				RandomBlockSelector.selectSlot(player.getInventory(), blockPlacerConfig, player.getRandom());
		if (targetSlotEntry.isEmpty()) {
			return;
		}

		// Save current slot state for restoration after placement
		rbp$originalSlot = player.getInventory().getSelectedSlot();
		rbp$swapped = true;
		rbp$invSwapped = false;
		rbp$invSlot = -1;

		// If the selected block is in the hotbar, just switch to it
		RandomBlockSelector.SlotEntry chosenSlotEntry = targetSlotEntry.get();
		if (chosenSlotEntry.slot() < Inventory.SELECTION_SIZE) {
			player.getInventory().setSelectedSlot(chosenSlotEntry.slot());
			return;
		}

		// If the block is in the main inventory, swap it with the current hotbar slot
		MultiPlayerGameMode self = (MultiPlayerGameMode) (Object) this;
		self.handleContainerInput(
				InventoryMenu.CONTAINER_ID,
				chosenSlotEntry.slot(),
				rbp$originalSlot,
				ContainerInput.SWAP,
				player
		);

		player.getInventory().setSelectedSlot(rbp$originalSlot);
		rbp$invSwapped = true;
		rbp$invSlot = chosenSlotEntry.slot();
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
