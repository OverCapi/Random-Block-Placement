package capi.rnd_block_placer.client.hud;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HudRndBlockPlacer {
    public static void init() {
        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("rnd-block-placer", "enable"),
                (extract, delta) -> {

                    if (!BlockPlacer.INSTANCE.isEnabled()) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    String text = "§aENABLE";
                    int tw = mc.font.width(text);
                    int x = mc.getWindow().getGuiScaledWidth() - tw - 4;
                    int y = 4;
                    extract.text(mc.font, Component.literal(text), x, y, 0xFFFFFFFF);
                }
        );
    }
}
