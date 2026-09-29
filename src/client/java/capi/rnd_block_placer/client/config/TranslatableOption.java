package capi.rnd_block_placer.client.config;

import net.minecraft.network.chat.Component;

import java.util.Locale;

// Enum setting whose values are labelled by the translation keys "<prefix>.<value>" and "<prefix>.<value>.tooltip"
public interface TranslatableOption {
    // Implemented by every enum
    String name();

    // Translation key of the setting itself, e.g. "options.rnd-block-placer.save_mode"
    String translationPrefix();

    // Short name shown on the settings cycle button
    default Component label() {
        return Component.translatable(translationPrefix() + "." + name().toLowerCase(Locale.ROOT));
    }

    // Longer explanation shown as the settings button tooltip
    default Component description() {
        return Component.translatable(translationPrefix() + "." + name().toLowerCase(Locale.ROOT) + ".tooltip");
    }
}
