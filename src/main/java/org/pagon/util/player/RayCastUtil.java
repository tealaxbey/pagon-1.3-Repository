package org.pagon.util.player;

import org.pagon.util.traits.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RayCastUtil implements Util {
    public static HitResult rayCast(double reach, float[] angles) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        Vec3 eyePos = mc.player.getEyePosition();
        return rayCast(reach, eyePos, angles);
    }

    public static HitResult rayCast(double reach, Vec3 eyePos, float[] angles) {
        if (mc.player == null || mc.level == null || eyePos == null || angles == null) {
            return null;
        }

        Vec3 rotation = getRotationVec(angles[0], angles[1]);
        Vec3 target = eyePos.add(rotation.scale(reach));
        return mc.level.clip(new ClipContext(eyePos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
    }

    public static HitResult raycastEntity(double reach, Vec3 position, float[] angles) {
        if (mc.player == null || mc.level == null || position == null || angles == null) {
            return null;
        }

        Vec3 rotation = getRotationVec(angles[0], angles[1]);
        Vec3 target = position.add(rotation.scale(reach));
        AABB box = new AABB(position, position).expandTowards(rotation.scale(reach)).inflate(1.0, 1.0, 1.0);
        Entity entity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        return ProjectileUtil.getEntityHitResult(entity, position, target, box,
                targetEntity -> !targetEntity.isSpectator() && targetEntity.isPickable(), reach * reach);
    }

    private static Vec3 getRotationVec(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(-yaw) - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }
}


