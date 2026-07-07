package capi.rnd_block_placer.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static capi.rnd_block_placer.client.screen.BlockSelectionScreenConstant.*;

public class BlockSelectionScreenRenderer {
    private int leftPos;
    private int topPos;

    private Font font;

    private Minecraft mc;

    private BlockSelectionScreenState currentState = null;

    public void init(int leftPos, int topPos, Font font, Minecraft mc) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.font = font;
        this.mc = mc;
    }

    public void setCurrentState(BlockSelectionScreenState currentState) {
        this.currentState = currentState;
    }

    private void drawBackground(GuiGraphicsExtractor extract) {
        extract.blit(
                RenderPipelines.GUI_TEXTURED,
                BlockSelectionScreenConstant.CONTAINER_BG,
                leftPos, topPos,
                0.0f, 0.0f,
                IMAGE_W, IMAGE_H,
                256, 256
        );
    }

    private ItemStack findStackFor(Identifier id, LocalPlayer p) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() instanceof BlockItem
                    && BuiltInRegistries.ITEM.getKey(st.getItem()).equals(id)) {
                return st;
            }
        }
        return null;
    }

    private void drawSelectedList(GuiGraphicsExtractor extract) {
        if (currentState.getWorkingWeights().isEmpty()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        int panelX = leftPos - 115;
        int panelY = topPos + 40;

        extract.text(font, "§lSelected:", panelX, panelY - 14, 0xFFCCCCCC);

        int totalWeight = currentState.getWorkingWeights().values().stream().mapToInt(Integer::intValue).sum();

        List<Map.Entry<Identifier, Integer>> workingWeightSorted = new ArrayList<>(currentState.getWorkingWeights().entrySet());
        workingWeightSorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int maxY = topPos + IMAGE_H - 10;
        int yOff = panelY;
        int shown = 0;
        int maxRows = (maxY - panelY) / 20;

        for (Map.Entry<Identifier, Integer> entry : workingWeightSorted) {
            Identifier id = entry.getKey();
            int weight = entry.getValue();
            ItemStack st = findStackFor(id, player);
            if (st == null) continue;

            shown++;
            if (shown > maxRows) {
                int rem = workingWeightSorted.size() - shown + 1;
                extract.text(font, "§7+ " + rem + " more", panelX, yOff, 0xFF888888);
                break;
            }

            extract.item(st, panelX, yOff);
            extract.text(font, Component.literal("§f" + weight * 100 / totalWeight + "%"), panelX + 18, yOff + 4, 0xFFFFFFFF);
            yOff += 20;
        }
    }

    private void drawInventorySlots(GuiGraphicsExtractor extract, int mx, int my) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        for (int row = 0; row < INVENTORY_ROW; row++) {

            for (int col = 0; col < INVENTORY_COL; col++) {

                int slotIndex = (row == 3) ? col : 9 + row * INVENTORY_COL + col;
                ItemStack itemStack = player.getInventory().getItem(slotIndex);

                boolean isBlock = itemStack.getItem() instanceof BlockItem;
                Identifier blockId = isBlock ? BuiltInRegistries.ITEM.getKey(itemStack.getItem()) : null;

                boolean selected = blockId != null && currentState.getWorkingWeights().containsKey(blockId);

                int x = leftPos + SLOT_X + col * SLOT_SIZE;
                int yOff = (row == 3) ? HOTBAR_Y : MAIN_Y + row * SLOT_SIZE;
                int y = topPos + yOff;

                if (selected) {
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, GREEN_SEL);
                } else {
                    extract.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x, y, SLOT_SIZE, SLOT_SIZE);
                }

                if (!itemStack.isEmpty()) {
                    extract.item(itemStack, x + 1, y + 1);
                    if (selected) {
                        int w = currentState.getWorkingWeights().get(blockId);
                        String weightStr = "§l" + w;
                        int tw = font.width(weightStr);
                        extract.text(font, weightStr, x + SLOT_SIZE - tw - 2, y + 2, 0xFFFFFF);
                    }
                }

                if (!itemStack.isEmpty() && !isBlock) {
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, GRAY_OVERLAY);
                }

                boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
                if (hovered) {
                    extract.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, HOVER);
                }
            }
        }

//        // Draw weight label above the EditBox
//        if (weightInput.isVisible() && editingSlot != null) {
//            extract.text(font, weightLabel, leftPos + SLOT_X, topPos + IMAGE_H - 4 - 10, 0xCCCCCC);
//        }
    }

    public void render(GuiGraphicsExtractor extract, int mx, int my, float delta) {
        if  (currentState == null) {
            return;
        }

        drawBackground(extract);
        drawSelectedList(extract);
        drawInventorySlots(extract, mx, my);
    }
}
