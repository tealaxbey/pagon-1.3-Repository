package org.pagon.features.gui;

import org.pagon.Pagon;
import org.pagon.features.modules.client.ClickGuiModule;
import org.pagon.util.render.ClientFont;
import net.minecraft.util.Mth;

import java.awt.*;

public final class GuiStyle {
    public static final int PANEL_WIDTH = 102;
    public static final int HEADER_HEIGHT = 11;
    public static final int ITEM_HEIGHT = 12;
    public static final int PANEL_GAP = 3;
    public static final float SETTING_TEXT_OFFSET = 5.0f;

    public static final int BACKGROUND = 0x66030003;
    public static final int PANEL = 0xD8141010;
    public static final int PANEL_DARK = 0xE0101010;
    public static final int PANEL_HOVER = 0xE01B1818;
    public static final int SETTINGS = 0xE0121111;
    public static final int SETTING_ALT = 0xE0181515;
    public static final int TEXT = 0xFFD0D0D0;
    public static final int TEXT_DIM = 0xFF7B7B7B;
    public static final int TEXT_BRIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xAA000000;

    private GuiStyle() {
        throw new AssertionError("Can't create an instance of utility class");
    }

    public static int accent() {
        ClickGuiModule clickGui = ClickGuiModule.getInstance();
        if (clickGui == null) {
            return TEXT_BRIGHT;
        }
        return Pagon.colorManager.resolve(clickGui.color).getRGB();
    }

    public static int background() {
        ClickGuiModule clickGui = ClickGuiModule.getInstance();
        if (clickGui == null) {
            return BACKGROUND;
        }
        int alpha = Mth.clamp(Math.round(clickGui.darkness.getValue() * 255.0f), 0, 255);
        return (alpha << 24) | (BACKGROUND & 0x00FFFFFF);
    }

    public static int accent(int alpha) {
        Color color = new Color(accent(), false);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }

    public static int accentBorder() {
        Color color = new Color(accent(), true);
        int red = Math.min(255, (int) (color.getRed() * 1.12f) + 12);
        int green = Math.min(255, (int) (color.getGreen() * 1.12f) + 12);
        int blue = Math.min(255, (int) (color.getBlue() * 1.12f) + 12);
        return new Color(red, green, blue, color.getAlpha()).getRGB();
    }

    public static int text(boolean active) {
        return active ? accent() : TEXT;
    }

    public static int headerHeight() {
        return HEADER_HEIGHT;
    }

    public static int itemHeight() {
        return ITEM_HEIGHT;
    }
}


