package org.pagon.manager;

import org.pagon.features.modules.Module;
import org.pagon.features.modules.client.ColorModule;
import org.pagon.features.modules.client.HudEditorModule;
import org.pagon.features.settings.Setting;
import org.pagon.util.ColorUtil;

import java.awt.*;

public class ColorManager {
    private Color color = new Color(67, 107, 183, 210);

    public void init() {
        refresh();
    }

    public void refresh() {
        ColorModule colorModule = ColorModule.getInstance();
        if (colorModule == null) {
            return;
        }
        this.color = resolve(colorModule.color);
    }

    public Color getColor() {
        refresh();
        return this.color;
    }

    public Color resolve(Setting<Color> setting) {
        if (setting == null) {
            return getColor();
        }

        Setting<?> modeSetting = getModeSetting(setting);
        if (modeSetting == null || !modeSetting.isEnumSetting()) {
            return setting.getValue();
        }

        String mode = modeSetting.currentEnumName();
        if (mode.equalsIgnoreCase("Global")) {
            if (setting.getFeature() instanceof Module module && module.getCategory() == Module.Category.HUD) {
                return getHudColor();
            }
            return getColor();
        }
        if (mode.equalsIgnoreCase("Rainbow")) {
            return getRainbowColor(0, setting.getValue().getAlpha());
        }
        return setting.getValue();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getColorAsInt() {
        return getColor().getRGB();
    }

    public int getColorAsIntFullAlpha() {
        Color current = getColor();
        return new Color(current.getRed(), current.getGreen(), current.getBlue(), 255).getRGB();
    }

    public int getColorWithAlpha(float offset, int alpha) {
        ColorModule colorModule = ColorModule.getInstance();
        if (colorModule != null) {
            Setting<?> modeSetting = getModeSetting(colorModule.color);
            if (modeSetting != null && modeSetting.isEnumSetting() && modeSetting.currentEnumName().equalsIgnoreCase("Rainbow")) {
                return getRainbowColor((int) offset, alpha).getRGB();
            }
        }
        Color current = getColor();
        return new Color(current.getRed(), current.getGreen(), current.getBlue(), alpha).getRGB();
    }

    public Color getRainbowColor(int delay, int alpha) {
        Color rainbow = ColorUtil.rainbow(delay, getRainbowBrightness(), getRainbowContrast());
        return new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), alpha);
    }

    public Color getHudColor() {
        HudEditorModule hudModule = HudEditorModule.getInstance();
        if (hudModule == null) {
            return getColor();
        }

        Setting<?> modeSetting = getModeSetting(hudModule.color);
        if (modeSetting == null || !modeSetting.isEnumSetting()) {
            return hudModule.color.getValue();
        }

        String mode = modeSetting.currentEnumName();
        if (mode.equalsIgnoreCase("Global")) {
            return getColor();
        }
        if (mode.equalsIgnoreCase("Rainbow")) {
            return getRainbowColor(0, hudModule.color.getValue().getAlpha());
        }
        return hudModule.color.getValue();
    }

    private float getRainbowBrightness() {
        ColorModule colorModule = ColorModule.getInstance();
        return colorModule == null ? 1.0f : colorModule.rainbowBrightness.getValue();
    }

    private float getRainbowContrast() {
        ColorModule colorModule = ColorModule.getInstance();
        return colorModule == null ? 1.0f : colorModule.rainbowContrast.getValue();
    }

    private Setting<?> getModeSetting(Setting<Color> setting) {
        if (setting == null || setting.getFeature() == null) {
            return null;
        }
        String scopedName = setting.getName() + "Mode";
        Setting<?> scoped = setting.getFeature().getSettingByName(scopedName);
        if (scoped != null) {
            return scoped;
        }
        return setting.getFeature().getSettingByName("Mode");
    }
}


