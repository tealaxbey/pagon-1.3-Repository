package org.pagon.features.modules.client;

import org.pagon.Pagon;
import org.pagon.event.impl.ClientEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.commands.Command;
import org.pagon.features.gui.PagonGui;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ClickGuiModule extends Module {
    private static ClickGuiModule INSTANCE;

    public Setting<String> prefix = str("Prefix", "+");
    public Setting<Color> color = color("Color", 67, 107, 183, 255);
    public Setting<Float> darkness = num("Darkness", 0.80f, 0.0f, 1.0f);

    public ClickGuiModule() {
        super("ClickGui", "ClickGui", Module.Category.CLIENT);
        setBind(GLFW.GLFW_KEY_P);
        INSTANCE = this;
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
            if (event.getSetting().equals(this.prefix)) {
                Pagon.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
                Command.sendMessage("Prefix set to {global} %s", Pagon.commandManager.getCommandPrefix());
            }
            if (event.getSetting().equals(this.color)) {
                Pagon.colorManager.refresh();
            }
        }
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            return;
        }
        mc.setScreen(PagonGui.getClickGui());
    }

    @Override
    public void onLoad() {
        Pagon.colorManager.refresh();
        Pagon.commandManager.setCommandPrefix(this.prefix.getValue());
    }

    @Override
    public void onTick() {
        if (!(ClickGuiModule.mc.screen instanceof PagonGui)) {
            this.disable();
        }
    }

    public static ClickGuiModule getInstance() {
        return INSTANCE;
    }
}


