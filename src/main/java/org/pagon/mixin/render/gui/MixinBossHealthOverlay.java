package org.pagon.mixin.render.gui;

import org.pagon.event.impl.gui.hud.RenderOverlayEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRender(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderOverlayEvent.BossBar(guiGraphics))) {
            ci.cancel();
        }
    }
}
