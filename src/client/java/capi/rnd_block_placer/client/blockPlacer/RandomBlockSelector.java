package capi.rnd_block_placer.client.blockPlacer;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Pure selection logic for choosing a weighted-random block from the player's inventory
public final class RandomBlockSelector {

    // Internal record representing an eligible slot with its stack and configured weight
    public record SlotEntry(int slot, ItemStack stack, int weight) {}

    private RandomBlockSelector() {}

    // Scans the inventory for slots containing selected blocks and returns them with their weights
    public static Map<Identifier, SlotEntry> eligibleSlots(Inventory inventory, BlockPlacerConfig config) {
        Map<Identifier, SlotEntry> eligibleSlots = new HashMap<>();

        Set<Identifier> selectedBlocks = config.getSelectedBlocksKey();
        for (int slotIndex = 0; slotIndex < Inventory.INVENTORY_SIZE; ++slotIndex) {
            ItemStack stack = inventory.getItem(slotIndex);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }

            Identifier blockItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (selectedBlocks.contains(blockItemId)) {
                eligibleSlots.put(blockItemId, new SlotEntry(slotIndex, stack, config.getSelectedBlockWeight(blockItemId)));
            }
        }
        return eligibleSlots;
    }

    // Weighted random selection from eligible slots using the configured weights
    public static Optional<SlotEntry> select(Map<Identifier, SlotEntry> eligibleSlots, RandomSource randomSource) {
        int totalWeight = 0;
        for (Map.Entry<Identifier, SlotEntry> entry : eligibleSlots.entrySet()) {
            totalWeight += entry.getValue().weight;
        }
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int randomInteger = randomSource.nextInt(totalWeight);
        int cumulativeWeight = 0;
        for (Map.Entry<Identifier, SlotEntry> entry : eligibleSlots.entrySet()) {
            cumulativeWeight += entry.getValue().weight;
            if (randomInteger < cumulativeWeight) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    // Scans the inventory and picks a weighted-random eligible slot
    public static Optional<SlotEntry> selectSlot(Inventory inventory, BlockPlacerConfig config, RandomSource randomSource) {
        return select(eligibleSlots(inventory, config), randomSource);
    }

    // Returns the selected blocks that are absent from the player's inventory
    public static Set<Identifier> missingBlocks(Inventory inventory, BlockPlacerConfig config) {
        Set<Identifier> missing = new HashSet<>();
        for (Identifier selectedBlock : config.getSelectedBlocksKey()) {
            boolean found = false;
            for (int slotIndex = 0; slotIndex < Inventory.INVENTORY_SIZE; ++slotIndex) {
                ItemStack stack = inventory.getItem(slotIndex);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                    continue;
                }
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(selectedBlock)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(selectedBlock);
            }
        }
        return missing;
    }

    // Builds the warning message listing missing materials
    public static Component missingBlocksMessage(Set<Identifier> missing) {
        StringBuilder names = new StringBuilder();
        boolean first = true;
        for (Identifier id : missing) {
            if (!first) names.append("§7, §e");
            first = false;
            BuiltInRegistries.ITEM.get(id).ifPresent(ref -> {
                ItemStack stack = new ItemStack(ref.value());
                names.append(ref.value().getName(stack).getString());
            });
        }
        return Component.translatable("message.rnd-block-placer.missing_materials", names.toString());
    }
}
