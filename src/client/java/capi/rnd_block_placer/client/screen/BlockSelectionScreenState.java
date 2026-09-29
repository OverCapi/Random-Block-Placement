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

    // Returns the given weight as a percentage of the total working weight
    public int percentOf(int weight) {
        int total = totalWeight();
        return total <= 0 ? 0 : weight * 100 / total;
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
