package org.pagon.util.font;

import com.mojang.blaze3d.platform.NativeImage;
import org.pagon.util.traits.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlyphCache implements Util {
    private final char start;
    private final char end;
    private final Font font;
    private final Identifier id;
    private final int padding;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private final int color;

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private int width;
    private int height;
    private boolean generated;

    public GlyphCache(char start, char end, Font font, Identifier id, int padding, boolean antiAlias, boolean fractionalMetrics, int color) {
        this.start = start;
        this.end = end;
        this.font = font;
        this.id = id;
        this.padding = padding;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.color = color;
    }

    public Glyph getGlyph(char c) {
        if (!generated) {
            createBitmap();
        }
        return glyphs.get(c);
    }

    public boolean contains(char c) {
        return c >= start && c < end;
    }

    public void clear() {
        try {
            Runnable releaseTask = () -> {
                try {
                    mc.getTextureManager().release(id);
                } catch (Exception ignored) {}
            };
            if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
                releaseTask.run();
            } else {
                try {
                    Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
                    String[] candidates = new String[]{"recordRenderCall", "recordRenderCalls", "runOnRenderThread", "addRenderCall"};
                    boolean scheduled = false;
                    for (String name : candidates) {
                        try {
                            java.lang.reflect.Method m = rs.getMethod(name, Runnable.class);
                            m.invoke(null, releaseTask);
                            scheduled = true;
                            break;
                        } catch (NoSuchMethodException ignoredMethod) {
                        }
                    }
                    if (!scheduled) {
                        try {
                            mc.execute(releaseTask);
                        } catch (Throwable t) {
                            releaseTask.run();
                        }
                    }
                } catch (Throwable reflectionFailed) {
                    try {
                        mc.execute(releaseTask);
                    } catch (Throwable t) {
                        releaseTask.run();
                    }
                }
            }
        } catch (Throwable ignored) {

            try {
                mc.getTextureManager().release(id);
            } catch (Exception ignored2) {}
        }
        glyphs.clear();
        generated = false;
    }

    public Identifier getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void createBitmap() {
        if (generated) {
            return;
        }
        List<Glyph> pendingGlyphs = new ArrayList<>();
        int range = end - start - 1;
        int ceiling = (int) (Math.ceil(Math.sqrt(range)) * 1.5);
        int cached = 0;
        int charX = 0;
        int maxX = 0;
        int maxY = 0;
        int currX = 0;
        int currY = 0;
        int currentRowMaxY = 0;
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), antiAlias, fractionalMetrics);
        while (cached <= range) {
            char currentChar = (char) (start + cached);
            Rectangle2D stringBounds = font.getStringBounds(String.valueOf(currentChar), frc);
            int glyphWidth = (int) Math.ceil(stringBounds.getWidth());
            int glyphHeight = (int) Math.ceil(stringBounds.getHeight());
            cached++;
            maxX = Math.max(maxX, currX + glyphWidth);
            maxY = Math.max(maxY, currY + glyphHeight);
            if (charX >= ceiling) {
                currX = 0;
                currY += currentRowMaxY + padding;
                charX = 0;
                currentRowMaxY = 0;
            }
            currentRowMaxY = Math.max(currentRowMaxY, glyphHeight);
            pendingGlyphs.add(new Glyph(currX, currY, glyphWidth, glyphHeight, currentChar, this));
            currX += glyphWidth + padding;
            charX++;
        }

        BufferedImage image = new BufferedImage(Math.max(maxX + padding, 1), Math.max(maxY + padding, 1), BufferedImage.TYPE_INT_ARGB);
        width = image.getWidth();
        height = image.getHeight();
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(255, 255, 255, 0));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(new Color(color, true));
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics
                ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias
                ? RenderingHints.VALUE_ANTIALIAS_ON
                : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias
                ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        for (Glyph glyph : pendingGlyphs) {
            g2d.drawString(String.valueOf(glyph.value()), glyph.textureWidth(), glyph.textureHeight() + metrics.getAscent());
            glyphs.put(glyph.value(), glyph);
        }
        g2d.dispose();
        registerTexture(image);
        generated = true;
    }

    private void registerTexture(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(outputStream.toByteArray()));
            DynamicTexture texture = new DynamicTexture(() -> "legacy-font-cache", nativeImage);
            mc.getTextureManager().register(id, texture);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to register glyph cache texture", exception);
        }
    }
}
