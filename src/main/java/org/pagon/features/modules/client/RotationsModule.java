package org.pagon.features.modules.client;

import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;

public class RotationsModule extends Module {
    public final Setting<Boolean> movementFix = bool("MovementFix", true);
    public final Setting<Boolean> motion = bool("Motion", true);
    public final Setting<Boolean> mouseOverFix = bool("Mouse Over Fix", true);
    public final Setting<Boolean> mouseSendFix = bool("Mouse Send Fix", true);

    public RotationsModule() {
        super("Rotations", "Configures global module rotations", Category.CLIENT);
        this.motion.setVisibility(value -> this.movementFix.getValue());
        enable();
    }

    @Override
    public boolean isAlwaysEnabled() {
        return true;
    }
}


