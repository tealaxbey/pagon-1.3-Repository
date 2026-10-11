package org.pagon.mixin.chunk.light;

import org.pagon.event.impl.chunk.light.RenderSkylightEvent;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ChunkSkyLightSources.class)
public class MixinChunkSkyLightSources {
    @Inject(method = "fillFrom", at = @At("HEAD"), cancellable = true)
    private void pagon$hookFillFrom(net.minecraft.world.level.chunk.ChunkAccess chunkAccess, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderSkylightEvent())) {
            ci.cancel();
        }
    }
}
