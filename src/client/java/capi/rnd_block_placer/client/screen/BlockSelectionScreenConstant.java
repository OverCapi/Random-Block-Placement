package capi.rnd_block_placer.client.screen;

import net.minecraft.resources.Identifier;

public class BlockSelectionScreenConstant {
    public static final Identifier CONTAINER_BG =
            Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    public static final Identifier SLOT =
            Identifier.withDefaultNamespace("container/slot");

    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 166;

    public static final int INVENTORY_ROW = 4;
    public static final int INVENTORY_COL = 9;

    public static final int SLOT_SIZE = 18;
    public static final int SLOT_X = 8;
    public static final int MAIN_Y = 84;
    public static final int HOTBAR_Y = 142;

    public static final int GREEN_SEL = 0xFF00CC00;
    public static final int GRAY_OVERLAY = 0x90808080;
    public static final int HOVER = 0x50FFFFFF;
}
