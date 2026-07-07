package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.resources.Identifier;

import java.util.HashMap;

// Mutable state for the block selection screen, isolated from the persisted config until "Save" is pressed
public class BlockSelectionScreenState {
    // Whether the selection screen is currently open
    private boolean isOpen = false;
    // Working copy of the block→weight map (not yet saved)
    private HashMap<Identifier, Integer> workingWeights = new HashMap<>();
    // Working copy of the enabled flag (not yet saved)
    private boolean workingEnableRndPlacement = false;

    // Initializes the working state from the current BlockPlacer config
    public void init() {
        BlockPlacer blockPlacer = BlockPlacer.INSTANCE;
        BlockPlacerConfig blockPlacerConfig = blockPlacer.getBlockPlacerConfig();

        isOpen = true;
        workingEnableRndPlacement = blockPlacer.isEnabled();
        workingWeights.clear();
        workingWeights.putAll(blockPlacerConfig.getSelectedBlocks());
    }

    // Resets the working state to defaults (empty selection, disabled)
    public void reset() {
        workingWeights.clear();
        workingEnableRndPlacement = false;
    }

    // Returns the working weight map for editing
    public HashMap<Identifier, Integer> getWorkingWeights() {
        return workingWeights;
    }

    // Adds or updates a block's weight in the working state
    public void addWeight(Identifier id, Integer weight) {
        workingWeights.put(id, weight);
    }

    // Toggles the working random placement flag
    public void toggleRndPlacement() {
        workingEnableRndPlacement = !workingEnableRndPlacement;
    }

    // Enables random placement in the working state
    public void enableRndPlacement() {
        workingEnableRndPlacement = true;
    }

    // Disables random placement in the working state
    public void disableRndPlacement() {
        workingEnableRndPlacement = false;
    }

    // Returns whether random placement is enabled in the working state
    public boolean isRndPlacementEnabled() {
        return workingEnableRndPlacement;
    }

    // Returns whether the screen is currently open
    public boolean isOpen() {
        return isOpen;
    }

    // Sets the open state
    public void setOpen(boolean open) {
        isOpen = open;
    }
}
