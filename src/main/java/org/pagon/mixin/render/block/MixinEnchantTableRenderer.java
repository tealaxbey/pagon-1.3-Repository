package org.pagon.mixin.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.block.RenderTileEntityEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(EnchantTableRenderer.class)
public class MixinEnchantTableRenderer {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSubmit(EnchantTableRenderState state, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState,
                                   CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderTileEntityEvent.EnchantingTableBook())) {
            ci.cancel();
        }
    }
}
