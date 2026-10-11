package org.pagon.features.modules.hud;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.gui.GuiStyle;
import org.pagon.features.modules.Module;
import org.pagon.features.modules.client.ColorModule;
import org.pagon.features.modules.client.HudModule;
import org.pagon.features.settings.DisplayNameProvider;
import org.pagon.features.settings.Setting;
import org.pagon.util.ColorUtil;
import org.pagon.util.render.ClientFont;
import org.joml.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FeatureListHudModule extends HudModule {
    public final Setting<Ordering> ordering = mode("Ordering", Ordering.Length);
    public final Setting<ColorMode> color = mode("Color", ColorMode.Static);

    public FeatureListHudModule() {
        super("FeatureList", "Display enabled modules", 90, 18);
        this.pos.setValueNoEvent(new Vector2f(0.95f, 0.003f));
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        List<Module> enabled = new ArrayList<>();
        for (Module module : Pagon.moduleManager.getModules()) {
            if (module == this || module.getCategory() == Module.Category.HUD || module.hidden || !module.isEnabled() || !module.isDrawn()) {
                continue;
            }
            enabled.add(module);
        }

        Comparator<Module> comparator = switch (ordering.getValue()) {
            case Length -> Comparator.comparingInt((Module module) -> ClientFont.width(module.getFullArrayString())).reversed();
            case Alphabetical -> Comparator.comparing(Module::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        };
        enabled.sort(comparator);

        List<String> lines = new ArrayList<>();
        for (Module module : enabled) {
            lines.add(module.getFullArrayString());
        }

        float x = getX();
        float y = getY();
        float right = x + getWidth();
        float maxWidth = 0.0f;
        int lineHeight = ClientFont.lineHeight() + 2;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, ClientFont.width(line));
        }
        boolean rightAligned = isRightAligned();
        if (rightAligned) {
            x = right - maxWidth;
            pos.getValue().x = x / mc.getWindow().getGuiScaledWidth();
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float drawX = rightAligned ? right - ClientFont.width(line) : x;
            ClientFont.drawString(e.getContext(), line, drawX, y + i * lineHeight, resolveColor(i, lines.size()));
        }

        setWidth(maxWidth);
        setHeight(lines.isEmpty() ? ClientFont.lineHeight() : lines.size() * lineHeight);
    }

    private int resolveColor(int index, int total) {
        return switch (color.getValue()) {
            case Static -> {
                Color moduleColor = Pagon.colorManager.getHudColor();
                yield moduleColor.getRGB();
            }
            case Rainbow -> Pagon.colorManager.getRainbowColor(index * 90, 255).getRGB();
            case Gradient -> {
                ColorModule colorModule = ColorModule.getInstance();
                if (colorModule == null) {
                    yield GuiStyle.accent();
                }
                float span = total <= 1 ? 0.0f : index / (float) (total - 1);
                float shift = (System.currentTimeMillis() % 2000L) / 2000.0f;
                yield ColorUtil.lerp(
                        Pagon.colorManager.resolve(colorModule.gColor1),
                        Pagon.colorManager.resolve(colorModule.gColor2),
                        (span + shift) % 1.0f
                ).getRGB();
            }
        };
    }

    private boolean isRightAligned() {
        return getX() >= mc.getWindow().getGuiScaledWidth() / 2.0f;
    }

    public enum Ordering implements DisplayNameProvider {
        Length("Length"),
        Alphabetical("Alphabetical");

        private final String displayName;

        Ordering(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }
    }

    public enum ColorMode implements DisplayNameProvider {
        Static("Static"),
        Rainbow("Rainbow"),
        Gradient("Gradient");

        private final String displayName;

        ColorMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }
    }
}


