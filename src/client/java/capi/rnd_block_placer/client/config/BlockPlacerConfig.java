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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Manages persistent configuration: selected blocks, their weights, and JSON serialization
public class BlockPlacerConfig {
    public static BlockPlacerConfig INSTANCE = new BlockPlacerConfig();
    // Default weight assigned to newly selected blocks
    public static final int DEFAULT_WEIGHT = 100;

    // Map of block identifier → weight for weighted random selection
    private HashMap<Identifier, Integer> selectedBlocks = new HashMap<>();
    // Pretty-printing Gson instance for JSON read/write
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Returns the set of selected block identifiers (keys only)
    public Set<Identifier> getSelectedBlocksKey() {
        return selectedBlocks.keySet();
    }

    // Returns the full block → weight map
    public HashMap<Identifier, Integer> getSelectedBlocks() {
        return selectedBlocks;
    }

    // Returns the weight for a specific block, or null if not selected
    public Integer getSelectedBlockWeight(Identifier id) {
        return selectedBlocks.get(id);
    }

    // Replaces the entire selection with a new map
    public void setSelectedBlocks(HashMap<Identifier, Integer> selectedBlocks) {
        this.selectedBlocks.clear();
        this.selectedBlocks.putAll(selectedBlocks);
    }

    // Clears all selected blocks
    public void resetSelectedBlocks() {
        selectedBlocks.clear();
    }

    // Persists the current selection to the config file as JSON
    public void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject blocks = new JsonObject();
            for (Map.Entry<Identifier, Integer> entry : selectedBlocks.entrySet()) {
                blocks.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("selectedBlocks", blocks);
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
            JsonObject blocks = root.getAsJsonObject("selectedBlocks");
            if (blocks != null) {
                for (String key : blocks.keySet()) {
                    String[] parts = key.split(":", 2);
                    if (parts.length == 2) {
                        selectedBlocks.put(Identifier.fromNamespaceAndPath(parts[0], parts[1]), blocks.get(key).getAsInt());
                    }
                }
            }
        } catch (IOException e) {
            RandomBlockPlacer.LOGGER.error("Failed to load config file!", e);
        }
    }

    // Returns the config file path: <config-dir>/rnd-block-placer.json
    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rnd-block-placer.json");
    }
}
