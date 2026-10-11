package org.pagon.mixin.render;

import org.pagon.event.impl.render.RenderFogEvent;
import org.pagon.event.impl.world.SkyboxEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.Color;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Unique
    private boolean pagon$terrainFog;

    @Inject(method = "setupFog", at = @At("HEAD"))
    private void hookSetupFog(Camera camera, int viewDistance, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<Vector4f> cir) {
        pagon$terrainFog = camera.getFluidInCamera() == FogType.NONE;
    }

    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void hookComputeFogColor(Camera camera, float tickDelta, ClientLevel level, int viewDistance, float darkenWorldAmount, CallbackInfoReturnable<Vector4f> cir) {
        SkyboxEvent.Fog event = new SkyboxEvent.Fog(tickDelta);
        if (EVENT_BUS.post(event)) {
            Color color = event.getColor();
            cir.setReturnValue(new Vector4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f));
        }
    }

    @ModifyArgs(
            method = "setupFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void hookUpdateFogBuffer(Args args) {
        if (!pagon$terrainFog) {
            return;
        }

        RenderFogEvent event = new RenderFogEvent((Float) args.get(6));
        if (EVENT_BUS.post(event)) {
            args.set(3, event.getStart());
            args.set(4, event.getEnd());
            args.set(5, event.getStart());
            args.set(6, event.getEnd());
            args.set(7, event.getEnd());
            args.set(8, event.getEnd());
        }
    }

}
