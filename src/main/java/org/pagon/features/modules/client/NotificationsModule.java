package org.pagon.features.modules.client;

import org.pagon.event.impl.ClientEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.TextUtil;
import org.pagon.util.player.ChatUtil;

public class NotificationsModule extends Module {
    private static final String MODULE_FORMAT = "%s toggled %s%s{}.";

    public Setting<Boolean> moduleToggle = bool("Module Toggle", true);

    public NotificationsModule() {
        super("Notifications", "Displays notifications for various client events", Category.CLIENT);
    }

    @Subscribe
    public void onClient(ClientEvent event) {
        if (!moduleToggle.getValue()
                || event.getType() != ClientEvent.Type.TOGGLE_MODULE
                || !(event.getFeature() instanceof Module module)
                || module instanceof ClickGuiModule) {
            return;
        }

        boolean moduleState = module.isEnabled();
        ChatUtil.sendNotificationMessage(TextUtil.text(MODULE_FORMAT,
                module.getDisplayName(),
                moduleState ? "{green}" : "{red}",
                moduleState ? "on" : "off"), module.getName());
    }
}


