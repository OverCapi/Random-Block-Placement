package capi.rnd_block_placer.client.blockPlacer;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;

public class BlockPlacer {
    public static BlockPlacer INSTANCE = new BlockPlacer();

    private boolean isEnabled = false;

    private BlockPlacerConfig blockPlacerConfig = BlockPlacerConfig.INSTANCE;

    public BlockPlacer() {}

    public boolean isEnabled() {
        return isEnabled;
    }

    public void toggleBlockPlacement() {
        isEnabled = !isEnabled;
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    public BlockPlacerConfig getBlockPlacerConfig() {
        return blockPlacerConfig;
    }
}
