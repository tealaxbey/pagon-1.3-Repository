package org.pagon.features.gui.items.buttons;

import org.pagon.features.gui.GuiStyle;
import org.pagon.features.settings.Bind;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class BindButton
        extends Button {
    private final Setting<Bind> setting;
    public boolean isListening;

    public BindButton(Setting<Bind> setting) {
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
        
        if (this.isListening) {
            int listeningColor = applyAlpha(GuiStyle.accent(), renderAlpha);
            drawString("Press a Key...", this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, listeningColor);
        } else {
            String str = this.setting.getValue().toString().toUpperCase();
            str = str.replace("KEY.KEYBOARD", "").replace(".", " ").trim();
            if (str.isBlank() || str.equals("-1")) str = "NONE";
            String trimmedValue = ClientFont.trimToWidth(str, Math.max(1, this.width / 2 - 6));
            int valueWidth = ClientFont.guiWidth(trimmedValue);
            int nameMaxWidth = Math.max(1, Math.round(this.width - GuiStyle.SETTING_TEXT_OFFSET - valueWidth - 9.0f));
            
            int nameColor = applyAlpha(GuiStyle.TEXT, renderAlpha);
            int valueColor = applyAlpha(GuiStyle.TEXT_BRIGHT, renderAlpha);
            
            drawString(ClientFont.trimToWidth(this.setting.getName(), nameMaxWidth), this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, nameColor);
            drawString(trimmedValue, this.x + this.width - valueWidth - 4.0f, this.y + 2.0f, valueColor);
        }
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
        if (this.isListening) {
            if (mouseButton != 0 && mouseButton != 1) {
                this.setting.setValue(new Bind(-mouseButton - 2));
                this.onMouseClick();
            }
        } else if (this.isHovering(mouseX, mouseY)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            Bind bind = new Bind(key);
            if (key == GLFW.GLFW_KEY_DELETE
                    || key == GLFW.GLFW_KEY_BACKSPACE
                    || key == GLFW.GLFW_KEY_ESCAPE) {
                bind = new Bind(-1);
            }
            this.setting.setValue(bind);
            this.onMouseClick();
        }
    }

    @Override
    public void toggle() {
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }
}


