package org.pagon.features.modules.client;

import org.pagon.Pagon;
import org.pagon.event.impl.ClientEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;

import java.awt.Color;

public class ColorModule extends Module {
    private static ColorModule INSTANCE;

    public final Setting<Color> color = color("Color", 67, 107, 183, 255);
    public final Setting<Color> gColor1 = color("GColor1", 67, 107, 183, 255);
    public final Setting<Color> gColor2 = color("GColor2", 51, 87, 155, 255);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Custom);
    public final Setting<ColorSettingMode> gColor1Mode = mode("GColor1Mode", ColorSettingMode.Custom);
    public final Setting<ColorSettingMode> gColor2Mode = mode("GColor2Mode", ColorSettingMode.Custom);
    public final Setting<Float> rainbowBrightness = num("RainbowBrightness", 1.0f, 0.0f, 2.0f);
    public final Setting<Float> rainbowContrast = num("RainbowContrast", 1.0f, 0.0f, 2.0f);

    public ColorModule() {
        super("Color", "Global client color", Category.CLIENT);
        INSTANCE = this;
        this.colorMode.setVisibility(v -> false);
        this.gColor1Mode.setVisibility(v -> false);
        this.gColor2Mode.setVisibility(v -> false);
        this.rainbowBrightness.setVisibility(v -> isRainbowActive());
        this.rainbowContrast.setVisibility(v -> isRainbowActive());
        enable();
    }

    public static ColorModule getInstance() {
        return INSTANCE;
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() != ClientEvent.Type.SETTING_UPDATE || event.getSetting().getFeature() != this) {
            return;
        }
        Pagon.colorManager.refresh();
    }

    @Override
    public void onLoad() {
        Pagon.colorManager.refresh();
    }

    @Override
    public boolean isAlwaysEnabled() {
        return true;
    }

    private boolean isRainbowActive() {
        return isRainbow(colorMode) || isRainbow(gColor1Mode) || isRainbow(gColor2Mode);
    }

    private boolean isRainbow(Setting<ColorSettingMode> mode) {
        return mode.getValue() == ColorSettingMode.Rainbow;
    }
}


