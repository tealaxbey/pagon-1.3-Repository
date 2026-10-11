package org.pagon.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.pagon.ducks.render.ILevelRenderer;
import org.pagon.event.impl.render.Render3DEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.features.Feature.EVENT_BUS;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer implements ILevelRenderer {

    @Shadow
    @Final
    private RenderTarget entityOutlineTarget;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderTail(GraphicsResourceAllocator objectAllocator, DeltaTracker tickCounter, boolean bl, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci) {
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
        
        Render3DEvent event = new Render3DEvent(matrices, matrix4f, tickCounter.getGameTimeDeltaPartialTick(false));
        EVENT_BUS.post(event);
        
        matrices.popPose();
    }

    @Override
    @Unique
    public RenderTarget pagon$getEntityOutlineTarget() {
        return entityOutlineTarget;
    }
}
