package capi.rnd_block_placer.client.config;

// When changes made in the selection screen are written to the config
public enum SaveMode implements TranslatableOption {
    // Changes are applied automatically when the screen is closed
    ON_CLOSE,
    // Changes must be applied with the "Apply" button; closing discards them
    MANUAL;

    @Override
    public String translationPrefix() {
        return "options.rnd-block-placer.save_mode";
    }
}
