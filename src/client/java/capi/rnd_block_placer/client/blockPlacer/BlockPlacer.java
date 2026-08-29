package capi.rnd_block_placer.client.blockPlacer;

// Singleton service that controls the random block placement mode
public class BlockPlacer {
    public static final BlockPlacer INSTANCE = new BlockPlacer();

    // Whether random block placement is currently active
    private boolean isEnabled = false;

    private BlockPlacer() {}

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
}
