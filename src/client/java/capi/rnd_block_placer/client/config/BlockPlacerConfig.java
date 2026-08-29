package capi.rnd_block_placer.client.config;

import capi.rnd_block_placer.RandomBlockPlacer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Manages persistent configuration: selected blocks, their weights, and JSON serialization
public class BlockPlacerConfig {
    public static final BlockPlacerConfig INSTANCE = new BlockPlacerConfig();
    // Default weight assigned to newly selected blocks
    public static final int DEFAULT_WEIGHT = 100;

    private static final String SELECTED_BLOCKS_KEY = "selectedBlocks";
    // Pretty-printing Gson instance for JSON read/write
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of block identifier → weight for weighted random selection
    private final Map<Identifier, Integer> selectedBlocks = new HashMap<>();

    private BlockPlacerConfig() {}

    // Returns the set of selected block identifiers (keys only)
    public Set<Identifier> getSelectedBlocksKey() {
        return Collections.unmodifiableSet(selectedBlocks.keySet());
    }

    // Returns the full block → weight map
    public Map<Identifier, Integer> getSelectedBlocks() {
        return Collections.unmodifiableMap(selectedBlocks);
    }

    // Returns the weight for a specific block, or 0 if not selected
    public int getSelectedBlockWeight(Identifier id) {
        return selectedBlocks.getOrDefault(id, 0);
    }

    // Replaces the entire selection with a new map
    public void setSelectedBlocks(Map<Identifier, Integer> newSelectedBlocks) {
        selectedBlocks.clear();
        selectedBlocks.putAll(newSelectedBlocks);
    }

    // Persists the current selection to the config file as JSON
    public void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject blocks = new JsonObject();
            for (Map.Entry<Identifier, Integer> entry : selectedBlocks.entrySet()) {
                blocks.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add(SELECTED_BLOCKS_KEY, blocks);
            Files.writeString(getConfigPath(), GSON.toJson(root));
        } catch (IOException e) {
            RandomBlockPlacer.LOGGER.error("Failed to save config file!", e);
        }
    }

    // Loads the selection from the config file, merging into the current map
    public void load() {
        try {
            Path path = getConfigPath();
            if (!Files.exists(path)) return;
            String content = Files.readString(path);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            selectedBlocks.clear();
            JsonObject blocks = root.getAsJsonObject(SELECTED_BLOCKS_KEY);
            if (blocks != null) {
                for (String key : blocks.keySet()) {
                    String[] parts = key.split(":", 2);
                    if (parts.length == 2) {
                        selectedBlocks.put(Identifier.fromNamespaceAndPath(parts[0], parts[1]), blocks.get(key).getAsInt());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            RandomBlockPlacer.LOGGER.error("Failed to load config file!", e);
            selectedBlocks.clear();
        }
    }

    // Returns the config file path: <config-dir>/rnd-block-placer.json
    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rnd-block-placer.json");
    }
}
