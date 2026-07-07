package capi.rnd_block_placer.client;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.hud.HudRndBlockPlacer;
import capi.rnd_block_placer.client.keymapping.Bind;

import net.fabricmc.api.ClientModInitializer;

public class RandomBlockPlacerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Load persisted block selection from config file
		BlockPlacerConfig.INSTANCE.load();
		// Register keybindings
		Bind.load();
		// Register the HUD element showing "ENABLE" status
		HudRndBlockPlacer.init();
	}
}
