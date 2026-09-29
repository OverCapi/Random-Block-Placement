package capi.rnd_block_placer.client.config;

// What random placement does when some selected blocks are no longer in the inventory
public enum MissingBlockMode implements TranslatableOption {
    // Keep placing with the blocks that are left; stop only when none are left
    CONTINUE,
    // Same as CONTINUE, without the chat warning when a block runs out
    CONTINUE_QUIET,
    // Turn random placement off as soon as one selected block is missing
    DISABLE;

    @Override
    public String translationPrefix() {
        return "options.rnd-block-placer.missing_block";
    }
}
