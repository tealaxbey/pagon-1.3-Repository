package org.pagon.features.modules.hud;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.modules.client.HudModule;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.BuildConfig;
import org.pagon.util.render.ClientFont;
import java.awt.Color;

public class WatermarkHudModule extends HudModule {
    public Setting<String> text = str("Text", BuildConfig.NAME);
    public Setting<Boolean> space = bool("Space", true);
    public Setting<Color> color = color("Color", 255, 255, 255, 255);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);

    public WatermarkHudModule() {
        super("Watermark", "Display watermark", 100, 10);
        this.pos.setValueNoEvent(new org.joml.Vector2f(0.0f, 0.0f));
        this.colorMode.setVisibility(v -> false);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        String watermark = text.getValue() + (space.getValue() ? " " : "") + BuildConfig.VERSION;
        ClientFont.drawString(e.getContext(), watermark, getX(), getY(), Pagon.colorManager.resolve(color).getRGB());

        setWidth(ClientFont.width(watermark));
        setHeight(ClientFont.lineHeight());
    }
}


