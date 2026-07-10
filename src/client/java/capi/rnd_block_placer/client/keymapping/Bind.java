package capi.rnd_block_placer.client.keymapping;

import capi.rnd_block_placer.RandomBlockPlacer;
import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.screen.BlockSelectionScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

// Singleton that registers and handles all keybindings for the mod
public final class Bind {
    // Custom keybinding category for organizing mod bindings in settings
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            RandomBlockPlacer.MOD_ID,
                            "rnd-block-placement"
                    )
            );

    // Keybinding: opens the block selection screen (default: B)
    private final KeyMapping openSelectionScreen =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key." + RandomBlockPlacer.MOD_ID + ".toggle_selection_screen",
                            InputConstants.Type.KEYSYM,
                            InputConstants.KEY_B,
                            CATEGORY
                    )
            );

    // Keybinding: toggles random block placement on/off (default: J)
    private final KeyMapping toggleBlockPlacement =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key." + RandomBlockPlacer.MOD_ID + ".toggle_block_placement",
                            InputConstants.Type.KEYSYM,
                            GLFW.GLFW_KEY_J,
                            CATEGORY
                    )
            );

    public static final Bind INSTANCE = new Bind();

    // Placeholder — actual registration happens in the constructor
    public static void load() {}

    private Bind() {
        register();
    }

    // Registers the client tick handler that checks for key presses
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    // Called every client tick; processes consumed key presses
    private void onClientTick(Minecraft client) {
        // Open the block selection screen when B is pressed
        while (openSelectionScreen.consumeClick()) {
            toggleSelectionScreen(client);
        }

        // Toggle random placement when J is pressed
        while (toggleBlockPlacement.consumeClick()) {
            BlockPlacer.INSTANCE.toggleBlockPlacement();
        }
    }

    // Opens the block selection GUI screen
    private void toggleSelectionScreen(Minecraft client) {
        client.setScreenAndShow(new BlockSelectionScreen());
    }
}
