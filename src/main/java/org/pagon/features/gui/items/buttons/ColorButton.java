package org.pagon.features.gui.items.buttons;

import org.pagon.Pagon;
import org.pagon.features.gui.PagonGui;
import org.pagon.features.gui.GuiStyle;
import org.pagon.features.gui.Widget;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.Animation;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.awt.*;

public class ColorButton extends Button {
    private static final int HEADER_HEIGHT = 12;
    private static final int MODE_HEIGHT = 14;
    private static final int HUE_HEIGHT = 8;
    private static final int ALPHA_WIDTH = 6;

    private final Setting<Color> setting;
    private Setting<? extends Enum<?>> modeSetting;
    private boolean open;
    private final Animation openAnimation = new Animation(0.4f, Animation.Easing.EASE_OUT_CUBIC);
    private boolean hoveringHue, hoveringColor, hoveringAlpha;
    private boolean draggingHue, draggingColor, draggingAlpha;
    private float[] hsb;

    public ColorButton(Setting<Color> setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
        this.hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), null);
        ensureModeSetting();
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        Color preview = previewColor();
        this.hsb = Color.RGBtoHSB(preview.getRed(), preview.getGreen(), preview.getBlue(), null);

        // Update animation
        openAnimation.setTarget(this.open && isCustomMode() ? 1.0f : 0.0f);
        openAnimation.update(partialTicks);
        float animProgress = openAnimation.getValue();

        if (animProgress < 0.01f) {
            drawClosed(context, mouseX, mouseY, preview);
            return;
        }

        int left = Math.round(this.x);
        int top = Math.round(this.y);
        int right = Math.round(this.x + this.width);
        int headerHeight = headerHeight();
        int modeY = top + headerHeight;
        int pickerX = left + 2;
        int pickerSize = Math.max(20, this.width - 6 - ALPHA_WIDTH);
        int pickerY = modeY + MODE_HEIGHT;
        int alphaX = pickerX + pickerSize + 1;
        
        // Calculate animated height
        int fullHeight = headerHeight + MODE_HEIGHT + pickerSize + 4 + HUE_HEIGHT + 3;
        int closedHeight = GuiStyle.itemHeight();
        int animatedHeight = (int) (closedHeight + (fullHeight - closedHeight) * animProgress);

        // Background with fade
        int bgAlpha = (int) (animProgress * 224);
        RenderUtil.rect(context, left, top, right, top + animatedHeight, (bgAlpha << 24) | 0x101010);
        RenderUtil.rect(context, left, top, right, top + headerHeight, GuiStyle.accent());
        
        // Border with fade
        int borderAlpha = (int) (animProgress * 255);
        RenderUtil.rect(context, left, top, right, top + animatedHeight, (GuiStyle.accentBorder() & 0x00FFFFFF) | (borderAlpha << 24), 1.0f);
        
        // Header text with fade
        int textAlpha = (int) (animProgress * 255);
        int headerTextColor = (GuiStyle.TEXT_BRIGHT & 0x00FFFFFF) | (textAlpha << 24);
        context.pose().pushMatrix();
        context.drawString(mc.font, ClientFont.trimToWidth(getColorTitle(), this.width - 6), left + 3, top + 2, headerTextColor);
        context.pose().popMatrix();

        // Mode section with fade - faster fade-in
        if (animProgress > 0.05f) {
            float modeProgress = Math.min(1.0f, (animProgress - 0.05f) / 0.25f); // Faster: 0.05-0.30 range
            int modeAlpha = (int) (modeProgress * modeProgress * 255); // Squared for speed
            RenderUtil.rect(context, left + 1, modeY, right - 1, modeY + MODE_HEIGHT, (modeAlpha << 24) | 0x181515);
            
            String trimmedModeName = ClientFont.trimToWidth(getModeName(), Math.max(1, this.width / 2 - 6));
            int modeWidth = ClientFont.guiWidth(trimmedModeName);
            int modeTextColor = (GuiStyle.TEXT & 0x00FFFFFF) | (modeAlpha << 24);
            int modeBrightColor = (GuiStyle.TEXT_BRIGHT & 0x00FFFFFF) | (modeAlpha << 24);
            
            context.pose().pushMatrix();
            context.drawString(mc.font, "Mode", left + 4, modeY + 3, modeTextColor);
            context.drawString(mc.font, trimmedModeName, right - modeWidth - 4, modeY + 3, modeBrightColor);
            context.pose().popMatrix();
        }

        if (isCustomMode() && animProgress > 0.15f) {
            float pickerProgress = Math.min(1.0f, (animProgress - 0.15f) / 0.35f); // Faster: 0.15-0.50 range
            float pickerAlpha = pickerProgress * pickerProgress; // Squared for speed
            
            int dragX = Mth.clamp(mouseX - pickerX, 0, pickerSize);
            int dragY = Mth.clamp(mouseY - pickerY, 0, pickerSize);
            int alphaDragY = Mth.clamp(mouseY - pickerY, 0, pickerSize);

            Color hueColor = Color.getHSBColor(hsb[0], 1.0f, 1.0f);
            RenderUtil.horizontalGradient(context, pickerX, pickerY, pickerX + pickerSize, pickerY + pickerSize, Color.WHITE, hueColor);
            RenderUtil.verticalGradient(context, pickerX, pickerY, pickerX + pickerSize, pickerY + pickerSize, new Color(0, 0, 0, 0), Color.BLACK);
            RenderUtil.rect(context, pickerX, pickerY, pickerX + pickerSize, pickerY + pickerSize, 0xFF000000, 1.0f);

            drawChecker(context, alphaX, pickerY, alphaX + ALPHA_WIDTH, pickerY + pickerSize);
            RenderUtil.verticalGradient(context, alphaX, pickerY, alphaX + ALPHA_WIDTH, pickerY + pickerSize,
                    new Color(preview.getRed(), preview.getGreen(), preview.getBlue(), 255),
                    new Color(preview.getRed(), preview.getGreen(), preview.getBlue(), 0));
            RenderUtil.rect(context, alphaX, pickerY, alphaX + ALPHA_WIDTH, pickerY + pickerSize, 0xFF000000, 1.0f);

            float satX = pickerX + hsb[1] * pickerSize;
            float briY = pickerY + (1.0f - hsb[2]) * pickerSize;
            int dotAlpha = (int) (pickerAlpha * 255);
            RenderUtil.rect(context, satX - 1, briY - 1, satX + 1, briY + 1, 0xFFFFFFFF);
            RenderUtil.rect(context, satX - 2, briY - 2, satX + 2, briY + 2, (dotAlpha << 24), 1.0f);

            float alphaY = pickerY + (1.0f - preview.getAlpha() / 255.0f) * pickerSize;
            RenderUtil.rect(context, alphaX - 1, alphaY - 1, alphaX + ALPHA_WIDTH + 1, alphaY + 1, 0xFFFFFFFF);
            RenderUtil.rect(context, alphaX - 1, alphaY - 1, alphaX + ALPHA_WIDTH + 1, alphaY + 1, (dotAlpha << 24), 1.0f);

            int hueY = pickerY + pickerSize + 4;
            for (int i = 0; i < this.width - 4; i++) {
                RenderUtil.rect(context, left + 2 + i, hueY, left + 3 + i, hueY + HUE_HEIGHT,
                        Color.getHSBColor(i / (float) (this.width - 4), 1.0f, 1.0f).getRGB());
            }
            RenderUtil.rect(context, left + 2, hueY, right - 2, hueY + HUE_HEIGHT, 0xFF000000, 1.0f);
            float hueX = left + 2 + hsb[0] * (this.width - 4);
            RenderUtil.rect(context, hueX - 1, hueY - 1, hueX + 1, hueY + HUE_HEIGHT + 1, 0xFFFFFFFF);
            RenderUtil.rect(context, hueX - 2, hueY - 1, hueX + 2, hueY + HUE_HEIGHT + 1, (dotAlpha << 24), 1.0f);

            hoveringColor = isHoveringArea(mouseX, mouseY, pickerX, pickerY, pickerX + pickerSize, pickerY + pickerSize);
            hoveringAlpha = isHoveringArea(mouseX, mouseY, alphaX, pickerY, alphaX + ALPHA_WIDTH, pickerY + pickerSize);
            hoveringHue = isHoveringArea(mouseX, mouseY, left + 2, hueY, right - 2, hueY + HUE_HEIGHT);

            if (draggingColor) {
                hsb[1] = dragX / (float) pickerSize;
                hsb[2] = 1.0f - dragY / (float) pickerSize;
                setColor(hsb);
            }
            if (draggingAlpha) {
                setColor(hsb, (int) (255.0f * (1.0f - alphaDragY / (float) pickerSize)));
            }
            if (draggingHue) {
                hsb[0] = Mth.clamp((mouseX - (left + 2)) / (float) (this.width - 4), 0.0f, 1.0f);
                setColor(hsb);
            }
        } else {
            hoveringColor = hoveringAlpha = hoveringHue = false;
        }
    }

    private void drawClosed(GuiGraphics context, int mouseX, int mouseY, Color preview) {
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.getHeight(), GuiStyle.PANEL_HOVER);
        }
        String modeName = getModeName();
        int modeWidth = ClientFont.guiWidth(modeName);
        int previewStart = Math.round(this.x + this.width - 11.0f);
        int nameMaxWidth = Math.max(1, Math.round(previewStart - (this.x + GuiStyle.SETTING_TEXT_OFFSET) - (this.isHovering(mouseX, mouseY) ? modeWidth + 8.0f : 4.0f)));
        drawString(ClientFont.trimToWidth(this.getName(), nameMaxWidth), this.x + GuiStyle.SETTING_TEXT_OFFSET, this.y + 2.0f, GuiStyle.TEXT);
        RenderUtil.rect(context, this.x + this.width - 11.0f, this.y + 3.0f,
                this.x + this.width - 4.0f, this.y + this.getHeight() - 3.0f,
                new Color(preview.getRGB(), false).getRGB());
        RenderUtil.rect(context, this.x + this.width - 11.0f, this.y + 3.0f,
                this.x + this.width - 4.0f, this.y + this.getHeight() - 3.0f, 0xFF000000, 1.0f);
        if (this.isHovering(mouseX, mouseY)) {
            String trimmedMode = ClientFont.trimToWidth(modeName, Math.max(1, this.width / 2 - 6));
            drawString(trimmedMode, this.x + this.width - ClientFont.guiWidth(trimmedMode) - 18.0f, this.y + 2.0f, GuiStyle.TEXT_BRIGHT);
        }
    }

    private String getColorTitle() {
        return this.setting.getName().equalsIgnoreCase("Color") ? "Global Color" : this.setting.getName();
    }

    private String getModeName() {
        Setting<? extends Enum<?>> mode = getModeSetting();
        if (mode == null) {
            return "Custom";
        }
        String name = mode.currentEnumName();
        if (!isGlobalAllowed() && name.equalsIgnoreCase("Global")) {
            return "Custom";
        }
        return name;
    }

    private boolean isCustomMode() {
        Setting<? extends Enum<?>> mode = getModeSetting();
        if (mode == null) {
            return true;
        }
        String name = mode.currentEnumName();
        if (!isGlobalAllowed() && name.equalsIgnoreCase("Global")) {
            return true;
        }
        return name.equalsIgnoreCase("Custom");
    }

    private boolean isRainbowMode() {
        Setting<? extends Enum<?>> mode = getModeSetting();
        if (mode == null) {
            return false;
        }
        return mode.currentEnumName().equalsIgnoreCase("Rainbow");
    }

    private boolean isGlobalAllowed() {
        return this.setting.getFeature() == null || !this.setting.getFeature().getName().equalsIgnoreCase("Color");
    }

    private Color previewColor() {
        if (isRainbowMode()) {
            return Pagon.colorManager.getRainbowColor(0, this.setting.getValue().getAlpha());
        }

        Setting<? extends Enum<?>> mode = getModeSetting();
        if (mode != null && mode.currentEnumName().equalsIgnoreCase("Global")) {
            Color global = Pagon.colorManager.getColor();
            return new Color(global.getRed(), global.getGreen(), global.getBlue(), this.setting.getValue().getAlpha());
        }

        return this.setting.getValue();
    }

    private void drawChecker(GuiGraphics context, int left, int top, int right, int bottom) {
        int size = 3;
        for (int x = left; x < right; x += size) {
            for (int y = top; y < bottom; y += size) {
                boolean light = ((x - left) / size + (y - top) / size) % 2 == 0;
                RenderUtil.rect(context, x, y, Math.min(x + size, right), Math.min(y + size, bottom),
                        light ? 0xFFFFFFFF : 0xFFB0B0B0);
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isModeHovering(mouseX, mouseY)) {
            cycleMode();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return;
        }

        if (this.isHovering(mouseX, mouseY) && mouseButton == 1) {
            if (isCustomMode()) {
                open = !open;
            }
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return;
        }

        if (mouseButton == 0) {
            if (!isCustomMode()) {
                return;
            }

            if (hoveringHue) draggingHue = true;
            if (hoveringColor) draggingColor = true;
            if (hoveringAlpha) draggingAlpha = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            draggingHue = false;
            draggingColor = false;
            draggingAlpha = false;
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
        if (!isCustomMode()) {
            open = false;
        }
    }

    @Override
    public int getHeight() {
        openAnimation.setTarget(this.open && isCustomMode() ? 1.0f : 0.0f);
        float animProgress = openAnimation.getRawValue();
        
        if (animProgress < 0.01f) {
            return GuiStyle.itemHeight();
        }
        
        int pickerSize = Math.max(20, this.width - 6 - ALPHA_WIDTH);
        int fullHeight = headerHeight() + MODE_HEIGHT + pickerSize + 4 + HUE_HEIGHT + 3;
        int closedHeight = GuiStyle.itemHeight();
        
        return (int) (closedHeight + (fullHeight - closedHeight) * animProgress);
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : PagonGui.getClickGui().getComponents()) {
            if (widget.drag) return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth()
                && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.getHeight();
    }

    private boolean isHoveringArea(int mouseX, int mouseY, float left, float top, float right, float bottom) {
        for (Widget widget : PagonGui.getClickGui().getComponents()) {
            if (widget.drag) return false;
        }
        return left <= mouseX && top <= mouseY && right > mouseX && bottom > mouseY;
    }

    private boolean isModeHovering(int mouseX, int mouseY) {
        if (open && isCustomMode()) {
            int top = Math.round(this.y);
            int headerHeight = headerHeight();
            return mouseX >= this.x && mouseX <= this.x + this.width
                    && mouseY >= top + headerHeight && mouseY <= top + headerHeight + MODE_HEIGHT;
        }
        return this.isHovering(mouseX, mouseY) && mouseX >= this.x + this.width / 2.0f;
    }

    private void cycleMode() {
        Setting<? extends Enum<?>> mode = getModeSetting();
        if (mode == null) {
            return;
        }
        if (isGlobalAllowed()) {
            mode.increaseEnum();
        } else {
            String current = mode.currentEnumName();
            mode.setEnumValue(current.equalsIgnoreCase("Custom") ? "Rainbow" : "Custom");
        }
        Pagon.colorManager.refresh();
    }

    @SuppressWarnings("unchecked")
    private Setting<? extends Enum<?>> getModeSetting() {
        if (this.modeSetting != null) {
            return this.modeSetting;
        }
        if (this.setting.getFeature() == null) {
            return null;
        }
        String scopedModeName = this.setting.getName() + "Mode";
        for (Setting<?> sibling : this.setting.getFeature().getSettings()) {
            if (sibling.isEnumSetting() && sibling != this.setting && sibling.getName().equalsIgnoreCase(scopedModeName)) {
                this.modeSetting = (Setting<? extends Enum<?>>) sibling;
                return this.modeSetting;
            }
        }
        for (Setting<?> sibling : this.setting.getFeature().getSettings()) {
            if (sibling.isEnumSetting() && sibling != this.setting && sibling.getName().equalsIgnoreCase("Mode")) {
                this.modeSetting = (Setting<? extends Enum<?>>) sibling;
                return this.modeSetting;
            }
        }
        return ensureModeSetting();
    }

    @SuppressWarnings("unchecked")
    private Setting<? extends Enum<?>> ensureModeSetting() {
        if (this.setting.getFeature() == null) {
            return null;
        }
        String scopedModeName = this.setting.getName() + "Mode";
        Setting<?> existing = this.setting.getFeature().getSettingByName(scopedModeName);
        if (existing != null && existing.isEnumSetting()) {
            this.modeSetting = (Setting<? extends Enum<?>>) existing;
            return this.modeSetting;
        }
        Setting<?> legacy = this.setting.getFeature().getSettingByName("Mode");
        if (legacy != null && legacy.isEnumSetting()) {
            this.modeSetting = (Setting<? extends Enum<?>>) legacy;
            return this.modeSetting;
        }

        Setting<ColorSettingMode> created = this.setting.getFeature().mode(scopedModeName, ColorSettingMode.Custom);
        created.setVisibility(v -> false);
        this.modeSetting = created;
        return this.modeSetting;
    }

    private void setColor(float[] hsb) {
        setColor(hsb, setting.getValue().getAlpha());
    }

    private void setColor(float[] hsb, int alpha) {
        Color color = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        setting.setValue(new Color(color.getRed(), color.getGreen(), color.getBlue(), Mth.clamp(alpha, 0, 255)));
        Pagon.colorManager.refresh();
    }

    private int headerHeight() {
        return HEADER_HEIGHT;
    }
}


