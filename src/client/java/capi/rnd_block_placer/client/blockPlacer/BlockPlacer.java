package capi.rnd_block_placer.client.blockPlacer;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;

// Singleton service that controls the random block placement mode
public class BlockPlacer {
    public static BlockPlacer INSTANCE = new BlockPlacer();

    // Whether random block placement is currently active
    private boolean isEnabled = false;

    // Reference to the shared config holding selected blocks and weights
    private BlockPlacerConfig blockPlacerConfig = BlockPlacerConfig.INSTANCE;

    public BlockPlacer() {}

    // Returns whether random placement mode is active
    public boolean isEnabled() {
        return isEnabled;
    }

    // Toggles the random placement mode on/off
    public void toggleBlockPlacement() {
        isEnabled = !isEnabled;
    }

    // Activates random placement mode
    public void enable() {
        isEnabled = true;
    }

    // Deactivates random placement mode
    public void disable() {
        isEnabled = false;
    }

    // Returns the config containing selected blocks and their weights
    public BlockPlacerConfig getBlockPlacerConfig() {
        return blockPlacerConfig;
    }
}
