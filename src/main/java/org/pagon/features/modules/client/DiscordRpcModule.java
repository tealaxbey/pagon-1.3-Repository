package org.pagon.features.modules.client;

import org.pagon.Pagon;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;

/**
 * Discord RPC Module - Displays your game activity on Discord
 * Shows "Playing Future with pagon" with server information
 * Works on Windows, Linux, and macOS
 */
public class DiscordRpcModule extends Module {
    
    public Setting<Boolean> showServerIp = bool("Show Server IP", true);
    
    public DiscordRpcModule() {
        super("DiscordRPC", "Shows your game activity on Discord", Category.CLIENT);
        this.enabled.setValue(true); // Enable by default
    }

    @Override
    public void onEnable() {
        if (Pagon.discordRpcManager != null) {
            Pagon.discordRpcManager.setEnabled(true);
        }
    }

    @Override
    public void onDisable() {
        if (Pagon.discordRpcManager != null) {
            Pagon.discordRpcManager.setEnabled(false);
        }
    }

    @Override
    public void onLoad() {
        // Sync module state with RPC manager state on load
        if (Pagon.discordRpcManager != null && Pagon.discordRpcManager.isEnabled() != this.isEnabled()) {
            this.enabled.setValue(Pagon.discordRpcManager.isEnabled());
        }
    }

    public boolean shouldShowServerIp() {
        return showServerIp.getValue();
    }
    
    @Override
    public boolean isAlwaysEnabled() {
        return false;
    }
}
