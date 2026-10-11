package org.pagon.features.gui.items.buttons;

import org.pagon.features.gui.GuiStyle;
import org.pagon.features.settings.Setting;
import org.pagon.util.models.Timer;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class StringButton
        extends Button {
    private static final Timer idleTimer = new Timer();
    private static boolean idle;
    private final Setting<String> setting;
    public boolean isListening;
    private CurrentString currentString = new CurrentString("");

    public StringButton(Setting<String> setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    public static String removeLastChar(String str) {
        String output = "";
        if (str != null && !str.isEmpty()) {
            output = str.substring(0, str.length() - 1);
        }
        return output;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        this.height = this.getHeight();
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.getHeight(), GuiStyle.PANEL_HOVER);
        }
        
        if (this.isListening) {
            int listeningColor = applyAlpha(GuiStyle.accent(), renderAlpha);
            String listening = ClientFont.trimToWidth(this.currentString.string() + getIdleSign(), this.width - 8);
            drawString(listening, this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, listeningColor);
        } else {
            String value = String.valueOf(this.setting.getValue());
            String trimmedValue = ClientFont.trimToWidth(value, Math.max(1, this.width / 2 - 6));
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
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    @Override
    public void onKeyTyped(String typedChar, int keyCode) {
        if (this.isListening) {
            this.setString(this.currentString.string() + typedChar);
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (isListening) {
            switch (key) {
                case GLFW.GLFW_KEY_ENTER: {
                    this.enterString();
                }
                case GLFW.GLFW_KEY_BACKSPACE: {
                    this.setString(StringButton.removeLastChar(this.currentString.string()));
                }
            }
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    private void enterString() {
        if (this.currentString.string().isEmpty()) {
            this.setting.setValue(this.setting.getDefaultValue());
        } else {
            this.setting.setValue(this.currentString.string());
        }
        this.setString("");
        this.onMouseClick();
    }

    @Override
    public void toggle() {
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }

    public void setString(String newString) {
        this.currentString = new CurrentString(newString);
    }

    public static String getIdleSign() {
        if (idleTimer.passedMs(500)) {
            idle = !idle;
            idleTimer.reset();
        }
        if (idle) return "_";
        return "";
    }

    public record CurrentString(String string) {
    }
}


