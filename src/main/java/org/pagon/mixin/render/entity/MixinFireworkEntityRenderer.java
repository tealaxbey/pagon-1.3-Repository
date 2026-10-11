package org.pagon.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.entity.RenderFireworkRocketEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(FireworkEntityRenderer.class)
public class MixinFireworkEntityRenderer {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmit(FireworkRocketRenderState state, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState,
                                   CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderFireworkRocketEvent())) {
            ci.cancel();
        }
    }
}
