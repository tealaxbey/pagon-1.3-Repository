package org.pagon.mixin.entity;

import org.pagon.event.impl.entity.player.PushFluidsEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;
import static org.pagon.util.traits.Util.mc;

@Mixin(Player.class)
public class MixinPlayerEntity {
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void pagon$hookIsPushedByFluid(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) {
            return;
        }
        if (EVENT_BUS.post(new PushFluidsEvent())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}


