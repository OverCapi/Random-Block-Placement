package capi.rnd_block_placer.client.hud;

import capi.rnd_block_placer.RandomBlockPlacer;
import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.HudPosition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Map;

// Registers a HUD element showing that random placement is on, with the loaded preset's name
public class HudRndBlockPlacer {
    private static final int MARGIN = 4;
    // Distance from the bottom of the screen for ABOVE_HOTBAR, clear of the hotbar, health and armor bars
    private static final int ABOVE_HOTBAR_OFFSET = 80;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public static void init() {
        HudElementRegistry.addFirst(
                RandomBlockPlacer.id("enable"),
                (extract, delta) -> {
                    // Only render when random placement is enabled
                    if (!BlockPlacer.INSTANCE.isEnabled()) return;
                    HudPosition position = BlockPlacerConfig.INSTANCE.getHudPosition();
                    if (position == HudPosition.HIDDEN) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    Component text = hudText();
                    int textWidth = mc.font.width(text);
                    int screenWidth = mc.getWindow().getGuiScaledWidth();
                    int screenHeight = mc.getWindow().getGuiScaledHeight();
                    int x = switch (position) {
                        case TOP_LEFT -> MARGIN;
                        case TOP_RIGHT -> screenWidth - textWidth - MARGIN;
                        default -> (screenWidth - textWidth) / 2;
                    };
                    int y = position == HudPosition.ABOVE_HOTBAR ? screenHeight - ABOVE_HOTBAR_OFFSET : MARGIN;
                    extract.text(mc.font, text, x, y, TEXT_COLOR);
                }
        );
    }

    // "Random placement" plus " · <preset>" when a preset is loaded, and " (modified)" when the selection differs from it
    private static Component hudText() {
        MutableComponent text = Component.translatable("hud.rnd-block-placer.enabled").withStyle(ChatFormatting.GREEN);

        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        String presetName = config.getActivePreset();
        Map<Identifier, Integer> preset = presetName == null ? null : config.getPreset(presetName);
        if (preset == null) {
            return text;
        }
        text.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(presetName).withStyle(ChatFormatting.WHITE));
        if (!preset.equals(config.getSelectedBlocks())) {
            text.append(Component.translatable("hud.rnd-block-placer.modified").withStyle(ChatFormatting.YELLOW));
        }
        return text;
    }
}
