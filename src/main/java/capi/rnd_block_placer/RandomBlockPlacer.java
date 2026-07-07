package capi.rnd_block_placer;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomBlockPlacer implements ModInitializer {
	// Unique identifier for this mod, used for registries and resource locations
	public static final String MOD_ID = "rnd-block-placer";

	// Logger instance for console and log file output
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Random Block Placer initialized!");
	}

	// Creates a namespaced Identifier using the mod ID
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
