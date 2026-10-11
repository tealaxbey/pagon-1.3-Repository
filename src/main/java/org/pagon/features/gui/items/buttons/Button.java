package org.pagon.features.gui.items.buttons;

import org.pagon.features.gui.PagonGui;
import org.pagon.features.gui.GuiStyle;
import org.pagon.features.gui.Widget;
import org.pagon.features.gui.items.Item;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Button
        extends Item {
    private boolean state;

    public Button(String name) {
        super(name);
        this.height = GuiStyle.itemHeight();
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.getHeight(), GuiStyle.PANEL_HOVER);
        }
        drawString(ClientFont.trimToWidth(this.getName(), this.width - 6), this.x + 3.0f, this.y + 2.0f, GuiStyle.text(this.getState()));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    public void toggle() {
    }

    public boolean getState() {
        return this.state;
    }

    @Override
    public int getHeight() {
        return GuiStyle.itemHeight();
    }

    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : PagonGui.getClickGui().getComponents()) {
            if (!widget.drag) continue;
            return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth() && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.getHeight();
    }
}


