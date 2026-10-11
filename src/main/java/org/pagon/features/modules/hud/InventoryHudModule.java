package org.pagon.features.modules.hud;

import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.gui.GuiStyle;
import org.pagon.features.modules.client.HudModule;
import org.pagon.util.render.RenderUtil;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;

public class InventoryHudModule extends HudModule {
    public InventoryHudModule() {
        super("Inventory", "Display the player inventory", 162, 54);
        this.pos.setValueNoEvent(new Vector2f(0.601f, 0.894f));
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        float x = getX();
        float y = getY();
        int slotSize = 16;
        int spacing = 2;
        int columns = 9;
        int rows = 3;
        float width = columns * slotSize + (columns - 1) * spacing;
        float height = rows * slotSize + (rows - 1) * spacing;

        RenderUtil.rect(e.getContext(), x - 3, y - 3, x + width + 3, y + height + 3, GuiStyle.PANEL_DARK);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slot = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getItem(slot);
                float slotX = x + col * (slotSize + spacing);
                float slotY = y + row * (slotSize + spacing);
                RenderUtil.rect(e.getContext(), slotX - 1, slotY - 1, slotX + slotSize + 1, slotY + slotSize + 1, 0x50000000);
                if (!stack.isEmpty()) {
                    e.getContext().renderItem(stack, Math.round(slotX), Math.round(slotY));
                    e.getContext().renderItemDecorations(mc.font, stack, Math.round(slotX), Math.round(slotY));
                }
            }
        }

        setWidth(width);
        setHeight(height);
    }
}


