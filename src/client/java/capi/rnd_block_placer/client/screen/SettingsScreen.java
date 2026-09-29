package capi.rnd_block_placer.client.screen;

import capi.rnd_block_placer.client.config.BlockPlacerConfig;
import capi.rnd_block_placer.client.config.SaveMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Mod settings, opened from the selection screen; returns to it when closed
public class SettingsScreen extends Screen {
    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;
    private int topY;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("screen.rnd-block-placer.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BlockPlacerConfig config = BlockPlacerConfig.INSTANCE;
        int x = (width - WIDGET_WIDTH) / 2;
        topY = height / 2 - ROW_SPACING;

        addRenderableWidget(CycleButton.builder(SaveMode::label, config.getSaveMode())
                .withValues(SaveMode.values())
                .withTooltip(mode -> Tooltip.create(mode.description()))
                .create(x, topY, WIDGET_WIDTH, WIDGET_HEIGHT, Component.translatable("options.rnd-block-placer.save_mode"),
                        (button, mode) -> {
                            config.setSaveMode(mode);
                            config.save();
                        }));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, topY + 2 * ROW_SPACING, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        super.extractRenderState(extract, mx, my, delta);
        extract.centeredText(font, title, width / 2, topY - ROW_SPACING, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }
}
