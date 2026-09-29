package capi.rnd_block_placer.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Shared drawing helpers so every screen of the mod has the same look
public final class PanelStyle {
    private PanelStyle() {}

    // Dark translucent panel with a thin border
    public static void drawPanel(GuiGraphicsExtractor extract, ScreenRectangle panel) {
        extract.fill(panel.left(), panel.top(), panel.right(), panel.bottom(), PANEL_COLOR);
        extract.outline(panel.left(), panel.top(), panel.width(), panel.height(), PANEL_BORDER_COLOR);
    }

    // Bold title in the top-left corner of a panel, inside its padding
    public static void drawPanelTitle(GuiGraphicsExtractor extract, Font font, ScreenRectangle panel, Component title) {
        ScreenRectangle inner = SelectionLayout.inner(panel);
        extract.text(font, title.copy().withStyle(ChatFormatting.BOLD), inner.left(), inner.top(), TEXT_COLOR);
    }

    // Screen title on the left of the header row, vertically centered
    public static void drawHeaderTitle(GuiGraphicsExtractor extract, Font font, ScreenRectangle header, Component title) {
        extract.text(font, title, header.left() + 2, header.top() + (HEADER_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR);
    }
}
