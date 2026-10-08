package capi.rnd_block_placer.client.share;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Shares presets with other players through /msg, since a vanilla server relays nothing else.
// Incoming chunks are hidden, reassembled, and offered with a clickable [Import] chat button.
public final class PresetShare {
    // One command per second: vanilla kicks for chat spam above roughly ten messages in a burst
    private static final int TICKS_BETWEEN_COMMANDS = 20;
    private static final int MAX_ASSEMBLIES = 16;
    private static final Pattern CHUNK = Pattern.compile("\\[rbp1 ([a-z0-9]{4}) (\\d+)/(\\d+)] ([A-Za-z0-9_-]+)");

    // Outgoing actions (commands, then the final confirmation), run one per throttle step
    private static final Queue<Runnable> outgoing = new ArrayDeque<>();
    private static int cooldown;
    // Ids of the shares we sent, so our own "You whisper to…" echoes are not offered for import
    private static final Set<String> sentIds = new HashSet<>();
    // Incoming shares being reassembled: msgId → chunk data (null until received)
    private static final Map<String, String[]> assemblies = new LinkedHashMap<>();
    // Complete shares waiting for the player to click [Import]: token → share
    private static final Map<Integer, PresetCodec.Shared> received = new HashMap<>();
    private static int nextToken = 1;
    private static final SystemToast.SystemToastId SHARE_TOAST = new SystemToast.SystemToastId();

    private PresetShare() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) ->
                !handleIncoming(message.getString(), sender != null ? sender.name() : null));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || !handleIncoming(message.getString(), null));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("rbp").then(ClientCommands.literal("import")
                        .then(ClientCommands.argument("token", IntegerArgumentType.integer())
                                .executes(ctx -> importReceived(IntegerArgumentType.getInteger(ctx, "token")))))));
    }

    // Names of the other players currently online, sorted
    public static List<String> otherPlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return List.of();
        }
        String self = connection.getLocalGameProfile().name();
        Collection<PlayerInfo> online = connection.getOnlinePlayers();
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : online) {
            String name = info.getProfile().name();
            if (!name.equals(self)) {
                names.add(name);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    // Queues the /msg commands sending the preset to each player, or public chat messages when players is empty
    public static void send(String name, Map<Identifier, Integer> weights, List<String> players) {
        String msgId = Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36 * 36, 36 * 36 * 36 * 36), 36);
        sentIds.add(msgId);
        Map<String, Integer> plain = new LinkedHashMap<>();
        weights.forEach((id, weight) -> plain.put(id.toString(), weight));
        List<String> chunks = PresetCodec.encode(msgId, name, plain);
        if (players.isEmpty()) {
            for (String chunk : chunks) {
                outgoing.add(() -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.sendChat(chunk);
                    }
                });
            }
            outgoing.add(() -> feedback(Component.translatable("chat.rnd-block-placer.share.sent_chat", name)
                    .withStyle(ChatFormatting.GREEN)));
            return;
        }
        for (String player : players) {
            for (String chunk : chunks) {
                outgoing.add(() -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.sendCommand("msg " + player + " " + chunk);
                    }
                });
            }
        }
        outgoing.add(() -> feedback(Component.translatable("chat.rnd-block-placer.share.sent",
                name, String.join(", ", players)).withStyle(ChatFormatting.GREEN)));
    }

    private static void tick() {
        if (Minecraft.getInstance().getConnection() == null) {
            outgoing.clear();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        Runnable next = outgoing.poll();
        if (next != null) {
            next.run();
            cooldown = TICKS_BETWEEN_COMMANDS;
        }
    }

    // Returns true when the message is a share chunk (and must be hidden); offers the share once complete
    private static boolean handleIncoming(String text, String sender) {
        Matcher matcher = CHUNK.matcher(text);
        if (!matcher.find()) {
            return false;
        }
        String msgId = matcher.group(1);
        int index = Integer.parseInt(matcher.group(2));
        int count = Integer.parseInt(matcher.group(3));
        if (sentIds.contains(msgId) || count < 1 || count > PresetCodec.MAX_CHUNKS || index < 1 || index > count) {
            return true;
        }

        String[] parts = assemblies.computeIfAbsent(msgId, id -> new String[count]);
        if (parts.length != count) {
            return true;
        }
        parts[index - 1] = matcher.group(4);
        while (assemblies.size() > MAX_ASSEMBLIES) {
            assemblies.remove(assemblies.keySet().iterator().next());
        }
        for (String part : parts) {
            if (part == null) {
                return true;
            }
        }
        assemblies.remove(msgId);

        PresetCodec.Shared shared = PresetCodec.decode(String.join("", parts));
        if (shared == null || shared.weights().isEmpty()) {
            feedback(Component.translatable("chat.rnd-block-placer.share.invalid").withStyle(ChatFormatting.RED));
            return true;
        }
        int token = nextToken++;
        received.put(token, shared);
        String from = sender != null ? sender : text.substring(0, matcher.start()).replaceAll("[:\\s]+$", "");
        Component button = Component.translatable("chat.rnd-block-placer.share.import_button").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.RunCommand("/rbp import " + token))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.rnd-block-placer.share.import_hover"))));
        feedback(Component.translatable("chat.rnd-block-placer.share.received",
                from, shared.name(), shared.weights().size()).withStyle(ChatFormatting.GOLD).append(" ").append(button));
        return true;
    }

    // Imports a share offered in chat by its [Import] button
    private static int importReceived(int token) {
        PresetCodec.Shared shared = received.remove(token);
        if (shared == null) {
            feedback(Component.translatable("chat.rnd-block-placer.share.import_missing").withStyle(ChatFormatting.RED));
            return 0;
        }
        String name = importShared(shared);
        if (name == null) {
            feedback(Component.translatable("chat.rnd-block-placer.share.import_empty").withStyle(ChatFormatting.RED));
            return 0;
        }
        feedback(Component.translatable("chat.rnd-block-placer.share.imported", name).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    // Copies the preset's code to the clipboard
    public static void copyToClipboard(String name, Map<Identifier, Integer> weights) {
        Map<String, Integer> plain = new LinkedHashMap<>();
        weights.forEach((id, weight) -> plain.put(id.toString(), weight));
        Minecraft.getInstance().keyboardHandler.setClipboard(PresetCodec.encodeData(name, plain));
        toast(Component.translatable("toast.rnd-block-placer.copied", name));
    }

    // Imports the code in the clipboard (a bare code or a single-chunk chat line); returns the new preset name or null
    public static String importFromClipboard() {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
        Matcher matcher = CHUNK.matcher(text);
        if (matcher.find() && matcher.group(3).equals("1")) {
            text = matcher.group(4);
        }
        PresetCodec.Shared shared = PresetCodec.decode(text);
        if (shared == null) {
            toast(Component.translatable("toast.rnd-block-placer.import_invalid"));
            return null;
        }
        String name = importShared(shared);
        toast(name == null
                ? Component.translatable("chat.rnd-block-placer.share.import_empty")
                : Component.translatable("chat.rnd-block-placer.share.imported", name));
        return name;
    }

    // Saves a share as a new preset, adding " (2)", " (3)"… when the name is taken.
    // Returns the preset name, or null when none of its blocks exist in this game.
    private static String importShared(PresetCodec.Shared shared) {
        Map<Identifier, Integer> weights = new HashMap<>();
        for (Map.Entry<String, Integer> entry : shared.weights().entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            // Blocks from mods this client lacks are dropped
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                weights.put(id, entry.getValue());
            }
        }
        if (weights.isEmpty()) {
            return null;
        }

        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        String name = shared.name();
        for (int i = 2; config.getPresets().containsKey(name); i++) {
            name = shared.name() + " (" + i + ")";
        }
        config.putPreset(name, weights);
        config.save();
        return name;
    }

    // Toast notification, visible even while a screen is open
    private static void toast(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        SystemToast.addOrUpdate(minecraft.gui.toastManager(), SHARE_TOAST,
                Component.translatable("toast.rnd-block-placer.title"), message);
    }

    private static void feedback(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("[RBP] ").append(message));
        }
    }
}
