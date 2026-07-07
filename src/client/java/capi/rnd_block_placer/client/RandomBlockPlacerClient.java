package capi.rnd_block_placer.client;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.hud.HudRndBlockPlacer;
import capi.rnd_block_placer.client.keymapping.Bind;

import net.fabricmc.api.ClientModInitializer;

public class RandomBlockPlacerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockPlacerConfig.INSTANCE.load();
		Bind.load();
		HudRndBlockPlacer.init();
	}
}
