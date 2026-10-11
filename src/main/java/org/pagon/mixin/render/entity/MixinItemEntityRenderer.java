package org.pagon.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.entity.RenderItemEvent;
import org.pagon.impl.imixin.IEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmit(ItemEntityRenderState state, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState,
                                   CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) state).pagon$getEntity();
        if (entity instanceof ItemEntity itemEntity && EVENT_BUS.post(new RenderItemEvent(itemEntity))) {
            ci.cancel();
        }
    }
}
