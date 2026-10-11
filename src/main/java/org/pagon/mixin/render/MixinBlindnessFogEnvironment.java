package org.pagon.mixin.render;

import org.pagon.event.impl.world.BlindnessEvent;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(MobEffectFogEnvironment.class)
public class MixinBlindnessFogEnvironment {
    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
    private void pagon$hookIsApplicable(FogType fogType, net.minecraft.world.entity.Entity entity,
                                         CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof BlindnessFogEnvironment && EVENT_BUS.post(new BlindnessEvent())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
