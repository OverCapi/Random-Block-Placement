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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Manages persistent configuration: selected blocks, named presets, and JSON serialization
public class BlockPlacerConfig {
    public static final BlockPlacerConfig INSTANCE = new BlockPlacerConfig();
    // Default weight assigned to newly selected blocks
    public static final int DEFAULT_WEIGHT = 100;

    private static final String SELECTED_BLOCKS_KEY = "selectedBlocks";
    private static final String PRESETS_KEY = "presets";
    private static final String ACTIVE_PRESET_KEY = "activePreset";
    // Pretty-printing Gson instance for JSON read/write
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of block identifier → weight for weighted random selection
    private final Map<Identifier, Integer> selectedBlocks = new HashMap<>();
    // Named block selections (preset name → block → weight), kept in insertion order
    private final Map<String, Map<Identifier, Integer>> presets = new LinkedHashMap<>();
    // Name of the currently selected preset, or null when none is selected
    private String activePreset = null;

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

    // Returns the named presets as an unmodifiable view, in insertion order
    public Map<String, Map<Identifier, Integer>> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    // Returns the blocks of the named preset, or null if it does not exist
    public Map<Identifier, Integer> getPreset(String name) {
        Map<Identifier, Integer> preset = presets.get(name);
        return preset == null ? null : Collections.unmodifiableMap(preset);
    }

    // Creates or replaces a preset from the given block weights
    public void putPreset(String name, Map<Identifier, Integer> blocks) {
        presets.put(name, new HashMap<>(blocks));
    }

    // Removes the named preset, clearing the active preset if it was removed
    public void removePreset(String name) {
        presets.remove(name);
        if (name.equals(activePreset)) {
            activePreset = null;
        }
    }

    // Returns the name of the currently selected preset, or null if none
    public String getActivePreset() {
        return activePreset;
    }

    // Marks the named preset as the current selection
    public void setActivePreset(String name) {
        activePreset = name;
    }

    // Clears the current preset selection
    public void clearActivePreset() {
        activePreset = null;
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
            root.add(PRESETS_KEY, serializePresets());
            if (activePreset != null) {
                root.addProperty(ACTIVE_PRESET_KEY, activePreset);
            }
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
                    Identifier id = parseBlockId(key);
                    if (id != null) {
                        selectedBlocks.put(id, blocks.get(key).getAsInt());
                    }
                }
            }
            parsePresets(root.getAsJsonObject(PRESETS_KEY));
            if (root.has(ACTIVE_PRESET_KEY)) {
                activePreset = root.get(ACTIVE_PRESET_KEY).getAsString();
            }
        } catch (IOException | RuntimeException e) {
            RandomBlockPlacer.LOGGER.error("Failed to load config file!", e);
            selectedBlocks.clear();
            presets.clear();
            activePreset = null;
        }
    }

    // Serializes all presets as a JSON object of name → block → weight
    private JsonObject serializePresets() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Map<Identifier, Integer>> preset : presets.entrySet()) {
            JsonObject blocks = new JsonObject();
            for (Map.Entry<Identifier, Integer> entry : preset.getValue().entrySet()) {
                blocks.addProperty(entry.getKey().toString(), entry.getValue());
            }
            json.add(preset.getKey(), blocks);
        }
        return json;
    }

    // Parses the presets JSON object into the presets map; missing entries are ignored
    private void parsePresets(JsonObject json) {
        presets.clear();
        if (json == null) {
            return;
        }
        for (String name : json.keySet()) {
            JsonObject blocksJson = json.getAsJsonObject(name);
            Map<Identifier, Integer> blocks = new HashMap<>();
            for (String key : blocksJson.keySet()) {
                Identifier id = parseBlockId(key);
                if (id != null) {
                    blocks.put(id, blocksJson.get(key).getAsInt());
                }
            }
            presets.put(name, blocks);
        }
    }

    // Parses a "namespace:path" string into a block identifier, or null when malformed
    private static Identifier parseBlockId(String key) {
        String[] parts = key.split(":", 2);
        return parts.length == 2 ? Identifier.fromNamespaceAndPath(parts[0], parts[1]) : null;
    }

    // Returns the config file path: <config-dir>/rnd-block-placer.json
    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rnd-block-placer.json");
    }
}
