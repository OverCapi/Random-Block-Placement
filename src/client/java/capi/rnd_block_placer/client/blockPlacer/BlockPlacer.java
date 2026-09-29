package capi.rnd_block_placer.client.blockPlacer;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.MissingBlockMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

// Singleton service that controls the random block placement mode
public class BlockPlacer {
    public static final BlockPlacer INSTANCE = new BlockPlacer();

    // Whether random block placement is currently active
    private boolean isEnabled = false;
    // Missing blocks the player has already been warned about, so each is reported once
    private final Set<Identifier> warnedMissing = new HashSet<>();

    private BlockPlacer() {}

    // Returns whether random placement mode is active
    public boolean isEnabled() {
        return isEnabled;
    }

    // Toggles the random placement mode on/off
    public void toggleBlockPlacement() {
        isEnabled = !isEnabled;
        warnedMissing.clear();
    }

    // Activates random placement mode
    public void enable() {
        isEnabled = true;
        warnedMissing.clear();
    }

    // Deactivates random placement mode
    public void disable() {
        isEnabled = false;
        warnedMissing.clear();
    }

    // Decides whether placement can go on while some selected blocks are missing from the inventory.
    // Warns once per missing block in CONTINUE mode (not in CONTINUE_QUIET); turns placement off in DISABLE mode
    // or when nothing is left.
    public boolean canContinueWithout(Set<Identifier> missing, LocalPlayer player) {
        // A block that came back will be reported again if it runs out later
        warnedMissing.retainAll(missing);
        if (missing.isEmpty()) {
            return true;
        }

        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        boolean nothingLeft = missing.containsAll(config.getSelectedBlocksKey());
        if (nothingLeft || config.getMissingBlockMode() == MissingBlockMode.DISABLE) {
            player.sendSystemMessage(RandomBlockSelector.missingBlocksMessage("message.rnd-block-placer.missing_materials", missing));
            disable();
            return false;
        }

        if (config.getMissingBlockMode() == MissingBlockMode.CONTINUE_QUIET) {
            return true;
        }
        Set<Identifier> newlyMissing = new HashSet<>(missing);
        newlyMissing.removeAll(warnedMissing);
        if (!newlyMissing.isEmpty()) {
            player.sendSystemMessage(RandomBlockSelector.missingBlocksMessage("message.rnd-block-placer.missing_continue", newlyMissing));
            warnedMissing.addAll(newlyMissing);
        }
        return true;
    }
}
