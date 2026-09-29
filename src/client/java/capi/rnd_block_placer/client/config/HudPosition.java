package capi.rnd_block_placer.client.config;

// Where the HUD indicator is drawn while random placement is on
public enum HudPosition implements TranslatableOption {
    TOP_CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    ABOVE_HOTBAR,
    HIDDEN;

    @Override
    public String translationPrefix() {
        return "options.rnd-block-placer.hud_position";
    }
}
