package org.pagon.util;

import org.pagon.Pagon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class TextUtil {
    private TextUtil() {
        throw new AssertionError("Can't create an instance of utility class");
    }

    public static void init() {
    }

    public static MutableComponent text(String content, Object... obj) {
        if (content == null) return Component.empty();
        String formatted = (obj == null || obj.length == 0) ? content : String.format(content, obj);

        MutableComponent out = Component.empty();
        Style style = Style.EMPTY;

        int i = 0;
        while (i < formatted.length()) {
            int open = formatted.indexOf('{', i);
            if (open < 0) {
                String tail = formatted.substring(i);
                if (!tail.isEmpty()) out.append(Component.literal(tail).setStyle(style));
                break;
            }

            if (open > i) {
                String chunk = formatted.substring(i, open);
                if (!chunk.isEmpty()) out.append(Component.literal(chunk).setStyle(style));
            }

            int close = formatted.indexOf('}', open + 1);
            if (close < 0) {
                String tail = formatted.substring(open);
                if (!tail.isEmpty()) out.append(Component.literal(tail).setStyle(style));
                break;
            }

            String tag = formatted.substring(open + 1, close).trim();
            style = switch (tag) {
                case "" -> Style.EMPTY;
                case "red" -> Style.EMPTY.withColor(ChatFormatting.RED);
                case "green" -> Style.EMPTY.withColor(ChatFormatting.GREEN);
                case "global" -> Style.EMPTY.withColor(Pagon.colorManager.getColorAsInt());
                default -> style;
            };

            i = close + 1;
        }

        return out;
    }
}


