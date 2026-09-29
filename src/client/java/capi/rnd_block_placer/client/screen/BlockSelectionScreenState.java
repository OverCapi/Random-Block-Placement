package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Working copy of the selection and loaded preset, isolated from the persisted config until applied
public class BlockSelectionScreenState {
    // Weights set from a percentage are stored in hundredths of a percent (100% = 10000)
    private static final int PERCENT_SCALE = 100;
    // Highest percentage a block can get while other blocks are selected, so they keep a share
    public static final int MAX_PERCENT_WITH_OTHERS = 99;

    // Working copy of the block→weight map (not yet applied)
    private final Map<Identifier, Integer> workingWeights = new HashMap<>();
    // Name of the preset the working selection was loaded from, or null
    private String activePreset;

    // Initializes the working state from the current config
    public BlockSelectionScreenState() {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        workingWeights.putAll(config.getSelectedBlocks());
        activePreset = config.getActivePreset();
    }

    // Returns whether the working selection is empty
    public boolean isEmpty() {
        return workingWeights.isEmpty();
    }

    // Returns the number of selected blocks
    public int size() {
        return workingWeights.size();
    }

    // Returns whether the given block is currently selected
    public boolean containsWeight(Identifier id) {
        return workingWeights.containsKey(id);
    }

    // Returns the weight of the given block, or 0 if not selected
    public int getWeight(Identifier id) {
        return workingWeights.getOrDefault(id, 0);
    }

    // Adds a block with the average weight of the current selection, so it gets an even share
    public void addBlock(Identifier id) {
        int weight = isEmpty() ? BlockPlacerConfig.DEFAULT_WEIGHT : Math.max(1, Math.round((float) totalWeight() / size()));
        workingWeights.put(id, weight);
    }

    // Percentage the block has, or would get if added now (even share with the current blocks)
    public int percentOrDefault(Identifier id) {
        if (containsWeight(id)) {
            return percentOf(getWeight(id));
        }
        return Math.round(100.0f / (size() + 1));
    }

    // Sets the block's chance to the given percentage. The other blocks share the rest and keep
    // their relative proportions. 0 removes the block; a block alone always has 100%.
    public void setPercent(Identifier id, int percent) {
        if (percent <= 0) {
            workingWeights.remove(id);
            return;
        }
        Map<Identifier, Integer> others = new HashMap<>(workingWeights);
        others.remove(id);
        if (others.isEmpty()) {
            workingWeights.put(id, workingWeights.getOrDefault(id, BlockPlacerConfig.DEFAULT_WEIGHT));
            return;
        }
        percent = Math.min(percent, MAX_PERCENT_WITH_OTHERS);

        // Rescale the other blocks so they sum to exactly (100 - percent) * PERCENT_SCALE
        int othersTarget = (100 - percent) * PERCENT_SCALE;
        long othersSum = others.values().stream().mapToLong(Integer::longValue).sum();
        int assigned = 0;
        Identifier largest = null;
        for (Map.Entry<Identifier, Integer> entry : others.entrySet()) {
            int weight = (int) Math.max(1, Math.round((double) entry.getValue() * othersTarget / othersSum));
            workingWeights.put(entry.getKey(), weight);
            assigned += weight;
            if (largest == null || weight > workingWeights.get(largest)) {
                largest = entry.getKey();
            }
        }
        // Give the rounding difference to the heaviest block so the total stays exact
        workingWeights.put(largest, Math.max(1, workingWeights.get(largest) + othersTarget - assigned));
        workingWeights.put(id, percent * PERCENT_SCALE);
    }

    // Removes the given block from the working selection
    public void removeWeight(Identifier id) {
        workingWeights.remove(id);
    }

    // Empties the working selection; the loaded preset itself is left untouched
    public void clear() {
        workingWeights.clear();
    }

    // Replaces the working selection with the named preset and marks it as loaded
    public void loadPreset(String name) {
        Map<Identifier, Integer> preset = BlockPlacerConfig.INSTANCE.getPreset(name);
        if (preset == null) {
            return;
        }
        workingWeights.clear();
        workingWeights.putAll(preset);
        activePreset = name;
    }

    // Returns the name of the loaded preset, or null
    public String getActivePreset() {
        return activePreset;
    }

    // Marks the named preset as loaded, or none when null
    public void setActivePreset(String name) {
        activePreset = name;
    }

    // Returns whether the working selection differs from the loaded preset's blocks
    public boolean isActivePresetModified() {
        Map<Identifier, Integer> preset = activePreset == null ? null : BlockPlacerConfig.INSTANCE.getPreset(activePreset);
        return preset != null && !preset.equals(workingWeights);
    }

    // Returns the sum of all working weights
    public int totalWeight() {
        int total = 0;
        for (int weight : workingWeights.values()) {
            total += weight;
        }
        return total;
    }

    // Returns the given weight as a rounded percentage of the total working weight
    public int percentOf(int weight) {
        return percent(weight, totalWeight());
    }

    // Returns weight / total as a rounded percentage
    public static int percent(int weight, int total) {
        return total <= 0 ? 0 : Math.round(weight * 100.0f / total);
    }

    // Returns working weights sorted by weight descending, ties broken by identifier
    public List<Map.Entry<Identifier, Integer>> weightsSortedByDesc() {
        List<Map.Entry<Identifier, Integer>> sorted = new ArrayList<>(workingWeights.entrySet());
        sorted.sort(Map.Entry.<Identifier, Integer>comparingByValue().reversed()
                .thenComparing(entry -> entry.getKey().toString()));
        return sorted;
    }

    // Returns a copy of the working selection
    public Map<Identifier, Integer> copyWeights() {
        return new HashMap<>(workingWeights);
    }

    // Returns whether the working state differs from what is saved in the config
    public boolean isDirty() {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        return !workingWeights.equals(config.getSelectedBlocks())
                || !Objects.equals(activePreset, config.getActivePreset());
    }

    // Writes the working state to the config and saves it to disk
    public void apply() {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        if (activePreset != null && config.getPreset(activePreset) == null) {
            activePreset = null;
        }
        config.setSelectedBlocks(workingWeights);
        config.setActivePreset(activePreset);
        config.save();
    }
}
