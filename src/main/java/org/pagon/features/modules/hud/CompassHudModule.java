package org.pagon.features.modules.hud;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.modules.client.HudModule;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2f;

import java.awt.Color;

public class CompassHudModule extends HudModule {
    public Setting<Integer> compassWidth = num("Width", 304, 200, 600);
    public Setting<Boolean> background = bool("Background", true);
    public Setting<Color> backgroundColor = color("BackgroundColor", 0, 0, 0, 200);
    public Setting<Color> textColor = color("TextColor", 255, 255, 255, 255);
    public Setting<Integer> fadeWidth = num("FadeWidth", 60, 30, 100);
    public final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);

    private static final float BAR_HEIGHT = 18;

    public CompassHudModule() {
        super("Compass", "Display a horizontal compass showing direction", 400, 18);
        this.pos.setValueNoEvent(new Vector2f(0.5f, 0.02f));
        this.colorMode.setVisibility(v -> false);
        this.backgroundColor.setVisibility(v -> background.getValue());
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        float width = compassWidth.getValue();
        float x = getX() - width / 2;
        float y = getY();

        setWidth(width);
        setHeight(BAR_HEIGHT);

        float yaw = (mc.player.getYRot() % 360 + 360) % 360;


        if (background.getValue()) {
            Color bgColor = Pagon.colorManager.resolve(backgroundColor);
            int fadeW = fadeWidth.getValue();
            Color transparent = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
            
            RenderUtil.horizontalGradient(e.getContext(), x, y, x + fadeW, y + BAR_HEIGHT, transparent, bgColor);
            
            RenderUtil.rect(e.getContext(), x + fadeW, y, x + width - fadeW, y + BAR_HEIGHT, bgColor.getRGB());
            
            RenderUtil.horizontalGradient(e.getContext(), x + width - fadeW, y, x + width, y + BAR_HEIGHT, bgColor, transparent);
        }

        Color textCol = Pagon.colorManager.resolve(textColor);
        float centerX = x + width / 2;
        
        float visibleRange = 120f;
        float renderPadding = fadeWidth.getValue();
        
        for (int degree = -180; degree <= 540; degree++) {
            int normalizedDeg = ((degree % 360) + 360) % 360;
            float angleDiff = normalizedDeg - yaw;
            
            if (angleDiff > 180) angleDiff -= 360;
            if (angleDiff < -180) angleDiff += 360;
            
            if (Math.abs(angleDiff) > visibleRange / 2) continue;
            
            float tickX = centerX + (angleDiff / visibleRange) * width;
            
            if (tickX < x - renderPadding || tickX > x + width + renderPadding) continue;
            
            float fadeAlpha = calculateFadeAlpha(tickX, x, width, fadeWidth.getValue());
            
            if (fadeAlpha < 0.05f) fadeAlpha = 0.05f;
            
            int color = applyAlpha(textCol.getRGB(), fadeAlpha);
            
            if (normalizedDeg % 45 == 0) {
                String label = getDirectionLabel(normalizedDeg);
                float labelWidth = ClientFont.width(label);
                ClientFont.drawString(e.getContext(), label, tickX - labelWidth / 2, y + 2, color);
                
                RenderUtil.rect(e.getContext(), tickX - 0.5f, y + BAR_HEIGHT - 8, tickX + 0.5f, y + BAR_HEIGHT - 1, color);
                
            } else if (normalizedDeg % 15 == 0) {
                String degLabel = String.valueOf(normalizedDeg);
                float labelWidth = ClientFont.width(degLabel);
                ClientFont.drawString(e.getContext(), degLabel, tickX - labelWidth / 2, y + 2, color);
                
                RenderUtil.rect(e.getContext(), tickX - 0.5f, y + BAR_HEIGHT - 6, tickX + 0.5f, y + BAR_HEIGHT - 1, color);
                
            } else if (normalizedDeg % 5 == 0) {
                RenderUtil.rect(e.getContext(), tickX - 0.5f, y + BAR_HEIGHT - 4, tickX + 0.5f, y + BAR_HEIGHT - 1, color);
            }
        }
    }

    private String getDirectionLabel(int degree) {
        return switch (degree) {
            case 0 -> "N";
            case 45 -> "NE";
            case 90 -> "E";
            case 135 -> "SE";
            case 180 -> "S";
            case 225 -> "SW";
            case 270 -> "W";
            case 315 -> "NW";
            default -> "";
        };
    }

    private float calculateFadeAlpha(float tickX, float x, float width, int fadeWidth) {
        float distFromLeftEdge = tickX - x;
        float distFromRightEdge = (x + width) - tickX;
        float distFromNearestEdge = Math.min(distFromLeftEdge, distFromRightEdge);
        
        if (distFromNearestEdge < fadeWidth) {
            return distFromNearestEdge / fadeWidth;
        }
        
        return 1.0f;
    }

    private int applyAlpha(int color, float alphaMultiplier) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        int newAlpha = (int) (a * alphaMultiplier);
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }
}
