package org.pagon.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.RenderEntityInWorldEvent;
import org.pagon.impl.imixin.IEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmit(EntityRenderState state, CameraRenderState cameraRenderState,
                                   double x, double y, double z, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).pagon$getEntity();
        if (entity != null && EVENT_BUS.post(new RenderEntityInWorldEvent(entity))) {
            ((IEntityRenderState) state).pagon$setEntity(null);
            ci.cancel();
        }
    }
}
