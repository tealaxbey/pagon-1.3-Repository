package org.pagon.mixin.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.block.entity.RenderSignTextEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(AbstractSignRenderer.class)
public class MixinAbstractSignRenderer {
    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmitSignText(SignRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector submitNodeCollector, boolean front,
                                           CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderSignTextEvent())) {
            ci.cancel();
        }
    }
}
