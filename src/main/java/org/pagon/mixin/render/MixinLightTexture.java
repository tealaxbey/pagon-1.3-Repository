package org.pagon.mixin.render;

import org.pagon.event.impl.render.AmbientColorEvent;
import org.pagon.event.impl.render.LightmapGammaEvent;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(LightTexture.class)
public class MixinLightTexture {
    @Unique
    private boolean pagon$ambientColorActive;
    @Unique
    private Color pagon$ambientColor = Color.WHITE;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void hookUpdateLightTexture(float tickDelta, CallbackInfo ci) {
        AmbientColorEvent event = new AmbientColorEvent();
        pagon$ambientColorActive = EVENT_BUS.post(event) && event.getColor() != null;
        pagon$ambientColor = pagon$ambientColorActive ? event.getColor() : Color.WHITE;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0))
    private float hookAmbientLightFactor(float value) {
        return pagon$ambientColorActive ? 1.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 1))
    private float hookSkyFactor(float value) {
        return pagon$ambientColorActive ? 0.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 2))
    private float hookBlockFactor(float value) {
        return pagon$ambientColorActive ? 0.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3))
    private float hookNightVisionFactor(float value) {
        return pagon$ambientColorActive ? 0.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 4))
    private float hookDarknessScale(float value) {
        return pagon$ambientColorActive ? 0.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 5))
    private float hookDarkenWorldFactor(float value) {
        return pagon$ambientColorActive ? 0.0f : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 6))
    private float hookBrightnessFactor(float value) {
        if (pagon$ambientColorActive) {
            return 0.0f;
        }
        LightmapGammaEvent event = new LightmapGammaEvent(value);
        return EVENT_BUS.post(event) ? event.getGamma() : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0))
    private Vector3fc hookSkyLightColor(Vector3fc value) {
        return pagon$ambientColorActive ? new Vector3f(0.0f) : value;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 1))
    private Vector3fc hookAmbientColor(Vector3fc value) {
        return pagon$ambientColorActive ? pagon$ambientColorVector() : value;
    }

    @Unique
    private Vector3f pagon$ambientColorVector() {
        return new Vector3f(
                pagon$ambientColor.getRed() / 255.0f,
                pagon$ambientColor.getGreen() / 255.0f,
                pagon$ambientColor.getBlue() / 255.0f
        );
    }
}
