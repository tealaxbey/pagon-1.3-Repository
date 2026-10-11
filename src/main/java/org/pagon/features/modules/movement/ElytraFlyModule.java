package org.pagon.features.modules.movement;

import org.pagon.Pagon;
import org.pagon.event.impl.entity.player.PlayerMoveEvent;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.network.PacketEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

public class ElytraFlyModule extends Module {
    
    private static final String REQUEST_ID = "ElytraFly";
    private static ElytraFlyModule INSTANCE;
    
    private final Setting<FlightMode> mode = register(new Setting<>("Mode", FlightMode.CONTROL));
    
    private final Setting<RocketMode> rocketMode = register(new Setting<>("RocketMode", RocketMode.AUTOMATIC, 
            v -> mode.getValue() == FlightMode.CONTROL));
    private final Setting<Integer> rocketDelay = register(new Setting<>("RocketDelay", 2250, 100, 10000, 
            v -> mode.getValue() == FlightMode.CONTROL && rocketMode.getValue() == RocketMode.STATIC, "Fixed delay between rockets (ms)"));
    private final Setting<Integer> retryDelay = register(new Setting<>("RetryDelay", 200, 50, 5000, 
            v -> mode.getValue() == FlightMode.CONTROL, "Delay before retrying rockets (ms)"));
    private final Setting<Boolean> offhand = register(new Setting<>("Offhand", false, 
            v -> mode.getValue() == FlightMode.CONTROL));
    private final Setting<ControlMode> controlMode = register(new Setting<>("ControlMode", ControlMode.SNAP, 
            v -> mode.getValue() == FlightMode.CONTROL));
    private final Setting<Float> hoverPitch = register(new Setting<>("HoverPitch", -1.1f, -10f, 10f, 
            v -> mode.getValue() == FlightMode.CONTROL, "Hover height when no movement keys are pressed"));
    private final Setting<Float> glidePitch = register(new Setting<>("GlidePitch", -1.1f, -10f, 10f, 
            v -> mode.getValue() == FlightMode.CONTROL, "Pitch angle when gliding without jump or sneak"));
    private final Setting<Boolean> autoSwap = register(new Setting<>("AutoSwap", false, 
            v -> mode.getValue() == FlightMode.CONTROL));
    
    private final Setting<Boolean> motionYBoost = register(new Setting<>("MotionYBoost", false, 
            v -> mode.getValue() == FlightMode.BOUNCE));
    private final Setting<Boolean> onlyWhileColliding = register(new Setting<>("OnlyWhileColliding", true, 
            v -> mode.getValue() == FlightMode.BOUNCE && motionYBoost.getValue()));
    private final Setting<Boolean> tunnelBounce = register(new Setting<>("TunnelBounce", false, 
            v -> mode.getValue() == FlightMode.BOUNCE && motionYBoost.getValue()));
    private final Setting<Double> bounceSpeed = register(new Setting<>("BounceSpeed", 100.0, 20.0, 250.0, 
            v -> mode.getValue() == FlightMode.BOUNCE && motionYBoost.getValue(), "Target speed in blocks per second"));
    private final Setting<Boolean> bounceLockPitch = register(new Setting<>("BounceLockPitch", true, 
            v -> mode.getValue() == FlightMode.BOUNCE));
    private final Setting<Float> bouncePitch = register(new Setting<>("BouncePitch", 90f, -90f, 90f, 
            v -> mode.getValue() == FlightMode.BOUNCE && bounceLockPitch.getValue(), "Pitch angle to lock to"));
    
    private final Setting<Double> lowerBounds = register(new Setting<>("LowerBounds", 120.0, 0.0, 255.0, 
            v -> mode.getValue() == FlightMode.BOOST, "Lower Y boundary for pitch oscillation"));
    private final Setting<Double> upperBounds = register(new Setting<>("UpperBounds", 200.0, 0.0, 255.0, 
            v -> mode.getValue() == FlightMode.BOOST, "Upper Y boundary for pitch oscillation"));
    private final Setting<Double> rotationSpeedUp = register(new Setting<>("RotationSpeedUp", 5.45, 0.1, 10.0, 
            v -> mode.getValue() == FlightMode.BOOST, "Pitch change speed when ascending"));
    private final Setting<Double> rotationSpeedDown = register(new Setting<>("RotationSpeedDown", 0.90, 0.1, 5.0, 
            v -> mode.getValue() == FlightMode.BOOST, "Pitch change speed when descending"));
    private final Setting<Boolean> boostAutoFirework = register(new Setting<>("BoostAutoFirework", true, 
            v -> mode.getValue() == FlightMode.BOOST));
    private final Setting<Integer> boostFireworkDelay = register(new Setting<>("BoostFireworkDelay", 5000, 500, 5000, 
            v -> mode.getValue() == FlightMode.BOOST && boostAutoFirework.getValue(), "Delay between firework boosts (ms)"));
    
    public enum FlightMode {
        CONTROL("Control"),
        BOUNCE("Bounce"),
        BOOST("Boost");
        
        private final String name;
        FlightMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    
    public enum RocketMode {
        AUTOMATIC("Automatic"),
        STATIC("Static");
        
        private final String name;
        RocketMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    
    public enum ControlMode {
        SMOOTH("Smooth"),
        SNAP("Snap");
        
        private final String name;
        ControlMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    
    private long lastRocket = 0L;
    private long rocketSuppressUntil = 0L;
    private boolean hopNextTick = false;
    private int airTicks = 0;
    private int notGlidingRetryTicks = 0;
    private int glideRetriesThisLaunch = 0;
    private boolean wasInWater = false;
    private boolean waterLaunchArmed = false;
    private static final int NOT_GLIDING_RETRY_PERIOD = 5;
    private static final int MAX_GLIDE_RETRIES = 2;
    private boolean wasFlying = false;
    private int idleTickPhase = 0;
    private boolean smoothPrimed = false;
    private float smoothYaw = 0f;
    private float smoothPitch = 0f;
    private volatile float desiredYaw = 0f;
    private volatile float desiredPitch = 0f;
    
    private boolean pitchingDown = true;
    private float boostPitch = 37.72F;
    private long lastBoostFirework = 0L;
    
    public ElytraFlyModule() {
        super("ElytraFly", "Advanced elytra flight with multiple modes", Category.MOVEMENT);
        INSTANCE = this;
    }
    
    public static ElytraFlyModule get() {
        return INSTANCE;
    }
    
    @Override
    public void onEnable() {
        resetState();
        
        if (nullCheck()) return;
        
        FlightMode currentMode = mode.getValue();
        
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            equipElytra();
        }
        
        if (currentMode == FlightMode.CONTROL) {
            if (mc.player.onGround() && autoSwap.getValue() && hasAnyRockets()) {
                equipElytra();
                mc.player.jumpFromGround();
                Vec3 vel = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(vel.x, vel.y + 0.1, vel.z);
            }
        } else if (currentMode == FlightMode.BOOST) {
            if (mc.player.getY() < upperBounds.getValue()) {
                sendMessage("Player must be above upper bounds!", ChatFormatting.RED);
                disable();
                return;
            } else if (mc.player.getY() - 40 < lowerBounds.getValue()) {
                sendMessage("Player must be at least 40 blocks above the lower bounds!", ChatFormatting.RED);
                disable();
                return;
            }
            boostPitch = 37.72F;
        }
    }
    
    @Override
    public void onDisable() {
        resetState();
        if (!nullCheck()) {
            Pagon.rotationManager.syncToClientCamera();
        }
    }
    
    private void resetState() {
        lastRocket = 0L;
        rocketSuppressUntil = 0L;
        hopNextTick = false;
        airTicks = 0;
        notGlidingRetryTicks = 0;
        glideRetriesThisLaunch = 0;
        wasInWater = false;
        waterLaunchArmed = false;
        wasFlying = false;
        idleTickPhase = 0;
        smoothPrimed = false;
        pitchingDown = true;
        boostPitch = 37.72F;
        lastBoostFirework = 0L;
    }
    
    @Subscribe
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        
        FlightMode currentMode = mode.getValue();
        
        switch (currentMode) {
            case CONTROL -> tickControl();
            case BOUNCE -> tickBounce();
            case BOOST -> tickBoost();
        }
    }
    
    @Subscribe
    public void onPlayerMove(PlayerMoveEvent event) {
        if (nullCheck()) return;
        
        if (mode.getValue() == FlightMode.BOUNCE) {
            handleBounceMotion(event);
        }
    }
    
    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        
        if (mode.getValue() == FlightMode.CONTROL) {
            handleControlPacket(event);
        }
    }
    
    private void tickControl() {
        if (!hasAnyRockets()) {
            wasFlying = mc.player.isFallFlying();
            return;
        }
        
        boolean flying = mc.player.isFallFlying();
        
        if (autoSwap.getValue()) {
            if (flying && !wasFlying) {
                equipElytra();
            } else if (!flying && wasFlying) {
                if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                    swapBackToChestplate();
                }
            }
        }
        wasFlying = flying;
        
        tickElytra();
        
        if (!isEnabled()) return;
        
        tickFlightControl();
    }
    
    private void tickElytra() {
        if (mc.player.onGround()) {
            airTicks = 0;
            notGlidingRetryTicks = 0;
            glideRetriesThisLaunch = 0;
            rocketSuppressUntil = 0L;
            
            if (!autoSwap.getValue() && !mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                hopNextTick = false;
                return;
            }
            
            if (!hopNextTick) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                hopNextTick = true;
                return;
            }
            
            hopNextTick = false;
            equipElytra();
            
            mc.player.jumpFromGround();
            Vec3 vel = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(vel.x, vel.y + 0.1, vel.z);
            
            startGlideAndBoost();
            return;
        }
        
        hopNextTick = false;
        airTicks++;
        
        boolean water = isWaterBlocked();
        if (water && !mc.player.isFallFlying()) {
            wasInWater = true;
            notGlidingRetryTicks = 0;
            if (shouldRocket()) fireRocket();
            return;
        }
        if (wasInWater && !water) {
            wasInWater = false;
            glideRetriesThisLaunch = 0;
            notGlidingRetryTicks = 0;
            if (waterLaunchArmed) {
                waterLaunchArmed = false;
                startGlideAndBoost();
                return;
            }
            requestGlide();
            if (shouldRocket()) fireRocket();
            return;
        }
        
        if (airTicks == 1) {
            glideRetriesThisLaunch = 0;
            requestGlide();
        } else if (airTicks == 11 && !mc.player.isFallFlying() && glideRetriesThisLaunch < MAX_GLIDE_RETRIES) {
            glideRetriesThisLaunch++;
            requestGlide();
        } else if (!mc.player.isFallFlying()) {
            if (glideRetriesThisLaunch < MAX_GLIDE_RETRIES && ++notGlidingRetryTicks >= NOT_GLIDING_RETRY_PERIOD) {
                notGlidingRetryTicks = 0;
                glideRetriesThisLaunch++;
                requestGlide();
            }
        } else {
            notGlidingRetryTicks = 0;
        }
        
        if (shouldRocket()) {
            fireRocket();
        }
    }
    
    private void tickFlightControl() {
        boolean isFlying = mc.player.isFallFlying();
        
        if (!isFlying || mc.player.onGround()) {
            idleTickPhase = 0;
            smoothPrimed = false;
            return;
        }
        
        boolean pressJump = mc.options.keyJump.isDown();
        boolean pressSneak = mc.options.keyShift.isDown();
        boolean pressW = mc.options.keyUp.isDown();
        boolean pressA = mc.options.keyLeft.isDown();
        boolean pressS = mc.options.keyDown.isDown();
        boolean pressD = mc.options.keyRight.isDown();
        boolean anyKey = pressJump || pressSneak || pressW || pressA || pressS || pressD;
        
        float yawBase = mc.player.getYRot();
        
        float dirX = 0f, dirZ = 0f;
        if (pressW) dirZ += 1f;
        if (pressS) dirZ -= 1f;
        if (pressA) dirX -= 1f;
        if (pressD) dirX += 1f;
        boolean hasHorizontal = dirX != 0f || dirZ != 0f;
        
        if (!anyKey) {
            boolean lookBack = idleTickPhase++ % 2 == 1;
            float yaw = lookBack ? Mth.wrapDegrees(yawBase + 180f) : yawBase;
            smoothPrimed = false;
            applyRotation(yaw, hoverPitch.getValue());
            return;
        }
        
        idleTickPhase = 0;
        
        float pitch = pressJump ? (hasHorizontal ? -45f : -90f)
                : pressSneak ? (hasHorizontal ? 45f : 90f)
                : glidePitch.getValue();
        
        float yaw = yawBase;
        if (hasHorizontal) {
            double angle = Math.toDegrees(Math.atan2(dirX, dirZ));
            yaw = Mth.wrapDegrees(yawBase + (float) angle);
        }
        
        if (controlMode.getValue() == ControlMode.SMOOTH) {
            if (!smoothPrimed) {
                smoothYaw = yawBase;
                smoothPitch = mc.player.getXRot();
                smoothPrimed = true;
            }
            smoothYaw = Mth.wrapDegrees(Mth.rotLerp(0.35f, smoothYaw, yaw));
            smoothPitch = Mth.lerp(0.35f, smoothPitch, pitch);
            yaw = smoothYaw;
            pitch = smoothPitch;
        } else {
            smoothPrimed = false;
            smoothYaw = yaw;
            smoothPitch = pitch;
        }
        
        applyRotation(yaw, pitch);
    }
    
    private void applyRotation(float yaw, float pitch) {
        desiredYaw = yaw;
        desiredPitch = pitch;
        Pagon.rotationManager.setPlayerRotations(yaw, pitch);
    }
    
    private void handleControlPacket(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundContainerClosePacket)) return;
        if (!mc.player.isFallFlying()) return;
        if (mc.screen == null) return;
        event.cancel();
    }
    
    private void tickBounce() {
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        
        if (bounceLockPitch.getValue()) {
            Pagon.rotationManager.setPlayerPitch(bouncePitch.getValue());
        }
        
        if (!mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
        
        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        }
        
        if (!mc.player.onGround() && !mc.player.isFallFlying() && !mc.player.isInWater()) {
            sendStartFlyingPacket();
            mc.player.startFallFlying();
        }
    }
    
    private void handleBounceMotion(PlayerMoveEvent event) {
        if (!motionYBoost.getValue()) return;
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        
        if (!mc.player.isFallFlying() && !mc.player.onGround()) return;
        
        if (onlyWhileColliding.getValue() && !mc.player.horizontalCollision) return;
        
        double currentSpeed = getHorizontalSpeed();
        if (currentSpeed < bounceSpeed.getValue() || tunnelBounce.getValue()) {
            event.setY(0.0);
        }
    }
    
    private void tickBoost() {
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        
        if (pitchingDown && mc.player.getY() <= lowerBounds.getValue()) {
            pitchingDown = false;
        } else if (!pitchingDown && mc.player.getY() >= upperBounds.getValue()) {
            pitchingDown = true;
        }
        
        if (!pitchingDown) {
            boostPitch -= randPitch(rotationSpeedUp.getValue().floatValue(), 1.0F);
            
            if (boostPitch < -54.77F) {
                boostPitch = -54.77F;
                pitchingDown = true;
            }
        } else if (boostPitch < 37.72F) {
            boostPitch += randPitch(rotationSpeedDown.getValue().floatValue(), 0.50F);
        }
        
        Pagon.rotationManager.setPlayerPitch(boostPitch);
        
        if (!mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
        
        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        }
        
        if (!mc.player.onGround() && !mc.player.isFallFlying() && !mc.player.isInWater()) {
            sendStartFlyingPacket();
            mc.player.startFallFlying();
        }
        
        if (boostAutoFirework.getValue() && mc.player.isFallFlying()) {
            long now = System.currentTimeMillis();
            if (now - lastBoostFirework >= boostFireworkDelay.getValue()) {
                fireBoostFirework();
                lastBoostFirework = now;
            }
        }
    }
    
    private float randPitch(float pitch, float bound) {
        return (float) (pitch + (bound * (Math.random() - 0.5)));
    }
    
    private boolean isWaterBlocked() {
        return mc.player.isInWater() || mc.player.isInLava();
    }
    
    private boolean canGlideNow() {
        return !mc.player.onGround() && !isWaterBlocked();
    }
    
    private void requestGlide() {
        sendGlide(true);
    }
    
    private void sendGlide(boolean updateLocalPose) {
        if (!canGlideNow()) return;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.startFallFlying();
    }
    
    private void startGlideAndBoost() {
        if (!canGlideNow()) {
            waterLaunchArmed = true;
            return;
        }
        sendGlide(true);
        airTicks = 1;
        rocketSuppressUntil = 0L;
        if (!hasActiveRocket()) {
            fireRocketImmediate();
        }
    }
    
    private void equipElytra() {
        if (!autoSwap.getValue() && mode.getValue() != FlightMode.CONTROL) {
            Result result = InventoryUtil.find(Items.ELYTRA, InventoryUtil.FULL_SCOPE);
            if (result.found()) {
                int slot = result.slot();
                InventoryUtil.swapToOffhand(slot);
                InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
                InventoryUtil.click(6, 0, net.minecraft.world.inventory.ClickType.PICKUP);
                InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            }
            return;
        }
        
        if (mode.getValue() == FlightMode.CONTROL && !autoSwap.getValue()) return;
        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        
        Result result = InventoryUtil.find(Items.ELYTRA, InventoryUtil.FULL_SCOPE);
        if (result.found()) {
            int slot = result.slot();
            InventoryUtil.swapToOffhand(slot);
            InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtil.click(6, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
        }
    }
    
    private void swapBackToChestplate() {
        Result result = InventoryUtil.find(stack -> 
            stack.getItem().toString().toLowerCase().contains("chestplate"),
            InventoryUtil.FULL_SCOPE
        );
        if (result.found()) {
            int slot = result.slot();
            InventoryUtil.swapToOffhand(slot);
            InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtil.click(6, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtil.click(45, 0, net.minecraft.world.inventory.ClickType.PICKUP);
        }
    }
    
    private boolean hasActiveRocket() {
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FireworkRocketEntity r && r.getOwner() == mc.player) {
                return true;
            }
        }
        return false;
    }
    
    private boolean shouldRocket() {
        long now = System.currentTimeMillis();
        if (isWaterBlocked() && !mc.player.isFallFlying()) return false;
        if (now < rocketSuppressUntil) return false;
        
        long since = now - lastRocket;
        if (rocketMode.getValue() == RocketMode.STATIC) {
            return since > rocketDelay.getValue();
        }
        if (hasActiveRocket()) return false;
        return since > retryDelay.getValue();
    }
    
    private int findRocketInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) return i;
        }
        return -1;
    }
    
    private boolean hasAnyRockets() {
        if (mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET)) return true;
        if (findRocketInHotbar() != -1) return true;
        return InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.FULL_SCOPE).found();
    }
    
    private void fireRocketImmediate() {
        if (hasActiveRocket()) {
            rocketSuppressUntil = System.currentTimeMillis() + retryDelay.getValue();
            return;
        }
        long now = System.currentTimeMillis();
        lastRocket = now;
        fireRocket();
        rocketSuppressUntil = now + retryDelay.getValue();
    }
    
    private void fireRocket() {
        int hot = findRocketInHotbar();
        Result invResult = InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.FULL_SCOPE);
        boolean offhandHasRocket = mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET);
        
        if (offhand.getValue()) {
            if (!offhandHasRocket && hot == -1 && !invResult.found()) {
                return;
            }
            lastRocket = System.currentTimeMillis();
            if (offhandHasRocket) {
                mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            } else {
                int src = hot != -1 ? hot : invResult.slot();
                InventoryUtil.swapToOffhand(src);
                mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
                InventoryUtil.swapToOffhand(src);
            }
            rocketSuppressUntil = System.currentTimeMillis() + retryDelay.getValue();
            return;
        }
        
        if (hot == -1 && !invResult.found()) {
            return;
        }
        int selected = InventoryUtil.selected();
        
        lastRocket = System.currentTimeMillis();
        
        if (hot == selected) {
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
        } else {
            int src = hot != -1 ? hot : invResult.slot();
            InventoryUtil.swap(src);
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            InventoryUtil.swap(selected);
        }
        rocketSuppressUntil = System.currentTimeMillis() + retryDelay.getValue();
    }
    
    private void sendStartFlyingPacket() {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
            mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }
    
    private void fireBoostFirework() {
        Result result = InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.FULL_SCOPE);
        if (!result.found()) return;
        
        boolean offhandHasRocket = mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET);
        int hotbarRocket = findRocketInHotbar();
        
        if (offhandHasRocket) {
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
        } else if (hotbarRocket != -1) {
            int selected = InventoryUtil.selected();
            InventoryUtil.swap(hotbarRocket);
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            InventoryUtil.swap(selected);
        } else {
            int slot = result.slot();
            InventoryUtil.swapToOffhand(slot);
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            InventoryUtil.swapToOffhand(slot);
        }
    }
    
    private double getHorizontalSpeed() {
        Vec3 vel = mc.player.getDeltaMovement();
        return Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0;
    }
    
    private void sendMessage(String message, ChatFormatting color) {
        if (mc.player != null && mc.gui != null) {
            mc.gui.getChat().addMessage(
                net.minecraft.network.chat.Component.literal(color + message)
            );
        }
    }
    
    @Override
    public String getDisplayInfo() {
        return mode.getValue().toString();
    }
}
