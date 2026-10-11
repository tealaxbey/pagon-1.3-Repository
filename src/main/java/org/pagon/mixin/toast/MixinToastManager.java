package org.pagon.mixin.toast;

import org.pagon.event.impl.toast.RenderToastEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ToastManager.class)
public class MixinToastManager {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRender(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderToastEvent())) {
            ci.cancel();
        }
    }
}
