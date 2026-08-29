package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mutable state for the block selection screen, isolated from the persisted config until "Save" is pressed
public class BlockSelectionScreenState {
    // Working copy of the block→weight map (not yet saved)
    private final Map<Identifier, Integer> workingWeights = new HashMap<>();
    // Working copy of the enabled flag (not yet saved)
    private boolean workingEnableRndPlacement = false;

    // Initializes the working state from the current BlockPlacer config
    public void init() {
        workingEnableRndPlacement = BlockPlacer.INSTANCE.isEnabled();
        workingWeights.clear();
        workingWeights.putAll(BlockPlacerConfig.INSTANCE.getSelectedBlocks());
    }

    // Resets the working state to defaults (empty selection, disabled)
    public void reset() {
        workingWeights.clear();
        workingEnableRndPlacement = false;
    }

    // Returns whether the working selection is empty
    public boolean isEmpty() {
        return workingWeights.isEmpty();
    }

    // Returns whether the given block is currently selected
    public boolean containsWeight(Identifier id) {
        return workingWeights.containsKey(id);
    }

    // Returns the weight of the given block, or 0 if not selected
    public int getWeight(Identifier id) {
        return workingWeights.getOrDefault(id, 0);
    }

    // Returns the weight of the given block, or a fallback if not selected
    public int getWeightOrElse(Identifier id, int fallback) {
        return workingWeights.getOrDefault(id, fallback);
    }

    // Sets the weight of a block; values ≤ 0 remove it from the selection
    public void setWeight(Identifier id, int weight) {
        if (weight <= 0) {
            workingWeights.remove(id);
        } else {
            workingWeights.put(id, weight);
        }
    }

    // Removes the given block from the working selection
    public void removeWeight(Identifier id) {
        workingWeights.remove(id);
    }

    // Returns the sum of all working weights
    public int totalWeight() {
        int total = 0;
        for (int weight : workingWeights.values()) {
            total += weight;
        }
        return total;
    }

    // Returns working weights sorted by weight descending, for display
    public List<Map.Entry<Identifier, Integer>> weightsSortedByDesc() {
        List<Map.Entry<Identifier, Integer>> sorted = new ArrayList<>(workingWeights.entrySet());
        sorted.sort(Map.Entry.<Identifier, Integer>comparingByValue().reversed());
        return sorted;
    }

    // Returns a copy of the working selection, for saving to the config
    public Map<Identifier, Integer> copyWeights() {
        return new HashMap<>(workingWeights);
    }

    // Returns whether random placement is enabled in the working state
    public boolean isRndPlacementEnabled() {
        return workingEnableRndPlacement;
    }
}
