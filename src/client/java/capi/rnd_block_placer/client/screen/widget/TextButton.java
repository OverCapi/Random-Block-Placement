package capi.rnd_block_placer.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// A simple text button drawn as a bordered box with a centered label
public class TextButton {
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int FILL_COLOR = 0xFF6C6C6C;
	private static final int HOVER_FILL_COLOR = 0xFF8C8C8C;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private final int x;
	private final int y;
	private final int width;
	private final int height;

	private Component label;
	private final Runnable onClick;

	public TextButton(int x, int y, int width, int height, Component label, Runnable onClick) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.label = label;
		this.onClick = onClick;
	}

	public boolean isHover(double mx, double my) {
		return mx >= x
				&& mx < x + width
				&& my >= y
				&& my < y + height;
	}

	public void onClick() {
		if (onClick != null) {
			onClick.run();
		}
	}

	public void setLabel(Component label) {
		this.label = label;
	}

	public void render(GuiGraphicsExtractor extract, Font font, int mx, int my) {
		boolean hovered = isHover(mx, my);

		extract.fill(x, y, x + width, y + height, BORDER_COLOR);
		extract.fill(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? HOVER_FILL_COLOR : FILL_COLOR);

		int textX = x + (width - font.width(label)) / 2;
		int textY = y + (height - font.lineHeight) / 2;
		extract.text(font, label, textX, textY, TEXT_COLOR);
	}
}