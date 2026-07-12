package capi.rnd_block_placer.client.screen.widget;

import net.minecraft.resources.Identifier;

public record Texture(
		Identifier id,
		int width,
		int height,
		int fullWidth,
		int fullHeight
) {
	public Texture(Identifier id, int width, int height) {
		this (id, width, height, width, height);
	}
}
