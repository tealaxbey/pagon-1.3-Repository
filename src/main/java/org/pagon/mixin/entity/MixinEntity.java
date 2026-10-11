package org.pagon.mixin.entity;

import org.pagon.event.impl.entity.player.PushEntityEvent;
import org.pagon.manager.TeleportManager;
import static org.pagon.util.traits.Util.mc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(Entity.class)
public abstract class MixinEntity {
    
    @Shadow public abstract Vec3 position();
    
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void pagon$hookPushAwayFrom(Entity entity, CallbackInfo ci) {
        if (EVENT_BUS.post(new PushEntityEvent((Entity) (Object) this, entity))) {
            ci.cancel();
        }
    }

    @Inject(method = "displayFireAnimation", at = @At("HEAD"), cancellable = true)
    private void pagon$hookDisplayFireAnimation(CallbackInfoReturnable<Boolean> cir) {
        if (EVENT_BUS.post(new org.pagon.event.impl.entity.RenderFireEntityEvent())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void pagon$hookGetPose(CallbackInfoReturnable<Pose> cir) {
        if ((Object) this == mc.player && org.pagon.features.modules.movement.FakeFlyModule.shouldOverridePose()) {
            cir.setReturnValue(Pose.FALL_FLYING);
            cir.cancel();
        }
    }
    
    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void pagon$onSetPos(double x, double y, double z, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (mc.player != null && entity == mc.player && org.pagon.Pagon.teleportManager != null) {
            org.pagon.Pagon.teleportManager.checkTeleport(entity);
        }
    }
}


