package org.pagon.util.render;

import org.pagon.features.modules.client.FontModule;
import org.pagon.util.font.Fonts;
import org.pagon.util.traits.Util;
import net.minecraft.client.gui.GuiGraphics;

public final class ClientFont implements Util {
    private ClientFont() {
        throw new AssertionError("Can't create an instance of utility class");
    }

    public static int width(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Fonts.isHudCustomEnabled() ? Math.round(Fonts.CLIENT.getStringWidth(text)) : mc.font.width(styledText(text));
    }

    public static int guiWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Fonts.isCustomEnabled() ? Math.round(Fonts.CLIENT_UNSCALED.getStringWidth(text)) : mc.font.width(styledText(text));
    }

    public static int lineHeight() {
        return Fonts.isHudCustomEnabled() ? Math.round(Fonts.CLIENT.getFontHeight()) : mc.font.lineHeight;
    }

    public static int guiLineHeight() {
        return Fonts.isCustomEnabled() ? Math.round(Fonts.CLIENT_UNSCALED.getFontHeight()) : mc.font.lineHeight;
    }

    public static String styledText(String text) {
        return text == null ? "" : text;
    }

    public static void drawString(GuiGraphics context, String text, double x, double y, int color) {
        if (text == null || context == null) {
            return;
        }
        FontModule fontModule = FontModule.getInstance();
        if (fontModule != null && fontModule.isEnabled() && fontModule.getHud() && Fonts.isInitialized()) {
            Fonts.CLIENT.drawStringWithShadow(context, text, x, y, color);
            return;
        }
        context.drawString(mc.font, styledText(text), Math.round((float) x), Math.round((float) y), color);
    }

    public static void drawGuiString(GuiGraphics context, String text, double x, double y, int color) {
        if (text == null || context == null) {
            return;
        }
        FontModule fontModule = FontModule.getInstance();
        if (fontModule != null && fontModule.isEnabled() && Fonts.isInitialized()) {
            Fonts.CLIENT_UNSCALED.drawString(context, text, x, y, color);
            return;
        }
        context.drawString(mc.font, styledText(text), Math.round((float) x), Math.round((float) y), color);
    }

    public static String trimToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (guiWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = guiWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + ellipsis;
            if (guiWidth(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return "";
    }
}


