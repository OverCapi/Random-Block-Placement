package capi.rnd_block_placer.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public class CustomButton {
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	private Texture texture;
	private final Runnable onClick;

	public CustomButton(
			int x,
			int y,
			int width,
			int height,
			Runnable onClick
	) {
		this(x, y, width, height, null, onClick);
	}


	public CustomButton(
			int x,
			int y,
			int width,
			int height,
			Texture texture,
			Runnable onClick
	) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.texture = texture;
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

	public void setTexture(Texture texture) {
		this.texture = texture;
	}

	public void render(GuiGraphicsExtractor extract) {
		if (texture == null) {
			return;
		}

		extract.blit(
				RenderPipelines.GUI_TEXTURED,
				texture.id(),
				x,
				y,
				0,
				0,
				width,
				height,
				texture.width(),
				texture.height(),
				texture.fullWidth(),
				texture.fullHeight()
		);
	}
}