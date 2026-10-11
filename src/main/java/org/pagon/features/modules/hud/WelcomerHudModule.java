package org.pagon.features.modules.hud;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.modules.client.HudModule;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.joml.Vector2f;

import java.awt.Color;

public class WelcomerHudModule extends HudModule {
    public final Setting<Boolean> highlight = bool("Highlight", true);
    public final Setting<Color> color = color("Color", 255, 255, 255, 255);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);

    public WelcomerHudModule() {
        super("Welcomer", "Display a welcome message", 120, 12);
        this.pos.setValueNoEvent(new Vector2f(0.50f, 0.004f));
        this.colorMode.setVisibility(v -> false);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        String prefix = "Welcome ";
        String name = mc.player.getGameProfile().name();
        String suffix = " :3";
        int textWidth = ClientFont.width(prefix + name + suffix);
        float left = getX() - textWidth / 2f;
        float y = getY();
        int baseColor = Pagon.colorManager.resolve(color).getRGB();

        ClientFont.drawString(e.getContext(), prefix, left, y, baseColor);
        int prefixWidth = ClientFont.width(prefix);
        ClientFont.drawString(e.getContext(), name, left + prefixWidth, y,
                highlight.getValue() ? 0xFFFFFFFF : baseColor);
        ClientFont.drawString(e.getContext(), suffix, left + prefixWidth + ClientFont.width(name), y, baseColor);

        setWidth(textWidth);
        setHeight(ClientFont.lineHeight());
    }
}


