package org.pagon.mixin.entity;

import org.pagon.Pagon;
import org.pagon.event.Stage;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.network.PushOutOfBlocksEvent;
import org.pagon.event.impl.entity.player.UpdateWalkingPlayerEvent;
import org.pagon.manager.TeleportManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity {
    
    @Unique
    private float pagon$movementFixYaw;
    @Unique
    private float pagon$movementFixPitch;
    @Unique
    private float pagon$movementFixXBob;
    @Unique
    private float pagon$movementFixYBob;
    @Unique
    private boolean pagon$movementFixing;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickHook(CallbackInfo ci) {
        EVENT_BUS.post(new TickEvent());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.AFTER))
    private void tickHook2(CallbackInfo ci) {
        EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.PRE));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", shift = At.Shift.AFTER))
    private void tickHook3(CallbackInfo ci) {
        EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.POST));
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void applyMovementFix(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (Pagon.rotationManager == null
                || !Pagon.rotationManager.hasServerRotation()
                || !Pagon.rotationManager.isMovementFixEnabled()) {
            return;
        }

        pagon$movementFixYaw = player.getYRot();
        pagon$movementFixPitch = player.getXRot();
        pagon$movementFixXBob = player.xBob;
        pagon$movementFixYBob = player.yBob;
        pagon$movementFixing = true;

        player.setYRot(Pagon.rotationManager.getServerYaw());
        player.yHeadRot = Pagon.rotationManager.getServerYaw();
        player.setXRot(Pagon.rotationManager.getServerPitch());
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void restoreMovementFix(CallbackInfo ci) {
        if (!pagon$movementFixing) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        player.setYRot(pagon$movementFixYaw);
        player.yHeadRot = pagon$movementFixYaw;
        player.setXRot(pagon$movementFixPitch);
        player.xBobO = pagon$movementFixXBob;
        player.yBobO = pagon$movementFixYBob;
        player.xBob = pagon$movementFixXBob + (pagon$movementFixPitch - pagon$movementFixXBob) * 0.5F;
        player.yBob = pagon$movementFixYBob + (pagon$movementFixYaw - pagon$movementFixYBob) * 0.5F;
        pagon$movementFixing = false;
    }

    @Inject(method = "applyInput", at = @At("RETURN"))
    private void applyMotionMovementFix(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!pagon$movementFixing
                || Pagon.rotationManager == null
                || !Pagon.rotationManager.isMovementMotionFixEnabled()
                || (player.xxa == 0.0F && player.zza == 0.0F)) {
            return;
        }

        float delta = (pagon$movementFixYaw - Pagon.rotationManager.getServerYaw()) * ((float) Math.PI / 180.0F);
        float sin = (float) Math.sin(delta);
        float cos = (float) Math.cos(delta);
        float strafe = player.xxa;
        float forward = player.zza;

        player.xxa = strafe * cos - forward * sin;
        player.zza = forward * cos + strafe * sin;
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void pagon$hookStartUsingItem(InteractionHand hand, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            if (!player.getMainHandItem().isEmpty()) {
                ItemUseAnimation animation = player.getMainHandItem().getItem().getUseAnimation(player.getMainHandItem());
                if (animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK) return;
            }

            Result res = InventoryUtil.find(stack -> {
                ItemUseAnimation a = stack.getItem().getUseAnimation(stack);
                return a == ItemUseAnimation.EAT || a == ItemUseAnimation.DRINK;
            }, InventoryUtil.HOTBAR_SCOPE);
            if (res.found() && !res.holding()) {
                InventoryUtil.swap(res.slot());
            }
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void hookPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (EVENT_BUS.post(new PushOutOfBlocksEvent())) {
            ci.cancel();
        }
    }

}
