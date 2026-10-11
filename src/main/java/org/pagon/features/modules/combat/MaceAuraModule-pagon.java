package org.pagon.features.modules.combat;

import org.pagon.Pagon;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.render.Render3DEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.manager.RotationManager;
import org.pagon.util.MathUtil;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.SilentSwapType;
import org.pagon.util.render.RenderUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class MaceAuraModule extends Module {

    public enum RotateMode { NORMAL, HOLD, NONE }

    private static final float MACE_BASE_COOLDOWN = 34f;
    private static final double TAKEOFF_VELOCITY = 0.42;
    private static final int CHEST_MENU_SLOT = 6;
    private static final int OFFHAND_MENU_SLOT = 45;
    private static final double SMASH_FALL_DISTANCE = 1.5;
    private static final int MAX_ACTIVATE_WAIT = 40;
    private static final int MAX_ATTACK_WAIT = 10;
    private static final int MAX_DIVE_COOLDOWN = 4;
    private static final double POSITION_THRESHOLD = 2.5;
    private static final float VERTICAL_DIVE_PITCH = 89f;
    private static final double ATTACK_AIM_HORIZON_TICKS = 3.0;
    private static final double PREDICTION_TICKS_CAP = 8.0;
    private static final double MAX_OBSTACLE_CLEARANCE = 16.0;
    private static final double DIVE_CUT_GAP = 5.0;
    private static final double PREDICTION_TICKS_PER_BLOCK = 0.3;
    private static final String ROTATION_ID = MaceAuraModule.class.getName();
    
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double SNEAK_MULTIPLIER = 0.3;
    private static final double GRAVITY_ACCELERATION = 0.08;
    private static final double AIR_RESISTANCE = 0.98;
    private static final double GROUND_FRICTION = 0.6;
    private static final double VELOCITY_BLEND_MOVING = 0.75;
    private static final double LOOK_BLEND_MOVING = 0.25;
    private static final double MIN_MOVEMENT_THRESHOLD = 0.05;

    private final Setting<Double> range = num("Range", 3.5, 1.0, 6.0);
    private final Setting<Double> delay = num("Delay", 0.85, 0.0, 1.0);
    private final Setting<Boolean> silentSwap = bool("SilentSwap", true);
    private final Setting<RotateMode> rotate = mode("Rotate", RotateMode.NORMAL);
    private final Setting<Boolean> swing = bool("Swing", true);
    private final Setting<Boolean> render = bool("Render", true);

    private final Setting<Boolean> autoElytra = bool("AutoElytra", true);
    private final Setting<Double> targetRange = num("TargetRange", 40.0, 8.0, 128.0);
    private final Setting<Double> height = num("Height", 16.0, 3.0, 25.0);
    private final Setting<Double> attackHeight = num("AttackHeight", 3.0, 0.5, 4.0);
    private final Setting<Double> climbPitch = num("ClimbPitch", 80.0, 45.0, 89.0);
    private final Setting<Double> descentPitch = num("DescentPitch", 45.0, 15.0, 85.0);
    private final Setting<Boolean> autoFirework = bool("AutoFirework", true);
    private final Setting<Integer> fireworkDelay = num("FireworkDelay", 20, 5, 60);
    
    private final Setting<Boolean> prediction = bool("Prediction", true);
    private final Setting<Integer> predictionTicks = num("Ticks", 2, 1, 20);

    private enum State {
        IDLE, FIND_TARGET, MELEE, EQUIP_ELYTRA, TAKEOFF, ACTIVATE_ELYTRA,
        APPROACHING, ASCENDING, DIVING, RECOVER
    }

    private Player currentTarget;
    private State state = State.IDLE;
    private float attackCooldownTicks;
    private int fireworkTimer;
    private int activateWaitTicks;
    private int attackWaitTicks;
    private int takeoffAirTicks;
    private int lastAimTick = Integer.MIN_VALUE / 2;
    private boolean boostArmed;
    private Vec3 preFlightVelocity = Vec3.ZERO;
    private boolean momentumCaptured;
    private boolean hasBoostedThisFlight;
    private boolean attackedThisDive;
    private boolean startFallFlyingSent;
    private boolean weEquippedElytra;
    private int stowedChestContainerSlot = -1;
    private ItemStack stowedChestStack = ItemStack.EMPTY;
    
    private Vec3 lastTargetPos = Vec3.ZERO;
    private Vec3 lastTargetVel = Vec3.ZERO;
    private int predictionHistoryTicks = 0;

    public MaceAuraModule() {
        super("MaceAura", "Automatic aerial mace combat system", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        resetAerialState(true);
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (nullCheck() || mc.player.isDeadOrDying()) {
            resetAerialState(false);
            return;
        }

        attackCooldownTicks = Math.max(0f, attackCooldownTicks - 1f);

        if (!isTargetValid()) {
            invalidateTarget();
        }

        switch (state) {
            case IDLE -> state = State.FIND_TARGET;
            case FIND_TARGET -> tickFindTarget();
            case MELEE -> tickMelee();
            case EQUIP_ELYTRA -> tickEquipElytra();
            case TAKEOFF -> tickTakeoff();
            case ACTIVATE_ELYTRA -> tickActivateElytra();
            case APPROACHING -> tickApproaching();
            case ASCENDING -> tickAscending();
            case DIVING -> tickDiving();
            case RECOVER -> tickRecover();
        }
    }

    private void tickFindTarget() {
        findTarget();
        if (currentTarget == null) {
            state = State.IDLE;
            return;
        }
        state = State.EQUIP_ELYTRA;
    }

    private void tickMelee() {
        if (!isTargetValid() || !isTargetInMeleeRange()) {
            state = State.FIND_TARGET;
            return;
        }

        if (equipElytra()) {
            startFallFlyingSent = false;
            activateWaitTicks = 0;
            hasBoostedThisFlight = false;
            boostArmed = false;
            state = State.TAKEOFF;
            return;
        }

        if (attackCooldownTicks > 0f) return;

        submitMeleeAim();

        boolean hit = attackLogic(currentTarget);
        if (hit) {
            attackCooldownTicks = MACE_BASE_COOLDOWN * delay.getValue().floatValue();
        } else {
            submitMeleeAim();
            attackCooldownTicks = 3f;
        }
    }

    private void submitMeleeAim() {
        if (rotate.getValue() == RotateMode.NONE || currentTarget == null) return;
        Vec3 eye = mc.player.getEyePosition();
        float[] angles = MathUtil.calcAngle(eye, getClosestPointToEye(eye, currentTarget.getBoundingBox()));
        Pagon.rotationManager.setPlayerRotations(angles[0], angles[1]);
    }

    private void findTarget() {
        double rangeSq = targetRange.getValue() * targetRange.getValue();
        Vec3 playerPos = mc.player.position();
        Player best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.getEntities(null, mc.player.getBoundingBox().inflate(targetRange.getValue()))) {
            if (!(entity instanceof Player player)) continue;
            if (player == mc.player) continue;
            if (!player.isAlive() || player.isDeadOrDying()) continue;
            
            // Friend check
            if (Pagon.friendManager != null && Pagon.friendManager.isFriend(player)) continue;

            double dist = playerPos.distanceToSqr(player.position());
            if (dist > rangeSq) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }

        currentTarget = best;
    }

    private void tickEquipElytra() {
        if (equipElytra()) {
            startFallFlyingSent = false;
            activateWaitTicks = 0;
            hasBoostedThisFlight = false;
            boostArmed = false;
            state = State.TAKEOFF;
        } else if (isTargetInMeleeRange()) {
            state = State.MELEE;
        } else {
            state = State.IDLE;
        }
    }

    private boolean equipElytra() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) {
            weEquippedElytra = false;
            stowedChestContainerSlot = -1;
            stowedChestStack = ItemStack.EMPTY;
            return true;
        }

        if (!autoElytra.getValue()) return false;
        if (!canClickPlayerInventory()) return false;

        Result elytra = InventoryUtil.find(Items.ELYTRA, InventoryUtil.FULL_SCOPE);
        if (!elytra.found()) return false;

        int containerSlot = containerSlotOf(elytra);
        stowedChestStack = chest;
        swapChestArmorWith(containerSlot);

        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            stowedChestStack = ItemStack.EMPTY;
            return false;
        }

        stowedChestContainerSlot = containerSlot;
        weEquippedElytra = true;
        return true;
    }

    private void tickTakeoff() {
        if (mc.player.isFallFlying()) {
            state = State.ACTIVATE_ELYTRA;
            takeoffAirTicks = 0;
            return;
        }

        if (mc.player.onGround()) {
            takeoffAirTicks = 0;
            Vec3 vel = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(vel.x, TAKEOFF_VELOCITY, vel.z);
            mc.player.setSprinting(true);
            return;
        }

        if (++takeoffAirTicks < 2) {
            Vec3 vel = mc.player.getDeltaMovement();
            if (vel.y < TAKEOFF_VELOCITY) {
                mc.player.setDeltaMovement(vel.x, TAKEOFF_VELOCITY, vel.z);
            }
            return;
        }

        state = State.ACTIVATE_ELYTRA;
    }

    private void tickActivateElytra() {
        if (mc.player.isFallFlying()) {
            startFallFlyingSent = false;
            activateWaitTicks = 0;
            fireworkTimer = 0;
            boostNow();
            state = State.APPROACHING;
            return;
        }

        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            state = State.EQUIP_ELYTRA;
            return;
        }

        if (mc.player.onGround()) {
            state = State.TAKEOFF;
            return;
        }

        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        mc.player.startFallFlying();
        startFallFlyingSent = true;

        if (++activateWaitTicks > MAX_ACTIVATE_WAIT) {
            abortTo(State.FIND_TARGET);
        }
    }

    private void tickApproaching() {
        if (!requireFlight()) return;

        Vec3 predicted = predictedTargetPosition();
        if (horizontalDistanceTo(predicted) < POSITION_THRESHOLD) {
            state = State.ASCENDING;
            return;
        }

        boostNow();
        tickFireworks();
        submitAimRotation(clearAimPoint(predicted));
    }

    private void tickAscending() {
        if (!requireFlight()) return;

        Vec3 predicted = predictedTargetPosition();
        double gap = mc.player.position().y - predicted.y;
        double horiz = horizontalDistanceTo(predicted);

        if (gap >= height.getValue()) {
            if (attackCooldownTicks <= MAX_DIVE_COOLDOWN && !divePathBlocked(predicted)) {
                attackedThisDive = false;
                attackWaitTicks = 0;
                state = State.DIVING;
                return;
            }
        }

        if (horiz > POSITION_THRESHOLD + 2.5) {
            state = State.APPROACHING;
            return;
        }

        boostNow();
        tickFireworks();
        submitAimRotation(clearAimPoint(predicted));
    }

    private void tickDiving() {
        if (mc.player.onGround()) {
            if (weEquippedElytra
                    && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                    && canClickPlayerInventory()) {
                restoreChestplate();
            }
            state = isTargetValid() ? State.EQUIP_ELYTRA : State.FIND_TARGET;
            return;
        }

        Vec3 predicted = predictedTargetPosition();
        double gap = mc.player.position().y - predicted.y;
        double horiz = horizontalDistanceTo(predicted);

        if (horiz > range.getValue() + 1.5) {
            state = State.APPROACHING;
            return;
        }

        if (mc.player.isFallFlying()) {
            if (gap <= DIVE_CUT_GAP) {
                cutGlideAndSwap();
            } else {
                float[] angles = MathUtil.calcAngle(mc.player.position(), predicted);
                submitFlightRotation(angles[0], Mth.clamp(angles[1], -climbPitch.getValue(), VERTICAL_DIVE_PITCH));
            }
            return;
        }

        submitMomentumAwareAim();

        if (attackCooldownTicks > 0f) {
            if (++attackWaitTicks > MAX_ATTACK_WAIT) {
                attackWaitTicks = 0;
                state = State.RECOVER;
            }
            return;
        }

        boolean window = !attackedThisDive
                && mc.player.fallDistance > SMASH_FALL_DISTANCE
                && mc.player.getDeltaMovement().y < 0.0
                && gap >= -1.5
                && gap <= attackHeight.getValue() + 3.0
                && horiz <= range.getValue() + 2.0;

        if (!window) {
            if (++attackWaitTicks > MAX_ATTACK_WAIT) {
                attackWaitTicks = 0;
                state = State.RECOVER;
                return;
            }
            return;
        }

        attackWaitTicks = 0;
        boolean hit = attackLogic(currentTarget, range.getValue() + 2.0, false);
        if (hit) {
            // Reset everything after successful hit
            attackedThisDive = false;
            attackCooldownTicks = MACE_BASE_COOLDOWN * delay.getValue().floatValue();
            
            if (mc.player.isFallFlying()) {
                hasBoostedThisFlight = false;
                boostArmed = false;
                fireworkTimer = 0;
                state = State.ASCENDING;
            } else {
                // Full reset on ground hit
                state = State.FIND_TARGET;
            }
        } else if (++attackWaitTicks > MAX_ATTACK_WAIT) {
            attackWaitTicks = 0;
            state = State.RECOVER;
        }
    }

    private void cutGlideAndSwap() {
        if (mc.player.isFallFlying()) {
            preFlightVelocity = mc.player.getDeltaMovement();
            momentumCaptured = true;
            mc.player.stopFallFlying();
        }
        if (weEquippedElytra 
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && canClickPlayerInventory()) {
            restoreChestplate();
        }
    }

    private boolean restoreChestplate() {
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return true;

        if (weEquippedElytra && stowedChestContainerSlot != -1) {
            swapChestArmorWith(stowedChestContainerSlot);
            if (isChestOn(stowedChestStack)) {
                resetStowState();
                return true;
            }
        }

        Result chestplate = InventoryUtil.find(MaceAuraModule::isChestplate, InventoryUtil.FULL_SCOPE);
        if (!chestplate.found()) return false;
        swapChestArmorWith(containerSlotOf(chestplate));

        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return false;

        resetStowState();
        return true;
    }

    private boolean isChestOn(ItemStack expected) {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) return false;
        return expected.isEmpty() || chest.is(expected.getItem());
    }

    private void resetStowState() {
        weEquippedElytra = false;
        stowedChestContainerSlot = -1;
        stowedChestStack = ItemStack.EMPTY;
    }

    private void tickRecover() {
        attackedThisDive = false;
        state = isTargetValid() ? State.EQUIP_ELYTRA : State.FIND_TARGET;
    }

    private boolean attackLogic(Player target) {
        return attackLogic(target, range.getValue(), true);
    }

    private boolean attackLogic(Player target, double reach, boolean abandonTarget) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 closestPoint = getClosestPointToEye(eyePos, target.getBoundingBox());
        float[] angles = MathUtil.calcAngle(eyePos, closestPoint);
        boolean insideBox = target.getBoundingBox().contains(eyePos);

        if (eyePos.distanceTo(clampToBox(eyePos, target.getBoundingBox())) > reach) {
            if (abandonTarget) {
                currentTarget = null;
            }
            return false;
        }

        boolean canAttack = rotate.getValue() == RotateMode.NONE || insideBox;

        if (rotate.getValue() != RotateMode.NONE && !insideBox) {
            Pagon.rotationManager.setPlayerRotations(angles[0], angles[1]);

            EntityHitResult serverCheck = raycastTarget(
                target, reach,
                Pagon.rotationManager.getServerYaw(),
                Pagon.rotationManager.getServerPitch()
            );

            canAttack = serverCheck != null;
        }

        if (!canAttack) {
            return false;
        }

        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof MaceItem) {
            performAttack();
            return true;
        }

        if (!silentSwap.getValue()) {
            return false;
        }

        // Silent mace swap
        Result mace = InventoryUtil.find(MaceAuraModule::isMace, InventoryUtil.HOTBAR_SCOPE);
        if (!mace.found()) {
            return false;
        }

        boolean[] success = {false};
        InventoryUtil.withSwap(mace, swapResult -> {
            performAttack();
            success[0] = true;
        });

        return success[0];
    }

    private void performAttack() {
        mc.gameMode.attack(mc.player, currentTarget);
        if (swing.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private Vec3 predictedTargetPosition() {
        Vec3 pos = currentTarget.position();
        
        if (!prediction.getValue()) {
            return pos;
        }
        
        int ticks = predictionTicks.getValue();
        Vec3 vel = currentTarget.getDeltaMovement();
        
        Vec3 acceleration = Vec3.ZERO;
        if (predictionHistoryTicks > 0 && !lastTargetVel.equals(Vec3.ZERO)) {
            acceleration = vel.subtract(lastTargetVel);
        }
        
        lastTargetPos = pos;
        lastTargetVel = vel;
        predictionHistoryTicks++;
        
        float targetYaw = currentTarget.getYRot();
        float targetPitch = currentTarget.getXRot();
        Vec3 lookDirection = calculateLookVector(targetYaw, targetPitch);
        
        MovementState moveState = analyzeMovementState(currentTarget, vel);
        
        return simulateTargetMovement(pos, vel, acceleration, lookDirection, moveState, ticks);
    }
    
    private Vec3 simulateTargetMovement(Vec3 pos, Vec3 vel, Vec3 accel, Vec3 lookDir, MovementState state, int ticks) {
        Vec3 currentPos = pos;
        Vec3 currentVel = vel;
        
        for (int i = 0; i < ticks; i++) {
            currentVel = applyMovementPhysics(currentVel, accel, lookDir, state, currentPos);
            
            Vec3 nextPos = currentPos.add(currentVel);
            
            if (willCollideWithBlock(currentPos, nextPos)) {
                Vec3 slideVel = calculateSlideVelocity(currentPos, nextPos, currentVel);
                
                if (slideVel.lengthSqr() < 0.01) {
                    currentVel = Vec3.ZERO;
                    break;
                } else {
                    currentVel = slideVel;
                    nextPos = currentPos.add(currentVel);
                }
            }
            
            currentPos = nextPos;
            
            if (isPositionOnGround(currentPos)) {
                currentVel = currentVel.multiply(GROUND_FRICTION, 1.0, GROUND_FRICTION);
            }
        }
        
        return currentPos;
    }
    
    private Vec3 applyMovementPhysics(Vec3 vel, Vec3 accel, Vec3 lookDir, MovementState state, Vec3 pos) {
        double velMagnitude = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        
        Vec3 newVel = vel;
        
        if (accel.lengthSqr() > 0.001) {
            newVel = newVel.add(accel.scale(0.5));
        }
        
        if (velMagnitude > MIN_MOVEMENT_THRESHOLD) {
            Vec3 normalizedVel = new Vec3(vel.x, 0, vel.z).normalize();
            Vec3 horizontalLook = new Vec3(lookDir.x, 0, lookDir.z).normalize();
            
            double alignment = normalizedVel.dot(horizontalLook);
            
            double lookInfluence = alignment > 0 ? LOOK_BLEND_MOVING : LOOK_BLEND_MOVING * 0.5;
            double velInfluence = 1.0 - lookInfluence;
            
            Vec3 blendedDir = normalizedVel.scale(velInfluence)
                                          .add(horizontalLook.scale(lookInfluence))
                                          .normalize();
            
            double speedMod = getSpeedModifier(state);
            newVel = new Vec3(
                blendedDir.x * velMagnitude * speedMod,
                vel.y,
                blendedDir.z * velMagnitude * speedMod
            );
        } else {
            Vec3 horizontalLook = new Vec3(lookDir.x, 0, lookDir.z).normalize();
            newVel = horizontalLook.scale(0.08);
        }
        
        if (!isPositionOnGround(pos)) {
            double yVel = newVel.y;
            
            if (state == MovementState.ELYTRA_FLYING) {
                yVel = Math.max(yVel - GRAVITY_ACCELERATION * 0.5, -1.5);
                newVel = new Vec3(
                    newVel.x * 0.99,
                    yVel,
                    newVel.z * 0.99
                );
            } else {
                yVel -= GRAVITY_ACCELERATION;
                yVel = Math.max(yVel, -3.0);
                newVel = new Vec3(
                    newVel.x * AIR_RESISTANCE,
                    yVel,
                    newVel.z * AIR_RESISTANCE
                );
            }
        }
        
        return newVel;
    }
    
    private MovementState analyzeMovementState(Player target, Vec3 vel) {
        if (target.isFallFlying()) {
            return MovementState.ELYTRA_FLYING;
        }
        
        if (target.isSprinting()) {
            return MovementState.SPRINTING;
        }
        
        if (target.isShiftKeyDown()) {
            return MovementState.SNEAKING;
        }
        
        double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        
        if (horizSpeed < 0.02) {
            return MovementState.STATIONARY;
        }
        
        if (target.onGround()) {
            return MovementState.WALKING;
        }
        
        return MovementState.AIRBORNE;
    }
    
    private double getSpeedModifier(MovementState state) {
        return switch (state) {
            case SPRINTING -> SPRINT_MULTIPLIER;
            case SNEAKING -> SNEAK_MULTIPLIER;
            case ELYTRA_FLYING -> 1.5;
            case WALKING -> 1.0;
            case AIRBORNE -> 0.95;
            case STATIONARY -> 0.1;
        };
    }
    
    private boolean willCollideWithBlock(Vec3 from, Vec3 to) {
        if (mc.level == null) return false;
        
        AABB playerBox = currentTarget.getBoundingBox();
        Vec3 motion = to.subtract(from);
        AABB sweepBox = playerBox.move(motion.x, motion.y, motion.z).inflate(0.1);
        
        return mc.level.getBlockCollisions(null, sweepBox).iterator().hasNext();
    }
    
    private Vec3 calculateSlideVelocity(Vec3 from, Vec3 to, Vec3 vel) {
        if (mc.level == null) return Vec3.ZERO;
        
        Vec3 xTest = from.add(vel.x, 0, 0);
        Vec3 zTest = from.add(0, 0, vel.z);
        
        boolean xBlocked = willCollideWithBlock(from, xTest);
        boolean zBlocked = willCollideWithBlock(from, zTest);
        
        if (xBlocked && zBlocked) {
            return Vec3.ZERO;
        }
        
        if (xBlocked) {
            return new Vec3(0, vel.y, vel.z * 0.8);
        }
        
        if (zBlocked) {
            return new Vec3(vel.x * 0.8, vel.y, 0);
        }
        
        return vel;
    }
    
    private boolean isPositionOnGround(Vec3 pos) {
        if (mc.level == null) return false;
        
        Vec3 below = pos.add(0, -0.1, 0);
        AABB checkBox = new AABB(
            pos.x - 0.3, below.y, pos.z - 0.3,
            pos.x + 0.3, pos.y, pos.z + 0.3
        );
        
        return mc.level.getBlockCollisions(null, checkBox).iterator().hasNext();
    }
    
    private Vec3 calculateLookVector(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        
        return new Vec3(x, y, z).normalize();
    }
    
    private enum MovementState {
        STATIONARY,
        WALKING,
        SPRINTING,
        SNEAKING,
        AIRBORNE,
        ELYTRA_FLYING
    }

    private boolean requireFlight() {
        if (mc.player.isFallFlying()) return true;
        if (mc.player.onGround() || !mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            state = State.EQUIP_ELYTRA;
        } else {
            state = State.ACTIVATE_ELYTRA;
        }
        return false;
    }

    private void boostNow() {
        if (!autoFirework.getValue() || !isTargetValid()) return;
        if (!mc.player.isFallFlying()) return;

        Vec3 predicted = predictedTargetPosition();
        double pitch = mc.player.position().y < predicted.y + height.getValue()
            ? -climbPitch.getValue() : 0.0;
        submitFlightRotation(calculateYawTo(predicted), pitch);

        if (hasBoostedThisFlight) return;

        if (!boostArmed) {
            boostArmed = true;
            return;
        }

        if (useFirework()) {
            hasBoostedThisFlight = true;
            boostArmed = false;
        }
    }

    private void submitAimRotation(Vec3 goal) {
        float[] angles = MathUtil.calcAngle(mc.player.position(), goal);
        float yaw = angles[0];
        double pitch = Mth.clamp(angles[1], -climbPitch.getValue(), descentPitch.getValue());
        submitFlightRotation(yaw, pitch);
    }

    private void tickFireworks() {
        if (!autoFirework.getValue()) return;
        if (!mc.player.isFallFlying()) return;
        if (!isTargetValid()) return;
        if (mc.player.tickCount - lastAimTick < 1) return;

        if (++fireworkTimer >= fireworkDelay.getValue()) {
            if (useFirework()) fireworkTimer = 0;
        }
    }

    private boolean useFirework() {
        if (!mc.player.isFallFlying()) return false;

        Result rocket = InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.FULL_SCOPE);
        if (!rocket.found()) return false;

        boolean[] success = {false};
        InventoryUtil.withSwap(rocket, swapResult -> {
            // Use gameMode.useItem for reliable item usage
            mc.gameMode.useItem(mc.player, swapResult.hand());
            success[0] = true;
        });

        return success[0];
    }

    private Vec3 clearAimPoint(Vec3 predicted) {
        Vec3 from = mc.player.position();
        double y = predicted.y + height.getValue();
        for (int i = 0; i < 8 && isBlocked(from, new Vec3(predicted.x, y, predicted.z)); i++) {
            y += 4.0;
        }
        return new Vec3(predicted.x, y, predicted.z);
    }

    private boolean isBlocked(Vec3 from, Vec3 to) {
        if (mc.level == null || mc.player == null) return false;
        BlockHitResult hit = mc.level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private boolean divePathBlocked(Vec3 predicted) {
        return isBlocked(
            new Vec3(predicted.x, mc.player.position().y, predicted.z),
            new Vec3(predicted.x, predicted.y + 0.5, predicted.z));
    }

    private void submitFlightRotation(float yaw, double pitch) {
        lastAimTick = mc.player.tickCount;
        Pagon.rotationManager.setPlayerRotations(yaw, (float) Mth.clamp(pitch, -90f, 90f));
    }

    private float calculateYawTo(Vec3 target) {
        Vec3 playerPos = mc.player.position();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    private double horizontalDistanceTo(Vec3 target) {
        Vec3 playerPos = mc.player.position();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isTargetInMeleeRange() {
        if (currentTarget == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        return eye.distanceTo(clampToBox(eye, currentTarget.getBoundingBox())) <= range.getValue();
    }

    private Vec3 getMomentumAwareAttackPoint() {
        Vec3 playerPos = mc.player.position();
        Vec3 vel = momentumCaptured ? preFlightVelocity : mc.player.getDeltaMovement();
        
        Vec3 predictedTarget = predictedTargetPosition();
        
        double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        double horizDist = horizontalDistanceTo(predictedTarget);
        double horizon = horizSpeed > 0.05
            ? Mth.clamp(horizDist / horizSpeed, 0.25, ATTACK_AIM_HORIZON_TICKS)
            : 1.0;

        Vec3 futurePlayer = playerPos.add(vel.x * horizon, 0.0, vel.z * horizon);
        
        return playerPos.add(predictedTarget.subtract(futurePlayer));
    }

    private void submitMomentumAwareAim() {
        if (currentTarget == null) return;
        if (rotate.getValue() == RotateMode.NONE) return;

        Vec3 point = getMomentumAwareAttackPoint();
        float[] angles = MathUtil.calcAngle(mc.player.position(), point);
        double pitch = Mth.clamp(angles[1], -climbPitch.getValue(), VERTICAL_DIVE_PITCH);
        submitFlightRotation(angles[0], pitch);
    }

    private void swapChestArmorWith(int itemContainerSlot) {
        AbstractContainerMenu handler = mc.player.containerMenu;
        Pagon.inventoryManager.pickupSlot(handler, itemContainerSlot);
        Pagon.inventoryManager.pickupSlot(handler, CHEST_MENU_SLOT);
        Pagon.inventoryManager.pickupSlot(handler, itemContainerSlot);
    }

    private int containerSlotOf(Result result) {
        int slot = result.slot();
        return slot < 9 ? slot + 36 : slot;
    }

    private static boolean isChestplate(ItemStack stack) {
        if (stack.is(Items.ELYTRA)) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static boolean isMace(ItemStack stack) {
        return stack.getItem() instanceof MaceItem;
    }

    private boolean canClickPlayerInventory() {
        if (mc.gameMode == null || mc.player == null) return false;
        AbstractContainerMenu menu = mc.player.containerMenu;
        return menu != null && (menu == mc.player.inventoryMenu || menu.containerId == 0);
    }

    private boolean isTargetValid() {
        if (currentTarget == null) return false;
        if (!currentTarget.isAlive()) return false;
        if (currentTarget.isDeadOrDying()) return false;
        
        if (Pagon.friendManager != null && Pagon.friendManager.isFriend(currentTarget)) return false;
        
        return true;
    }

    private void invalidateTarget() {
        currentTarget = null;
        attackedThisDive = false;
        state = State.FIND_TARGET;
        
        lastTargetPos = Vec3.ZERO;
        lastTargetVel = Vec3.ZERO;
        predictionHistoryTicks = 0;
    }

    private void abortTo(State next) {
        attackedThisDive = false;
        state = next;
    }

    private void resetAerialState(boolean restoreArmor) {
        if (mc.player != null && mc.player.isFallFlying()) {
            mc.player.stopFallFlying();
        }

        if (restoreArmor && mc.player != null
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && weEquippedElytra
                && stowedChestContainerSlot != -1
                && canClickPlayerInventory()) {
            swapChestArmorWith(stowedChestContainerSlot);
        }

        currentTarget = null;
        state = State.IDLE;
        attackCooldownTicks = 0f;
        fireworkTimer = 0;
        activateWaitTicks = 0;
        attackWaitTicks = 0;
        takeoffAirTicks = 0;
        hasBoostedThisFlight = false;
        boostArmed = false;
        attackedThisDive = false;
        startFallFlyingSent = false;
        weEquippedElytra = false;
        stowedChestContainerSlot = -1;
        stowedChestStack = ItemStack.EMPTY;
        preFlightVelocity = Vec3.ZERO;
        momentumCaptured = false;
        
        lastTargetPos = Vec3.ZERO;
        lastTargetVel = Vec3.ZERO;
        predictionHistoryTicks = 0;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue() || currentTarget == null || nullCheck() || mc.player.isDeadOrDying()) return;

        Color color = Pagon.colorManager.getColor();
        drawBox(currentTarget, color, event.getMatrix(), event.getDelta());
    }

    private void drawBox(Player entity, Color color, com.mojang.blaze3d.vertex.PoseStack matrices, float partialTicks) {
        double interpX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double interpY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double interpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
        AABB box = entity.getBoundingBox().move(
            interpX - entity.getX(), 
            interpY - entity.getY(), 
            interpZ - entity.getZ()
        );

        RenderUtil.drawBox(matrices, box, color, 1.5f);
    }

    private Vec3 getClosestPointToEye(Vec3 eye, AABB box) {
        double x = Mth.clamp(eye.x, box.minX, box.maxX);
        double y = Mth.clamp(eye.y, box.minY, box.maxY);
        double z = Mth.clamp(eye.z, box.minZ, box.maxZ);

        final double VEC = 1.0 / 16.0;
        final double EPS = 1e-9;
        if (Math.abs(x - box.minX) < EPS) x = Math.min(box.minX + VEC, box.maxX - EPS);
        else if (Math.abs(x - box.maxX) < EPS) x = Math.max(box.maxX - VEC, box.minX + EPS);
        if (Math.abs(z - box.minZ) < EPS) z = Math.min(box.minZ + VEC, box.maxZ - EPS);
        else if (Math.abs(z - box.maxZ) < EPS) z = Math.max(box.maxZ - VEC, box.minZ + EPS);

        return new Vec3(x, y, z);
    }

    private Vec3 clampToBox(Vec3 point, AABB box) {
        return new Vec3(
            Mth.clamp(point.x, box.minX, box.maxX),
            Mth.clamp(point.y, box.minY, box.maxY),
            Mth.clamp(point.z, box.minZ, box.maxZ)
        );
    }

    private EntityHitResult raycastTarget(Player target, double range, float yaw, float pitch) {
        Vec3 eyePos = mc.player.getEyePosition();
        float f = yaw * (float) (Math.PI / 180.0);
        float g = -pitch * (float) (Math.PI / 180.0);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        Vec3 lookVec = new Vec3(Mth.sin(f) * h, -i, Mth.cos(f) * h);
        Vec3 reachEnd = eyePos.add(lookVec.scale(range));

        return target.getBoundingBox().clip(eyePos, reachEnd)
            .map(hit -> new EntityHitResult(target, hit))
            .orElse(null);
    }

    @Override
    public String getDisplayInfo() {
        return state.name();
    }
}
