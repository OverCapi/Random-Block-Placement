package capi.rnd_block_placer.client.config;

import net.minecraft.network.chat.Component;

import java.util.Locale;

// When changes made in the selection screen are written to the config
public enum SaveMode {
    // Changes are applied automatically when the screen is closed
    ON_CLOSE,
    // Changes must be applied with the "Apply" button; closing discards them
    MANUAL;

    // Short name shown on the settings cycle button
    public Component label() {
        return Component.translatable("options.rnd-block-placer.save_mode." + key());
    }

    // Longer explanation shown as the settings button tooltip
    public Component description() {
        return Component.translatable("options.rnd-block-placer.save_mode." + key() + ".tooltip");
    }

    private String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
