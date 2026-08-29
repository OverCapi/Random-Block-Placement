package capi.rnd_block_placer.client.hud;

import capi.rnd_block_placer.RandomBlockPlacer;
import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// Registers a HUD element that displays "ENABLE" in green when random placement is active
public class HudRndBlockPlacer {
    private static final int Y = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public static void init() {
        HudElementRegistry.addFirst(
                RandomBlockPlacer.id("enable"),
                (extract, delta) -> {

                    // Only render when random placement is enabled
                    if (!BlockPlacer.INSTANCE.isEnabled()) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    // Display "ENABLE" in the top-right corner
                    Component text = Component.translatable("hud.rnd-block-placer.enabled");
                    int tw = mc.font.width(text);
                    int x = mc.getWindow().getGuiScaledWidth() / 2 - tw / 2;
                    extract.text(mc.font, text, x, Y, TEXT_COLOR);
                }
        );
    }
}
