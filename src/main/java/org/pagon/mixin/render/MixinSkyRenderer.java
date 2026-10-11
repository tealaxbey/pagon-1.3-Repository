package org.pagon.mixin.render;

import org.pagon.event.impl.world.SkyboxEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hookExtractRenderState(ClientLevel level, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        SkyboxEvent.Sky event = new SkyboxEvent.Sky();
        if (EVENT_BUS.post(event)) {
            state.skyColor = event.getRGB();
        }
    }
}
