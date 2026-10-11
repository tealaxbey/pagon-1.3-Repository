package org.pagon.manager;

import org.pagon.Pagon;
import org.pagon.features.modules.client.RotationsModule;
import org.pagon.util.MathUtil;
import org.pagon.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RotationManager implements Util {
    private float yaw;
    private float pitch;
    private float headYaw;
    private float headYawO;
    private float bodyYaw;
    private float bodyYawO;
    private float serverYaw;
    private float serverPitch;
    private boolean hasServerRotation;
    private boolean requestedThisTick;

    public void updateRotations() {
        if (Pagon.phasePause) return;
        this.yaw = mc.player.getYRot();
        this.pitch = mc.player.getXRot();
        this.headYaw = mc.player.yHeadRot;
        this.headYawO = mc.player.yHeadRotO;
        this.bodyYaw = mc.player.yBodyRot;
        this.bodyYawO = mc.player.yBodyRotO;
    }

    public void restoreRotations() {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yHeadRot = headYaw;
        mc.player.yHeadRotO = headYawO;
        mc.player.yBodyRot = bodyYaw;
        mc.player.yBodyRotO = bodyYawO;
    }

    public void applyRotations() {
        if (!hasServerRotation) return;
        mc.player.setYRot(serverYaw);
        mc.player.setXRot(serverPitch);
        mc.player.yHeadRot = serverYaw;
        mc.player.yHeadRotO = serverYaw;
        mc.player.yBodyRot = serverYaw;
        mc.player.yBodyRotO = serverYaw;
    }

    public void setPlayerRotations(float yaw, float pitch) {
        if (Pagon.phasePause) return;
        this.serverYaw = MathUtil.wrapDegrees(yaw);
        this.serverPitch = MathUtil.clamp(pitch, -90.0f, 90.0f);
        this.hasServerRotation = true;
        this.requestedThisTick = true;
    }

    public void setPlayerYaw(float yaw) {
        if (Pagon.phasePause) return;
        this.setPlayerRotations(yaw, hasServerRotation ? serverPitch : mc.player.getXRot());
    }

    public void lookAtPos(BlockPos pos) {
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), new Vec3((float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f));
        this.setPlayerRotations(angle[0], angle[1]);
    }

    public void lookAtVec3d(Vec3 vec3d) {
        if (Pagon.phasePause) return;
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), new Vec3(vec3d.x, vec3d.y, vec3d.z));
        this.setPlayerRotations(angle[0], angle[1]);
    }

    public void lookAtVec3d(double x, double y, double z) {
        Vec3 vec3d = new Vec3(x, y, z);
        this.lookAtVec3d(vec3d);
    }

    public void lookAtEntity(Entity entity) {
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), entity.getEyePosition());
        this.setPlayerRotations(angle[0], angle[1]);
    }

    public void setPlayerPitch(float pitch) {
        if (Pagon.phasePause) return;
        this.setPlayerRotations(hasServerRotation ? serverYaw : mc.player.getYRot(), pitch);
    }

    public Packet<?> rewriteMovementPacket(Packet<?> packet) {
        if (!hasServerRotation || !isMouseSendFixEnabled() || !(packet instanceof ServerboundMovePlayerPacket move)) {
            return packet;
        }

        boolean onGround = move.isOnGround();
        boolean horizontalCollision = move.horizontalCollision();
        if (packet instanceof ServerboundMovePlayerPacket.PosRot || packet instanceof ServerboundMovePlayerPacket.Pos) {
            return new ServerboundMovePlayerPacket.PosRot(
                    move.getX(mc.player.getX()),
                    move.getY(mc.player.getY()),
                    move.getZ(mc.player.getZ()),
                    serverYaw,
                    serverPitch,
                    onGround,
                    horizontalCollision
            );
        }
        if (packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.StatusOnly) {
            return new ServerboundMovePlayerPacket.Rot(serverYaw, serverPitch, onGround, horizontalCollision);
        }

        return packet;
    }

    public boolean isMovementFixEnabled() {
        RotationsModule module = getRotationsModule();
        return module != null && module.movementFix.getValue();
    }

    public boolean isMovementMotionFixEnabled() {
        RotationsModule module = getRotationsModule();
        return module != null && module.movementFix.getValue() && module.motion.getValue();
    }

    public boolean isMouseOverFixEnabled() {
        RotationsModule module = getRotationsModule();
        return module != null && module.mouseOverFix.getValue();
    }

    public boolean isMouseSendFixEnabled() {
        RotationsModule module = getRotationsModule();
        return module != null && module.mouseSendFix.getValue();
    }

    public boolean hasServerRotation() {
        return hasServerRotation;
    }

    public void beginRotationTick() {
        requestedThisTick = false;
    }

    public void endRotationTick() {
        if (requestedThisTick || !hasServerRotation || mc.player == null || mc.level == null) return;
        syncToClientCamera();
    }

    public void syncToClientCamera() {
        this.serverYaw = mc.player.getYRot();
        this.serverPitch = mc.player.getXRot();
        this.hasServerRotation = false;
        this.requestedThisTick = false;
    }

    public float getServerYaw() {
        return serverYaw;
    }

    public float getServerPitch() {
        return serverPitch;
    }

    private RotationsModule getRotationsModule() {
        if (Pagon.moduleManager == null) return null;
        return Pagon.moduleManager.getModuleByClass(RotationsModule.class);
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

}


