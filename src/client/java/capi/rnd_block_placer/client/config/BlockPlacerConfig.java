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

public class BlockPlacerConfig {
    public static BlockPlacerConfig INSTANCE = new BlockPlacerConfig();
    public static final int DEFAULT_WEIGHT = 100;

    private HashMap<Identifier, Integer> selectedBlocks = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Set<Identifier> getSelectedBlocksKey() {
        return selectedBlocks.keySet();
    }

    public HashMap<Identifier, Integer> getSelectedBlocks() {
        return selectedBlocks;
    }

    public Integer getSelectedBlockWeight(Identifier id) {
        return selectedBlocks.get(id);
    }

    public void setSelectedBlocks(HashMap<Identifier, Integer> selectedBlocks) {
        this.selectedBlocks.clear();
        this.selectedBlocks.putAll(selectedBlocks);
    }

    public void resetSelectedBlocks() {
        selectedBlocks.clear();
    }

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

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rnd-block-placer.json");
    }
}
