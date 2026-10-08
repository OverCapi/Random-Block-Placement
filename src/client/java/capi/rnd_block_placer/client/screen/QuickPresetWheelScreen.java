package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.blockPlacer.BlockPlacer;
import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.keymapping.KeyBindings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstants.*;

// Radial menu shown while the wheel key is held: the slot the cursor points at is loaded on release
public class QuickPresetWheelScreen extends Screen {
    private static final int SLOTS = BlockPlacerConfig.QUICK_PRESET_COUNT;

    public QuickPresetWheelScreen() {
        super(Component.translatable("screen.rnd-block-placer.wheel"));
    }

    // Slot pointed at by a cursor offset from the wheel center, or -1 inside the dead zone.
    // Slot 0 is straight up, the others follow clockwise.
    static int segmentAt(double dx, double dy, double deadZone) {
        if (dx * dx + dy * dy < deadZone * deadZone) {
            return -1;
        }
        double sector = 360.0 / SLOTS;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90 + sector / 2;
        return (int) (((angle % 360) + 360) % 360 / sector);
    }

    // Outer radius of the ring, shrunk on small windows so the popped-out slot still fits
    private int outerRadius() {
        return Math.max(2 * WHEEL_DEAD_ZONE, Math.min(WHEEL_MAX_RADIUS, Math.min(width, height) / 2 - WHEEL_HOVER_GROW - 12));
    }

    private int innerRadius() {
        return Math.round(outerRadius() * WHEEL_INNER_RATIO);
    }

    private int hoveredSlot(double mx, double my) {
        return segmentAt(mx - width / 2.0, my - height / 2.0, WHEEL_DEAD_ZONE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Light dim only, so the world stays visible behind the wheel
    @Override
    public void extractBackground(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        extractTransparentBackground(extract);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractRenderState(extract, mx, my, delta);
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        int cx = width / 2;
        int cy = height / 2;
        int outer = outerRadius();
        int inner = innerRadius();
        int hovered = hoveredSlot(mx, my);
        drawWheel(extract, cx, cy, inner, outer, hovered);

        // Each slot: the icon of its preset in the middle of the slice, and its number near the outer edge
        for (int i = 0; i < SLOTS; i++) {
            String name = config.getQuickPreset(i);
            double angle = Math.toRadians(i * 360.0 / SLOTS - 90);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            int outerR = outer + (i == hovered ? WHEEL_HOVER_GROW : 0);
            double mid = (inner + outerR) / 2.0;
            ItemStack icon = iconOf(name);
            if (!icon.isEmpty()) {
                extract.item(icon, cx + (int) Math.round(cos * mid) - 8, cy + (int) Math.round(sin * mid) - 8);
            }

            boolean active = name != null && name.equals(config.getActivePreset());
            int numberColor = i == hovered || active ? TEXT_ACTIVE_COLOR : TEXT_MUTED_COLOR;
            double numberR = outerR - font.lineHeight / 2.0 - 3;
            extract.centeredText(font, String.valueOf(i + 1), cx + (int) Math.round(cos * numberR),
                    cy + (int) Math.round(sin * numberR) - font.lineHeight / 2 + 1, numberColor);
        }

        // Center: the pointed preset, or what releasing now would do
        Component center = hovered < 0 ? Component.translatable("label.rnd-block-placer.wheel.cancel")
                : config.getQuickPreset(hovered) == null ? Component.translatable("label.rnd-block-placer.wheel.empty")
                : Component.literal(config.getQuickPreset(hovered));
        int textWidth = 2 * (inner - WHEEL_GAP) - 6;
        int lineY = cy - font.wordWrapHeight(center, textWidth) / 2;
        for (var line : font.split(center, textWidth)) {
            extract.centeredText(font, line, cx, lineY, TEXT_COLOR);
            lineY += font.lineHeight;
        }
        extract.centeredText(font, Component.translatable("label.rnd-block-placer.wheel.hint"),
                cx, cy + outer + WHEEL_HOVER_GROW + 4, TEXT_MUTED_COLOR);
    }

    // Draws the center disc and the ring of slices, one GUI pixel row at a time: each row is cut into
    // runs of the same color and every run is one fill.
    // ponytail: pixel-sized steps, so edges look stepped like the rest of Minecraft's GUI; switch to a
    // custom render pipeline if smooth edges are ever wanted
    private void drawWheel(GuiGraphicsExtractor extract, int cx, int cy, int inner, int outer, int hovered) {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        int[] fill = new int[SLOTS];
        int[] border = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            String name = config.getQuickPreset(i);
            boolean active = name != null && name.equals(config.getActivePreset());
            fill[i] = i == hovered ? WHEEL_HOVER_COLOR : active ? WHEEL_ACTIVE_COLOR : PANEL_COLOR;
            border[i] = i == hovered ? SLOT_SELECTED_BORDER_COLOR : PANEL_BORDER_COLOR;
        }

        int reach = outer + WHEEL_HOVER_GROW;
        for (int dy = -reach; dy < reach; dy++) {
            int runStart = -reach;
            int runColor = 0;
            for (int dx = -reach; dx <= reach; dx++) {
                int color = dx == reach ? 0 : pixelColor(dx + 0.5, dy + 0.5, inner, outer, hovered, fill, border);
                if (color != runColor) {
                    if (runColor != 0) {
                        extract.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    }
                    runStart = dx;
                    runColor = color;
                }
            }
        }
    }

    // Color of the wheel at an offset from its center, or 0 where nothing is drawn (gaps and outside)
    private static int pixelColor(double dx, double dy, int inner, int outer, int hovered, int[] fill, int[] border) {
        double r = Math.sqrt(dx * dx + dy * dy);
        // Center disc, separated from the ring by a gap
        double disc = inner - WHEEL_GAP;
        if (r < disc) {
            return disc - r < 1 ? PANEL_BORDER_COLOR : PANEL_COLOR;
        }
        if (r < inner) {
            return 0;
        }
        int slot = segmentAt(dx, dy, 0);
        double outerR = outer + (slot == hovered ? WHEEL_HOVER_GROW : 0);
        if (r > outerR) {
            return 0;
        }
        // Distance in pixels to the nearest slice edge, along the arc
        double sector = 360.0 / SLOTS;
        double angle = ((Math.toDegrees(Math.atan2(dy, dx)) + 90 + sector / 2) % 360 + 360) % 360;
        double local = angle - slot * sector;
        double edge = Math.toRadians(Math.min(local, sector - local)) * r - WHEEL_GAP / 2.0;
        if (edge < 0) {
            return 0;
        }
        return edge < 1 || r - inner < 1 || outerR - r < 1 ? border[slot] : fill[slot];
    }

    // Item of the preset's heaviest block, or empty when the slot has no preset
    private static ItemStack iconOf(String name) {
        Map<Identifier, Integer> preset = name == null ? null : BlockPlacerConfig.INSTANCE.getPreset(name);
        if (preset == null || preset.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier id = preset.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    // Loads the pointed preset if the slot has one, turns placement on, and returns to the game
    private void confirm(double mx, double my) {
        int slot = hoveredSlot(mx, my);
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        if (slot >= 0 && config.getQuickPreset(slot) != null && config.loadPreset(config.getQuickPreset(slot))) {
            config.save();
            BlockPlacer.INSTANCE.enable();
        }
        onClose();
    }

    private double mouseX() {
        return minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
    }

    private double mouseY() {
        return minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (KeyBindings.INSTANCE.getQuickPresetWheelKey().matches(event)) {
            confirm(mouseX(), mouseY());
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // Same release when the wheel key is bound to a mouse button
        if (KeyBindings.INSTANCE.getQuickPresetWheelKey().matchesMouse(event)) {
            confirm(event.x(), event.y());
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (KeyBindings.INSTANCE.getQuickPresetWheelKey().matchesMouse(event)) {
            return true;
        }
        int slot = hoveredSlot(event.x(), event.y());
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && slot >= 0) {
            minecraft.setScreenAndShow(new QuickPresetPickerScreen(slot));
            return true;
        }
        // Left click also confirms, in case the key was released before the wheel opened
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            confirm(event.x(), event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

}
