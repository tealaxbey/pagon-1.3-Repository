package org.pagon.features.modules.client;

import org.pagon.features.gui.HudEditorScreen;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;

import java.awt.Color;

public class HudEditorModule extends Module {
    private static HudEditorModule INSTANCE;

    public final Setting<Color> color = color("Color", 255, 255, 255, 255);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);

    public HudEditorModule() {
        super("Hud", "Edit HUD element positions", Category.CLIENT);
        INSTANCE = this;
        this.colorMode.setVisibility(v -> false);
    }

    public static HudEditorModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        mc.setScreen(HudEditorScreen.getInstance());
        disable();
    }
}



