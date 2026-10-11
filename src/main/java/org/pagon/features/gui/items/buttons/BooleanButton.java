package org.pagon.features.gui.items.buttons;


import org.pagon.features.gui.GuiStyle;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class BooleanButton
        extends Button {
    private final Setting<Boolean> setting;

    public BooleanButton(Setting<Boolean> setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        this.height = this.getHeight();
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.getHeight(), GuiStyle.PANEL_HOVER);
        }
        
        int textColor = applyAlpha(GuiStyle.text(this.getState()), renderAlpha);
        drawString(this.getName(), this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, textColor);
    }
    
    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    @Override
    public void toggle() {
        this.setting.setValue(!this.setting.getValue());
    }

    @Override
    public boolean getState() {
        return this.setting.getValue();
    }
}


