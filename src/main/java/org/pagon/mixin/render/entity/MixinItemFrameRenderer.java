package org.pagon.mixin.render.entity;

import org.pagon.event.impl.render.entity.RenderItemFrameEvent;
import org.pagon.mixin.render.accessor.AccessorItemStackRenderState;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ItemFrameRenderer.class)
public class MixinItemFrameRenderer {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void pagon$hookExtractRenderState(net.minecraft.world.entity.decoration.ItemFrame itemFrame,
                                               ItemFrameRenderState state, float tickDelta, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderItemFrameEvent())) {
            ((AccessorItemStackRenderState) state.item).pagon$clear();
        }
    }
}
