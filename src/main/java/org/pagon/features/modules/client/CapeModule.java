package org.pagon.features.modules.client;

import org.pagon.features.modules.Module;
import net.minecraft.resources.Identifier;

public class CapeModule extends Module {
    public static CapeModule INSTANCE;

    public CapeModule() {
        super("Capes", "Client cape src", Category.CLIENT);
        INSTANCE = this;
    }

    public Identifier getCapeTexture() {
        return Identifier.fromNamespaceAndPath("pagon", "capes/cape.png");
    }
}


