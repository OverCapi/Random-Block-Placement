package capi.rnd_block_placer.client.share;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// Turns a named block selection into compact chat-safe chunks and back; touches no game state
public final class PresetCodec {
    // Data characters per chunk, so "msg <name> [rbp1 xxxx nn/nn] <data>" stays under the 256-char command limit
    public static final int CHUNK_DATA_LENGTH = 180;
    public static final int MAX_CHUNKS = 32;
    private static final int MAX_DECODED_BYTES = 16 * 1024;
    private static final String DEFAULT_NAMESPACE = "minecraft:";

    // A decoded share: preset name and block id → weight
    public record Shared(String name, Map<String, Integer> weights) {}

    private PresetCodec() {}

    // Encodes the name and weights into chunk lines "[rbp1 <msgId> <i>/<n>] <data>"
    public static List<String> encode(String msgId, String name, Map<String, Integer> weights) {
        String data = encodeData(name, weights);
        int count = (data.length() + CHUNK_DATA_LENGTH - 1) / CHUNK_DATA_LENGTH;
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String part = data.substring(i * CHUNK_DATA_LENGTH, Math.min(data.length(), (i + 1) * CHUNK_DATA_LENGTH));
            chunks.add("[rbp1 " + msgId + " " + (i + 1) + "/" + count + "] " + part);
        }
        return chunks;
    }

    // Encodes the name and weights as a single unsplit code, as copied to the clipboard
    public static String encodeData(String name, Map<String, Integer> weights) {
        StringBuilder text = new StringBuilder(name.replace('\n', ' '));
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            String id = entry.getKey();
            if (id.startsWith(DEFAULT_NAMESPACE)) {
                id = id.substring(DEFAULT_NAMESPACE.length());
            }
            text.append('\n').append(id).append('=').append(entry.getValue());
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(deflate(text.toString().getBytes(StandardCharsets.UTF_8)));
    }

    // Decodes the joined chunk data, or returns null when it is malformed or too large
    public static Shared decode(String data) {
        try {
            String text = new String(inflate(Base64.getUrlDecoder().decode(data)), StandardCharsets.UTF_8);
            String[] lines = text.split("\n");
            String name = lines[0].trim();
            if (name.isEmpty()) {
                return null;
            }
            Map<String, Integer> weights = new LinkedHashMap<>();
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split("=", 2);
                int weight = Integer.parseInt(parts[1]);
                if (weight > 0) {
                    String id = parts[0].contains(":") ? parts[0] : DEFAULT_NAMESPACE + parts[0];
                    weights.put(id, weight);
                }
            }
            return new Shared(name, weights);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | DataFormatException e) {
            return null;
        }
    }

    private static byte[] deflate(byte[] input) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    // Inflates with a size cap so a malicious code cannot exhaust memory
    private static byte[] inflate(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new DataFormatException("truncated");
                }
                out.write(buffer, 0, n);
                if (out.size() > MAX_DECODED_BYTES) {
                    throw new DataFormatException("too large");
                }
            }
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    // Round-trip self-check: java -ea PresetCodec.java
    public static void main(String[] args) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (int i = 0; i < 300; i++) {
            weights.put("minecraft:block_" + Integer.toString(i * 7919, 36), 1 + i * 37);
        }
        weights.put("othermod:thing", 42);
        List<String> chunks = encode("ab12", "Château", weights);
        assert chunks.size() > 1 : "expected several chunks";
        StringBuilder data = new StringBuilder();
        for (String chunk : chunks) {
            assert ("msg SixteenCharsName " + chunk).length() <= 256 : chunk;
            data.append(chunk.substring(chunk.indexOf("] ") + 2));
        }
        Shared shared = decode(data.toString());
        assert shared != null && shared.name().equals("Château") && shared.weights().equals(weights);
        assert decode("not-valid!") == null;
        assert decode(data.substring(0, data.length() / 2)) == null;
        System.out.println("PresetCodec OK, " + chunks.size() + " chunks");
    }
}
