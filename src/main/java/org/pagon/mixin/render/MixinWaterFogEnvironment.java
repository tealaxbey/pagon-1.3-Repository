package org.pagon.mixin.render;

import org.pagon.event.impl.world.BiomeColorEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(WaterFogEnvironment.class)
public class MixinWaterFogEnvironment {
    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void hookGetBaseColor(ClientLevel level, Camera camera, int viewDistance, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Water event = new BiomeColorEvent.Water();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }
}
