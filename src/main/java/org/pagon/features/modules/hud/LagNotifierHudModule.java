package org.pagon.features.modules.hud;

import org.pagon.Pagon;
import org.pagon.features.gui.HudEditorScreen;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.modules.client.HudModule;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.joml.Vector2f;

import java.awt.Color;

public class LagNotifierHudModule extends HudModule {
    public final Setting<Color> color = color("Color", 255, 255, 255, 255);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);

    public LagNotifierHudModule() {
        super("LagNotifier", "Display server lag status", 72, 12);
        this.displayName.setValueNoEvent("Lag Notifier");
        this.pos.setValueNoEvent(new Vector2f(0.414f, 0.448f));
        this.colorMode.setVisibility(v -> false);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        boolean editor = mc.screen instanceof HudEditorScreen;
        double seconds = Pagon.serverManager.serverRespondingTime() / 1000.0;
        if (!editor && seconds <= 3.0) {
            setWidth(0);
            setHeight(0);
            return;
        }

        String prefix = "The server is not responding ";
        String time = String.format("%.1fs!", editor ? 0.0 : seconds);
        int accent = Pagon.colorManager.resolve(color).getRGB();
        ClientFont.drawString(e.getContext(), prefix, getX(), getY(), accent);
        ClientFont.drawString(e.getContext(), time, getX() + ClientFont.width(prefix), getY(), 0xFFFFFFFF);
        String text = prefix + time;
        setWidth(ClientFont.width(text));
        setHeight(ClientFont.lineHeight());
    }
}


