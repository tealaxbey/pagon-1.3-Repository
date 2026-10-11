package org.pagon.mixin.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.mc;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void pagon$hookJumpFromGround(CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }

        // GrimThrow module removed - jump hook disabled
    }
}


