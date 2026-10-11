package org.pagon.mixin.client;

import org.pagon.Pagon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
public abstract class MixinCamera {
    
    @Shadow protected abstract void setPosition(double x, double y, double z);
    
    @Inject(method = "setup", at = @At("TAIL"))
    private void pagon$smoothTeleport(Level level, Entity entity, 
                                       boolean detached, boolean thirdPerson, float partialTick, CallbackInfo ci) {
        if (Pagon.teleportManager != null && entity != null) {
            Vec3 smoothPos = Pagon.teleportManager.getRenderPosition(entity, partialTick);
            if (smoothPos != null) {
                double eyeHeight = entity.getEyeHeight();
                setPosition(smoothPos.x, smoothPos.y + eyeHeight, smoothPos.z);
            }
        }
    }
}
