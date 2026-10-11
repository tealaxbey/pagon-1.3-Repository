package org.pagon.features.modules.client;

import org.pagon.event.impl.ClientEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.font.Fonts;

public class FontModule extends Module {
    private static FontModule INSTANCE;

    public final Setting<Boolean> antiAlias = bool("AntiAlias", true);
    public final Setting<Boolean> hud = bool("Hud", true);

    private boolean lastAntiAlias = antiAlias.getValue();

    public FontModule() {
        super("Font", "Changes the client text to custom font rendering", Category.CLIENT);
        INSTANCE = this;
    }

    public static FontModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        Fonts.loadFonts();
    }

    @Override
    public void onTick() {
        if (lastAntiAlias != antiAlias.getValue()) {
            lastAntiAlias = antiAlias.getValue();
            Fonts.loadFonts();
        }
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() != ClientEvent.Type.SETTING_UPDATE || event.getSetting().getFeature() != this) {
            return;
        }
        if (event.getSetting() == antiAlias && Fonts.isInitialized()) {
            Fonts.loadFonts();
            lastAntiAlias = antiAlias.getValue();
        }
    }

    public boolean getAntiAlias() {
        return antiAlias.getValue();
    }

    public boolean getHud() {
        return hud.getValue();
    }

}


