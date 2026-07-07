package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import net.minecraft.resources.Identifier;

import java.util.HashMap;

public class BlockSelectionScreenState {
    private boolean isOpen = false;
    private HashMap<Identifier, Integer> workingWeights = new HashMap<>();
    private boolean workingEnableRndPlacement = false;

    public void init() {
        BlockPlacer blockPlacer = BlockPlacer.INSTANCE;
        BlockPlacerConfig blockPlacerConfig = blockPlacer.getBlockPlacerConfig();

        isOpen = true;
        workingEnableRndPlacement = blockPlacer.isEnabled();
        workingWeights.clear();
        workingWeights.putAll(blockPlacerConfig.getSelectedBlocks());
    }

    public void reset() {
        workingWeights.clear();
        workingEnableRndPlacement = false;
    }

    public HashMap<Identifier, Integer> getWorkingWeights() {
        return workingWeights;
    }

    public void addWeight(Identifier id, Integer weight) {
        workingWeights.put(id, weight);
    }

    public void toggleRndPlacement() {
        workingEnableRndPlacement = !workingEnableRndPlacement;
    }

    public void enableRndPlacement() {
        workingEnableRndPlacement = true;
    }

    public void disableRndPlacement() {
        workingEnableRndPlacement = false;
    }

    public boolean isRndPlacementEnabled() {
        return workingEnableRndPlacement;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }
}
