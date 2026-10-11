package org.pagon.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.entity.RenderWitherSkullEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.state.WitherSkullRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(WitherSkullRenderer.class)
public class MixinWitherSkullRenderer {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmit(WitherSkullRenderState state, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState,
                                   CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderWitherSkullEvent())) {
            ci.cancel();
        }
    }
}
