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

public final class Bind {
    public static final Bind INSTANCE = new Bind();

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            RandomBlockPlacer.MOD_ID,
                            "rnd-block-placement"
                    )
            );

    private final KeyMapping openSelectionScreen =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key." + RandomBlockPlacer.MOD_ID + ".toggle_selection_screen",
                            InputConstants.Type.KEYSYM,
                            GLFW.GLFW_KEY_B,
                            CATEGORY
                    )
            );

    private final KeyMapping toggleBlockPlacement =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key." + RandomBlockPlacer.MOD_ID + ".toggle_block_placement",
                            InputConstants.Type.KEYSYM,
                            GLFW.GLFW_KEY_J,
                            CATEGORY
                    )
            );

    public static void load() {

    }

    private Bind() {
        register();
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {

        while (openSelectionScreen.consumeClick()) {
            toggleSelectionScreen(client);
        }

        while (toggleBlockPlacement.consumeClick()) {
            BlockPlacer.INSTANCE.toggleBlockPlacement();
        }
    }

    private void toggleSelectionScreen(Minecraft client) {
        client.setScreenAndShow(new BlockSelectionScreen());
    }
}
