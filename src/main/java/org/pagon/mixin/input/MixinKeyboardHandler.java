package org.pagon.mixin.input;

import org.pagon.event.impl.input.KeyInputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("TAIL"), cancellable = true)
    private void keyPress(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (action != 1) {
            return;
        }

        if (EVENT_BUS.post(new KeyInputEvent(input.key()))) {
            ci.cancel();
        }
    }
}


