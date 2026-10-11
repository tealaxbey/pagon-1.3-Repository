package org.pagon.util.font;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FormattingUtil {
    private static final Map<Integer, ChatFormatting> COLOR_TO_FORMATTING = new HashMap<>();

    static {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            Integer color = formatting.getColor();
            if (color != null) {
                COLOR_TO_FORMATTING.put(color, formatting);
            }
        }
    }

    private FormattingUtil() {
    }

    public static String toString(Component text) {
        StringBuilder builder = new StringBuilder();
        text.visit((styleOverride, message) -> {
            if (!message.isEmpty()) {
                appendFormatting(builder, styleOverride);
                builder.append(message);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    public static String toString(FormattedCharSequence text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            appendFormatting(builder, style);
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private static void appendFormatting(StringBuilder builder, Style style) {
        if (style.getColor() != null) {
            int rgb = style.getColor().getValue();
            ChatFormatting formatting = COLOR_TO_FORMATTING.get(rgb);
            if (formatting != null) {
                builder.append(ChatFormatting.PREFIX_CODE).append(formatting.getChar());
                return;
            }
        }
        if (style.isObfuscated()) {
            builder.append(ChatFormatting.PREFIX_CODE).append('k');
        } else if (style.isBold()) {
            builder.append(ChatFormatting.PREFIX_CODE).append('l');
        } else if (style.isStrikethrough()) {
            builder.append(ChatFormatting.PREFIX_CODE).append('m');
        } else if (style.isUnderlined()) {
            builder.append(ChatFormatting.PREFIX_CODE).append('n');
        } else if (style.isItalic()) {
            builder.append(ChatFormatting.PREFIX_CODE).append('o');
        } else {
            builder.append(ChatFormatting.PREFIX_CODE).append('r');
        }
    }
}
