package org.pagon.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.gui.hud.RenderOverlayEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void pagon$hookRenderFire(PoseStack poseStack, MultiBufferSource multiBufferSource,
                                              TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderOverlayEvent.Fire(null))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void pagon$hookRenderWater(Minecraft minecraft, PoseStack poseStack,
                                               MultiBufferSource multiBufferSource, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderOverlayEvent.Water(null))) {
            ci.cancel();
        }
    }

    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void pagon$hookGetViewBlockingState(net.minecraft.world.entity.player.Player player,
                                                        CallbackInfoReturnable<BlockState> cir) {
        if (EVENT_BUS.post(new RenderOverlayEvent.Block(null))) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
}
