package org.pagon.features.gui.items.buttons;


import org.pagon.features.gui.GuiStyle;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class EnumButton
        extends Button {
    public Setting<Enum<?>> setting;

    public EnumButton(Setting<Enum<?>> setting) {
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
        
        String value = this.setting.currentEnumName().equalsIgnoreCase("ABC") ? "ABC" : this.setting.currentEnumName();
        String trimmedValue = ClientFont.trimToWidth(value, Math.max(1, this.width / 2 - 6));
        int valueWidth = ClientFont.guiWidth(trimmedValue);
        int nameMaxWidth = Math.max(1, Math.round(this.width - GuiStyle.SETTING_TEXT_OFFSET - valueWidth - 9.0f));
        
        int nameColor = applyAlpha(GuiStyle.TEXT, renderAlpha);
        int valueColor = applyAlpha(GuiStyle.TEXT_BRIGHT, renderAlpha);
        
        drawString(ClientFont.trimToWidth(this.setting.getName(), nameMaxWidth), this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, nameColor);
        drawString(trimmedValue, this.x + this.width - valueWidth - 4.0f, this.y + 2.0f, valueColor);
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
        this.setting.increaseEnum();
    }

    @Override
    public boolean getState() {
        return true;
    }
}


