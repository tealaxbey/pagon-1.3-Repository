package org.pagon.mixin.render;

import org.pagon.event.impl.render.RenderWorldBorderEvent;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(WorldBorderRenderer.class)
public class MixinWorldBorderRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRender(WorldBorderRenderState worldBorderRenderState, Vec3 vec3, double d, double e, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderWorldBorderEvent())) {
            ci.cancel();
        }
    }
}
