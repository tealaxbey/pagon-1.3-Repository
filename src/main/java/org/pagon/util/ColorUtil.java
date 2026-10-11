package org.pagon.util;

import java.awt.Color;

public class ColorUtil {
    public static Color rainbow(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() + (long) delay) / 20.0);
        return Color.getHSBColor((float) ((rainbowState % 360.0) / 360.0), 1.0f, 1.0f);
    }

    public static Color rainbow(int delay, float brightness, float contrast) {
        return adjustBrightnessContrast(rainbow(delay), brightness, contrast);
    }

    public static Color adjustBrightnessContrast(Color color, float brightness, float contrast) {
        float red = adjustChannel(color.getRed() / 255.0f, brightness, contrast);
        float green = adjustChannel(color.getGreen() / 255.0f, brightness, contrast);
        float blue = adjustChannel(color.getBlue() / 255.0f, brightness, contrast);
        return new Color(red, green, blue, color.getAlpha() / 255.0f);
    }

    private static float adjustChannel(float value, float brightness, float contrast) {
        float contrasted = (value - 0.5f) * contrast + 0.5f;
        return Math.max(0.0f, Math.min(1.0f, contrasted * brightness));
    }

    public static Color lerp(Color first, Color second, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int red = Math.round(first.getRed() + (second.getRed() - first.getRed()) * ratio);
        int green = Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * ratio);
        int blue = Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * ratio);
        int alpha = Math.round(first.getAlpha() + (second.getAlpha() - first.getAlpha()) * ratio);
        return new Color(red, green, blue, alpha);
    }
}


