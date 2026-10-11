package org.pagon.features.gui.items.buttons;

import org.pagon.features.gui.PagonGui;
import org.pagon.features.gui.GuiStyle;
import org.pagon.features.gui.Widget;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class Slider
        extends Button {
    private final Number min;
    private final Number max;
    private final double difference;
    public Setting<Number> setting;

    public Slider(Setting<Number> setting) {
        super(setting.getName());
        this.setting = setting;
        this.min = setting.getMin();
        this.max = setting.getMax();
        this.difference = this.max.doubleValue() - this.min.doubleValue();
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        this.height = this.getHeight();
        this.dragSetting(mouseX, mouseY);
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.getHeight(), GuiStyle.PANEL_HOVER);
        }
        
        String value = this.setting.getValue() instanceof Integer
                ? String.valueOf(this.setting.getValue().intValue())
                : formatDecimal(this.setting.getValue().doubleValue());
        String trimmedValue = ClientFont.trimToWidth(value, Math.max(1, this.width / 2 - 6));
        int valueWidth = ClientFont.guiWidth(trimmedValue);
        int nameMaxWidth = Math.max(1, Math.round(this.width - GuiStyle.SETTING_TEXT_OFFSET - valueWidth - 9.0f));
        
        int nameColor = applyAlpha(GuiStyle.TEXT, renderAlpha);
        int valueColor = applyAlpha(GuiStyle.TEXT_BRIGHT, renderAlpha);
        int accentColor = applyAlpha(GuiStyle.accent(), renderAlpha);
        
        drawString(ClientFont.trimToWidth(this.getName(), nameMaxWidth), this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 1.0f, nameColor);
        drawString(trimmedValue, this.x + this.width - valueWidth - 4.0f, this.y + 1.0f, valueColor);
        RenderUtil.rect(context, this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + this.getHeight() - 2.0f,
                this.x + GuiStyle.SETTING_TEXT_OFFSET + (this.width - GuiStyle.SETTING_TEXT_OFFSET - 4.0f) * this.partialMultiplier(),
                this.y + this.getHeight() - 1.0f, accentColor);
    }
    
    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY)) {
            this.setSettingFromX(mouseX);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : PagonGui.getClickGui().getComponents()) {
            if (!widget.drag) continue;
            return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth() && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.getHeight();
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    private void dragSetting(int mouseX, int mouseY) {
        if (this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1) {
            this.setSettingFromX(mouseX);
        }
    }

    private void setSettingFromX(int mouseX) {
        float percent = Math.max(0.0f, Math.min(1.0f, ((float) mouseX - this.x) / (float) this.width));
        if (this.setting.getValue() instanceof Double) {
            double result = this.setting.getMin().doubleValue() + this.difference * percent;
            this.setting.setValue(roundToStep(result));
        } else if (this.setting.getValue() instanceof Float) {
            double result = this.setting.getMin().doubleValue() + this.difference * percent;
            this.setting.setValue((float) roundToStep(result));
        } else if (this.setting.getValue() instanceof Integer) {
            this.setting.setValue((Integer) this.setting.getMin() + (int) Math.round(this.difference * percent));
        }
    }

    private double roundToStep(double value) {
        double step = Math.abs(this.difference) <= 0.1 ? 1000.0 : 10.0;
        return Math.round(value * step) / step;
    }

    private String formatDecimal(double value) {
        double rounded = roundToStep(value);
        if (Math.abs(this.difference) <= 0.1) {
            return String.format(java.util.Locale.ROOT, "%.3f", rounded);
        }
        return String.valueOf(Math.round(rounded * 10.0) / 10.0);
    }

    private float middle() {
        return this.max.floatValue() - this.min.floatValue();
    }

    private float part() {
        return (this.setting.getValue()).floatValue() - this.min.floatValue();
    }

    private float partialMultiplier() {
        return this.part() / this.middle();
    }
}


