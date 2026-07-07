package capi.rnd_block_placer.client.hud;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Registers a HUD element that displays "ENABLE" in green when random placement is active
public class HudRndBlockPlacer {
    public static void init() {
        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("rnd-block-placer", "enable"),
                (extract, delta) -> {

                    // Only render when random placement is enabled
                    if (!BlockPlacer.INSTANCE.isEnabled()) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    // Display "ENABLE" in the top-right corner
                    String text = "§aENABLE";
                    int tw = mc.font.width(text);
                    int x = mc.getWindow().getGuiScaledWidth() - tw - 4;
                    int y = 4;
                    extract.text(mc.font, Component.literal(text), x, y, 0xFFFFFFFF);
                }
        );
    }
}
