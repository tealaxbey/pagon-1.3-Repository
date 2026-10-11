package org.pagon.mixin.entity;

import org.pagon.event.impl.entity.FireworkTickEvent;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void pagon$hookTick(CallbackInfo ci) {
        if (EVENT_BUS.post(new FireworkTickEvent((FireworkRocketEntity) (Object) this))) {
            ci.cancel();
        }
    }
}
