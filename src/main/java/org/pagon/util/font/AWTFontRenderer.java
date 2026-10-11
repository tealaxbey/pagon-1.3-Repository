package org.pagon.util.font;

import org.pagon.features.modules.client.FontModule;
import org.pagon.util.traits.Util;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AWTFontRenderer implements Closeable, Util {
    private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OGS]");
    private static final int SHADOW_COLOR = 0x80000000;

    private final float size;
    private final Map<Integer, List<GlyphCache>> caches = new HashMap<>();
    private final Map<Integer, Map<Character, Glyph>> glyphs = new HashMap<>();
    private final Map<Identifier, RenderType> renderTypes = new HashMap<>();

    private Font baseFont;
    private Font font;
    private int scale;
    private int lastScale;

    public AWTFontRenderer(InputStream inputStream, float size) {
        try {
            this.baseFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            this.baseFont = new Font("Verdana", Font.PLAIN, Math.round(size));
        }
        this.size = size;
        createFont(baseFont, size);
    }

    public static String stripControlCodes(String text) {
        return text == null ? "" : PATTERN_CONTROL_CODE.matcher(text).replaceAll("");
    }

    private void createFont(Font font, float size) {
        int currentScale = 1;
        if (mc.getWindow() != null) {
            currentScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        }
        this.lastScale = currentScale;
        this.scale = this.lastScale;
        this.font = font.deriveFont(size * scale);
    }

    public void drawStringWithShadow(GuiGraphics graphics, String text, double x, double y, int color) {
        drawString(graphics, stripControlCodes(text), x + 1.0f, y + 1.0f, SHADOW_COLOR, false);
        drawString(graphics, text, x, y, color, false);
    }

    public void drawStringWithShadow(PoseStack matrices, String text, double x, double y, int color) {
        drawString(matrices, stripControlCodes(text), x + 0.5f, y + 0.5f, SHADOW_COLOR, false);
        drawString(matrices, text, x, y, color, false);
    }

    public void drawString(GuiGraphics graphics, String text, double x, double y, int color) {
        drawString(graphics, text, x, y, color, false);
    }

    public void drawString(PoseStack matrices, String text, double x, double y, int color) {
        drawString(matrices, text, x, y, color, false);
    }

    public void drawString(GuiGraphics graphics, String text, double x, double y, Color color) {
        drawString(graphics, text, x, y, color.getRGB(), false);
    }

    public void drawString(PoseStack matrices, String text, double x, double y, Color color) {
        drawString(matrices, text, x, y, color.getRGB(), false);
    }

    private void drawString(PoseStack matrices, String text, double x, double y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int currentScale = 1;
        if (mc.getWindow() != null) {
            currentScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        }
        if (currentScale != lastScale) {
            close();
            createFont(baseFont, size);
        }

        float brightnessMultiplier = shadow ? 0.25f : 1.0f;
        int baseR = Math.round(((color >> 16) & 0xff) * brightnessMultiplier);
        int baseG = Math.round(((color >> 8) & 0xff) * brightnessMultiplier);
        int baseB = Math.round((color & 0xff) * brightnessMultiplier);
        int alpha = (color >> 24) & 0xff;
        if (alpha <= 0) {
            alpha = 255;
        }

        matrices.pushPose();
        try {
            matrices.translate((float) x, (float) y - 3.0f, 0.0f);
            matrices.scale(1.0f / scale, 1.0f / scale, 1.0f);

            Map<Identifier, List<CharLocation>> queued = new HashMap<>();
            float xOffset = 0.0f;
            float yOffset = 0.0f;
            float lineHeight = 0.0f;
            int r = baseR;
            int g = baseG;
            int b = baseB;
            boolean formatting = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (formatting) {
                    formatting = false;
                    if (Character.toLowerCase(c) == 'r') {
                        r = baseR;
                        g = baseG;
                        b = baseB;
                    } else {
                        int mapped = mapColorCode(c, color);
                        r = Math.round(((mapped >> 16) & 0xff) * brightnessMultiplier);
                        g = Math.round(((mapped >> 8) & 0xff) * brightnessMultiplier);
                        b = Math.round((mapped & 0xff) * brightnessMultiplier);
                    }
                    continue;
                }
                if (c == '\u00A7') {
                    formatting = true;
                    continue;
                }
                if (c == '\n') {
                    yOffset += lineHeight <= 0.0f ? getFontHeight() * scale : lineHeight;
                    xOffset = 0.0f;
                    lineHeight = 0.0f;
                    continue;
                }

                Glyph glyph = getGlyphFromChar(c, 0xffffffff);
                if (glyph == null) {
                    continue;
                }
                lineHeight = Math.max(lineHeight, glyph.height());
                if (glyph.value() != ' ') {
                    queued.computeIfAbsent(glyph.owner().getId(), id -> new ArrayList<>())
                            .add(new CharLocation(xOffset, yOffset, r, g, b, alpha, glyph));
                }
                xOffset += glyph.width();
            }

            for (Map.Entry<Identifier, List<CharLocation>> entry : queued.entrySet()) {
                BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (CharLocation location : entry.getValue()) {
                    Glyph glyph = location.glyph();
                    GlyphCache owner = glyph.owner();
                    float gx = location.x();
                    float gy = location.y();
                    float w = glyph.width();
                    float h = glyph.height();
                    float u1 = glyph.textureWidth() / (float) owner.getWidth();
                    float v1 = glyph.textureHeight() / (float) owner.getHeight();
                    float u2 = (glyph.textureWidth() + glyph.width()) / (float) owner.getWidth();
                    float v2 = (glyph.textureHeight() + glyph.height()) / (float) owner.getHeight();
                    int packed = ((location.a() & 0xff) << 24)
                            | ((location.r() & 0xff) << 16)
                            | ((location.g() & 0xff) << 8)
                            | (location.b() & 0xff);
                    bufferBuilder.addVertex(matrices.last().pose(), gx, gy + h, 0.0f).setUv(u1, v2).setColor(packed);
                    bufferBuilder.addVertex(matrices.last().pose(), gx + w, gy + h, 0.0f).setUv(u2, v2).setColor(packed);
                    bufferBuilder.addVertex(matrices.last().pose(), gx + w, gy, 0.0f).setUv(u2, v1).setColor(packed);
                    bufferBuilder.addVertex(matrices.last().pose(), gx, gy, 0.0f).setUv(u1, v1).setColor(packed);
                }
                renderType(entry.getKey()).draw(bufferBuilder.buildOrThrow());
            }
        } finally {
            matrices.popPose();
        }
    }

    private void drawString(GuiGraphics graphics, String text, double x, double y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int currentScale = 1;
        if (mc.getWindow() != null) {
            currentScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        }
        if (currentScale != lastScale) {
            close();
            createFont(baseFont, size);
        }

        float brightnessMultiplier = shadow ? 0.25f : 1.0f;
        float baseR = ((color >> 16) & 0xff) / 255.0f * brightnessMultiplier;
        float baseG = ((color >> 8) & 0xff) / 255.0f * brightnessMultiplier;
        float baseB = (color & 0xff) / 255.0f * brightnessMultiplier;
        float alpha = ((color >> 24) & 0xff) / 255.0f;
        if (alpha <= 0.0f) {
            alpha = 1.0f;
        }

        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(1.0f / scale, 1.0f / scale);
            float drawX = (float) x * scale;
            float drawY = ((float) y - 3.0f) * scale;
            float xOffset = 0.0f;
            float yOffset = 0.0f;
            float lineHeight = 0.0f;
            int currentColor = color;
            boolean formatting = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (formatting) {
                    formatting = false;
                    if (Character.toLowerCase(c) == 'r') {
                        currentColor = color;
                    } else {
                        currentColor = darken(mapColorCode(c, color), brightnessMultiplier);
                    }
                    continue;
                }
                if (c == '\u00A7') {
                    formatting = true;
                    continue;
                }
                if (c == '\n') {
                    yOffset += lineHeight <= 0.0f ? getFontHeight() * scale : lineHeight;
                    xOffset = 0.0f;
                    lineHeight = 0.0f;
                    continue;
                }
                Glyph glyph = getGlyphFromChar(c, currentColor);
                if (glyph == null) {
                    continue;
                }
                lineHeight = Math.max(lineHeight, glyph.height());
                if (glyph.value() != ' ') {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, glyph.owner().getId(),
                            Math.round(drawX + xOffset), Math.round(drawY + yOffset),
                            glyph.textureWidth(), glyph.textureHeight(),
                            glyph.width(), glyph.height(),
                            glyph.owner().getWidth(), glyph.owner().getHeight());
                }
                xOffset += glyph.width();
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        char[] chars = stripControlCodes(text).toCharArray();
        float currentLine = 0.0f;
        float previousMax = 0.0f;
        for (char c : chars) {
            if (c == '\n') {
                previousMax = Math.max(currentLine, previousMax);
                currentLine = 0.0f;
                continue;
            }
            Glyph glyph = getGlyphFromChar(c, 0xffffffff);
            currentLine += glyph == null ? 0.0f : glyph.width() / (float) scale;
        }
        return Math.max(currentLine, previousMax);
    }

    public float getStringHeight(String text) {
        if (text == null || text.isEmpty()) {
            return getFontHeight();
        }
        char[] chars = stripControlCodes(text).toCharArray();
        float currentLine = 0.0f;
        float previous = 0.0f;
        for (char c : chars) {
            if (c == '\n') {
                if (currentLine == 0.0f) {
                    Glyph glyph = getGlyphFromChar(' ', 0xffffffff);
                    currentLine = glyph == null ? getFontHeight() : glyph.height() / (float) scale;
                }
                previous += currentLine;
                currentLine = 0.0f;
                continue;
            }
            Glyph glyph = getGlyphFromChar(c, 0xffffffff);
            if (glyph != null) {
                currentLine = Math.max(glyph.height() / (float) scale, currentLine);
            }
        }
        return currentLine + previous;
    }

    public float getFontHeight() {
        return size;
    }

    private Glyph getGlyphFromChar(char c, int color) {
        List<GlyphCache> colorCaches = caches.computeIfAbsent(color, k -> new ArrayList<>());
        Map<Character, Glyph> colorGlyphs = glyphs.computeIfAbsent(color, k -> new HashMap<>());
        Glyph existing = colorGlyphs.get(c);
        if (existing != null) {
            return existing;
        }
        for (GlyphCache cache : colorCaches) {
            if (cache.contains(c)) {
                Glyph glyph = cache.getGlyph(c);
                colorGlyphs.put(c, glyph);
                return glyph;
            }
        }
        FontModule fontModule = FontModule.getInstance();
        boolean antiAlias = fontModule == null || fontModule.getAntiAlias();
        boolean fractionalMetrics = false;
        int base = 256 * (int) Math.floor(c / 256.0);
        GlyphCache glyphCache = new GlyphCache((char) base, (char) (base + 256), font, newGlyphIdentifier(), 5, antiAlias, fractionalMetrics, color);
        colorCaches.add(glyphCache);
        Glyph glyph = glyphCache.getGlyph(c);
        colorGlyphs.put(c, glyph);
        return glyph;
    }

    @Override
    public void close() {
        for (List<GlyphCache> cacheList : caches.values()) {
            for (GlyphCache cache : cacheList) {
                cache.clear();
            }
        }
        caches.clear();
        glyphs.clear();
        renderTypes.clear();
    }

    private Identifier newGlyphIdentifier() {
        return Identifier.fromNamespaceAndPath("pagon", "font/storage/" + UUID.randomUUID().toString().replace("-", ""));
    }

    private RenderType renderType(Identifier texture) {
        return renderTypes.computeIfAbsent(texture, id -> RenderType.create("pagon_font_" + id.getPath(),
                RenderSetup.builder(RenderPipelines.GUI_TEXTURED)
                        .withTexture("Sampler0", id)
                        .createRenderSetup()));
    }

    private int mapColorCode(char code, int fallback) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xff000000;
            case '1' -> 0xff0000AA;
            case '2' -> 0xff00AA00;
            case '3' -> 0xff00AAAA;
            case '4' -> 0xffAA0000;
            case '5' -> 0xffAA00AA;
            case '6' -> 0xffFFAA00;
            case '7' -> 0xffAAAAAA;
            case '8' -> 0xff555555;
            case '9' -> 0xff5555FF;
            case 'a' -> 0xff55FF55;
            case 'b' -> 0xff55FFFF;
            case 'c' -> 0xffFF5555;
            case 'd' -> 0xffFF55FF;
            case 'e' -> 0xffFFFF55;
            case 'f' -> 0xffffffff;
            default -> fallback;
        };
    }

    private int darken(int color, float brightnessMultiplier) {
        int a = (int) (((color >>> 24) & 0xff) * brightnessMultiplier);
        int r = (int) (((color >>> 16) & 0xff) * brightnessMultiplier);
        int g = (int) (((color >>> 8) & 0xff) * brightnessMultiplier);
        int b = (int) ((color & 0xff) * brightnessMultiplier);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record CharLocation(float x, float y, int r, int g, int b, int a, Glyph glyph) {
    }
}
