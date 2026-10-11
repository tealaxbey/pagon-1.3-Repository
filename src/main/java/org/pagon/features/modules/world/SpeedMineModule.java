package org.pagon.features.modules.world;

import org.pagon.Pagon;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.world.AttackBlockEvent;
import org.pagon.event.impl.render.Render3DEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.ColorSettingMode;
import org.pagon.features.settings.Setting;
import org.pagon.util.InteractionUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.ResultType;
import org.pagon.util.inventory.SwapMode;
import org.pagon.util.inventory.SwapPriority;
import org.pagon.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpeedMineModule extends Module implements MineApi {

    private static final int DECOY_Y_OFFSET = 2000;

    private static final double GRIM_MIN_EYE = 0.4;
    private static final double GRIM_MAX_EYE = 1.62;

    /** ticks past the predicted server completion before the secondary is written off. */
    private static final int SECONDARY_TIMEOUT = 10;

    /** how far inside a face the BreakAhead ray has to exit for it to count as straight-through. */
    private static final double BREAK_AHEAD_EDGE = 0.05;

    private final Setting<Double> threshold = num("Threshold", 0.7, 0.1, 1.0);
    private final Setting<Integer> fudgeTicks = num("FudgeTicks", 1, 0, 5);
    private final Setting<Integer> breakDelay = num("BreakDelay", 6, 0, 10);
    private final Setting<Boolean> pauseOnEat = bool("PauseOnEat", true);
    private final Setting<Boolean> decoy = bool("GrimDecoy", true);
    private final Setting<RotateMode> rotate = mode("Rotate", RotateMode.NORMAL);
    private final Setting<Boolean> grimAC = bool("GrimAC", false);
    private final Setting<Boolean> doubleBreak = bool("DoubleBreak", true);
    private final Setting<Boolean> breakAhead = bool("BreakAhead", false);
    private final Setting<Boolean> rebreak = bool("Rebreak", true);
    private final Setting<Boolean> swing = bool("Swing", true);
    private final Setting<Double> range = num("Range", 5.0, 1.0, 7.0);

    private final Setting<RenderMode> renderMode = mode("RenderMode", RenderMode.NATIVE);
    private final Setting<ColorSettingMode> colorMode = mode("ColorMode", ColorSettingMode.Global);
    private final Setting<Float> lineWidth = num("LineWidth", 2.0f, 0.5f, 5.0f);
    private final Setting<Color> lineColor = color("LineColor", 67, 107, 183, 150);
    private final Setting<Color> fillColor = color("FillColor", 67, 107, 183, 60);
    
    private enum RenderMode {
        NATIVE, CUSTOM
    }
    
    private enum RotateMode {
        NONE, NORMAL, HOLD
    }

    private BlockPos pos;
    private Direction direction;
    private int ticks;
    private double progress;
    private double previousProgress; // For smooth interpolation
    private boolean started;
    private boolean finished;

    private BlockPos secondaryPos;
    private int secondaryTicks;
    private boolean secondaryHolding;
    private double secondaryProgress;
    private double secondaryPreviousProgress; // For smooth interpolation

    private int stopCooldown;

    private long lastStopMs;
    private double delayBalance;

    private BlockPos rebreakHoldPos;
    private int rebreakHoldTicks;
    
    // Client-side instant rebreak simulation
    private boolean instantRebreak;
    private int brokenCount;
    private int lastBrokenCount;
    private long lastSimulateTime;

    public interface MineFinishListener { void onMineFinish(BlockPos pos); }

    private final CopyOnWriteArrayList<MineFinishListener> finishListeners = new CopyOnWriteArrayList<>();

    public void addFinishListener(MineFinishListener l) { finishListeners.addIfAbsent(l); }

    public void removeFinishListener(MineFinishListener l) { finishListeners.remove(l); }

    private void fireFinish(BlockPos target) {
        for (MineFinishListener l : finishListeners) l.onMineFinish(target);
    }

    public SpeedMineModule() {
        super("SpeedMine", "Packet mines the clicked block at best-tool speed behind a Grim decoy, then rebreaks it.", Category.WORLD);

        breakAhead.setVisibility(v -> doubleBreak.getValue());
        colorMode.setVisibility(v -> renderMode.getValue() == RenderMode.CUSTOM);
        lineWidth.setVisibility(v -> renderMode.getValue() == RenderMode.CUSTOM);
        lineColor.setVisibility(v -> renderMode.getValue() == RenderMode.CUSTOM && colorMode.getValue() == ColorSettingMode.Custom);
        fillColor.setVisibility(v -> renderMode.getValue() == RenderMode.CUSTOM && colorMode.getValue() == ColorSettingMode.Custom);
    }

    @Override
    public void onDisable() {
        if (!nullCheck() && pos != null && started && !finished) {
            abortBreak();
        }
        clearSecondary();
        clearMine();
        rebreakHoldPos = null;
        rebreakHoldTicks = 0;
    }

    @Override
    public String getDisplayInfo() {
        String extra = secondaryPos != null ? " +1" : "";
        if (pos == null) return secondaryPos != null ? "+1" : null;
        if (!started) return "wait" + extra;
        return (finished ? "rebreak" : (int) (Math.min(progress / threshold.getValue(), 1) * 100) + "%") + extra;
    }

    @Subscribe
    private void onAttackBlock(AttackBlockEvent event) {
        if (nullCheck()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;

        event.cancel();

        if (isMining(event.getPos())) return;
        if (!InteractionUtil.canBreak(event.getPos(), event.getState())) return;

        // start the block behind first so it gets demoted into the secondary slot and
        // finishes on its own while the clicked block runs as the primary break.
        BlockPos ahead = breakAheadPos(event.getPos());
        if (ahead != null) startMine(ahead, validFace(ahead));

        startMine(event.getPos(), event.getDirection());
    }

    /**
     * Traces the look ray through the clicked block and returns the block it continues into,
     * or null if it leaves through an edge / into something we can't mine. Straight-on mining
     * (any of the 8 compass directions) exits cleanly through a face, while grazing a corner
     * lands too close to the face border and is rejected so we don't waste the second slot.
     */
    private BlockPos breakAheadPos(BlockPos target) {
        if (!breakAhead.getValue() || !doubleBreak.getValue()) return null;
        // only when both slots are free, so we never disturb a break already in progress
        if (secondaryPos != null || (pos != null && started && !finished)) return null;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 dir = mc.player.getLookAngle();
        AABB box = new AABB(target);

        double tEnter = Double.NEGATIVE_INFINITY;
        double tExit = Double.POSITIVE_INFINITY;
        Direction exitFace = null;

        for (Direction.Axis axis : Direction.Axis.values()) {
            double d = axis.choose(dir.x, dir.y, dir.z);
            double o = axis.choose(eye.x, eye.y, eye.z);
            double min = axis.choose(box.minX, box.minY, box.minZ);
            double max = axis.choose(box.maxX, box.maxY, box.maxZ);
            if (Math.abs(d) < 1.0E-7) {
                if (o < min || o > max) return null;
                continue;
            }
            double t1 = (min - o) / d;
            double t2 = (max - o) / d;
            double near = Math.min(t1, t2);
            double far = Math.max(t1, t2);
            if (near > tEnter) tEnter = near;
            if (far < tExit) {
                tExit = far;
                exitFace = Direction.fromAxisAndDirection(axis,
                        d > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            }
        }
        if (exitFace == null || tEnter > tExit || tExit <= 0) return null;

        // reject exits that hug the border of the face, those are corner grazes
        Vec3 exit = eye.add(dir.scale(tExit));
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis == exitFace.getAxis()) continue;
            double p = axis.choose(exit.x, exit.y, exit.z) - axis.choose(target.getX(), target.getY(), target.getZ());
            if (p < BREAK_AHEAD_EDGE || p > 1 - BREAK_AHEAD_EDGE) return null;
        }

        BlockPos ahead = target.relative(exitFace);
        if (!inMineRange(ahead)) return null;
        BlockState state = mc.level.getBlockState(ahead);
        if (state.isAir() || !InteractionUtil.canBreak(ahead, state)) return null;
        return ahead;
    }

    private void startMine(BlockPos target, Direction dir) {
        if (pos != null && started && !finished) {
            if (!doubleBreak.getValue()) {
                abortBreak();
            } else if (secondaryPos != null) {
                Pagon.LOGGER.info("[SpeedMine] Cannot start new mine - secondary slot occupied");
                abortBreak();
            } else if (!demote()) {
                Pagon.LOGGER.info("[SpeedMine] Cannot demote current mine to secondary");
                abortBreak();
            } else {
                Pagon.LOGGER.info("[SpeedMine] Demoted {} to secondary, starting {}", pos, target);
            }
        }

        pos = target.immutable();
        direction = dir;
        ticks = 0;
        progress = 0;
        started = false;
        finished = false;

        if (stopCooldown == 0 && canBegin()) begin();
    }

    private boolean canBegin() {
        long delay = System.currentTimeMillis() - lastStopMs;
        if (delay >= 275) return true; // grim decays the balance instead
        double cost = (300 - delay) * (decoy.getValue() ? 2 : 1);
        return delayBalance + cost <= 900;
    }

    private void trackStarts(int starts) {
        long delay = System.currentTimeMillis() - lastStopMs;
        for (int i = 0; i < starts; i++) {
            if (delay >= 275) delayBalance *= 0.9;
            else delayBalance += 300 - delay;
        }
        delayBalance = Mth.clamp(delayBalance, -1000, 1000);
    }

    @Override
    public boolean isAvailable() {
        return isEnabled() && !nullCheck() && !mc.player.isCreative() && !mc.player.isSpectator();
    }

    @Override
    public boolean requestBreak(BlockPos target) {
        if (!isAvailable()) return false;
        if (isMining(target)) return true;
        if (!inMineRange(target)) return false;
        BlockState state = mc.level.getBlockState(target);
        if (state.isAir() || !InteractionUtil.canBreak(target, state)) return false;
        if (pos != null && !finished) {
            if (!started) return false;
            if (!doubleBreak.getValue() || secondaryPos != null) return false;
        }
        startMine(target, validFace(target));
        return true;
    }

    @Override
    public boolean isMining(BlockPos target) {
        return target.equals(pos) || target.equals(secondaryPos);
    }

    @Override
    public BlockPos getRebreakPos() {
        return finished && rebreak.getValue() ? pos : null;
    }

    @Override
    public boolean hasFreePrimary() {
        return pos == null || finished;
    }

    @Override
    public boolean hasFreeSecondary() {
        return doubleBreak.getValue() && secondaryPos == null;
    }

    @Override
    public boolean inMineRange(BlockPos target) {
        return mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(target)) <= range.getValue();
    }

    /** The primary mine target, finished or not. */
    public BlockPos getPrimaryPos() {
        return pos;
    }

    /** The secondary (double break) target, or null. */
    public BlockPos getSecondaryPos() {
        return secondaryPos;
    }

    /**
     * Suppresses the rebreak of {@code target} for {@code ticks} ticks, so a module can drop
     * something into the hole (AutoMine's GlassPush) before we take the block back out.
     */
    public void holdRebreak(BlockPos target, int ticks) {
        rebreakHoldPos = target != null ? target.immutable() : null;
        rebreakHoldTicks = target != null ? ticks : 0;
    }

    public void collectMiningPositions(Set<BlockPos> out, double minProgress) {
        if (pos != null && started && (finished || progress >= minProgress)) out.add(pos);
        if (secondaryPos != null && !nullCheck()) {
            BlockState state = mc.level.getBlockState(secondaryPos);
            if (!state.isAir()) {
                double delta = InteractionUtil.getBreakDelta(
                        mc.player.getInventory().getItem(bestSlot(state, secondaryPos)), state, secondaryPos);
                if (delta > 0 && secondaryTicks * delta >= minProgress) out.add(secondaryPos);
            }
        }
    }

    private boolean demote() {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return false;
        if (!stopBreak(bestSlot(state, pos), false)) {
            sendAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, validFace(pos));
        }
        secondaryPos = pos;
        secondaryTicks = ticks;
        secondaryHolding = false;
        secondaryProgress = Math.min(progress, 1);
        return true;
    }

    private void begin() {
        started = true;
        trackStarts(decoy.getValue() ? 2 : 1);
        if (faceMargin(direction, targetBox(pos)) <= 0) {
            direction = validFace(pos);
        }
        
        // Apply rotation if enabled
        if (rotate.getValue() != RotateMode.NONE) {
            applyRotation(pos);
        }
        
        sendAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction);
        
        if (decoy.getValue()) {
            BlockPos decoyPos = pos.below(DECOY_Y_OFFSET);
            sendAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, decoyPos, validFace(decoyPos));
        }
        
        if (swing.getValue()) {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (stopCooldown > 0) stopCooldown--;
        if (rebreakHoldTicks > 0) rebreakHoldTicks--;

        tickSecondary();

        if (pos == null) return;

        // HOLD rotation: Keep rotating to block while mining
        if (rotate.getValue() == RotateMode.HOLD && started && !finished) {
            applyRotation(pos);
        }

        if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > range.getValue()) {
            if (started && !finished) abortBreak();
            clearMine();
            return;
        }

        BlockState state = mc.level.getBlockState(pos);

        if (!started) {
            if (state.isAir() || !InteractionUtil.canBreak(pos, state)) {
                clearMine();
                return;
            }
            if (stopCooldown > 0 || !canBegin()) return;
            direction = validFace(pos);
            begin();
            return;
        }

        if (!finished) {
            if (state.isAir()) {
                clearMine();
                return;
            }

            int slot = bestSlot(state, pos);
            double delta = InteractionUtil.getBreakDelta(mc.player.getInventory().getItem(slot), state, pos);
            if (delta <= 0) {
                abortBreak();
                clearMine();
                return;
            }

            ticks++;
            previousProgress = progress;
            progress = Math.max(ticks - fudgeTicks.getValue(), 0) * delta;
            
            // Send swing packet every tick while mining (for non-Grim servers)
            if (swing.getValue()) {
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
            
            if (progress >= threshold.getValue()) {
                finished = stopBreak(slot);
                if (finished) fireFinish(pos);
            }
            return;
        }

        if (!rebreak.getValue()) {
            clearMine();
            return;
        }
        
        // Handle instant rebreak simulation
        if (state.isAir()) {
            if (!instantRebreak) {
                instantRebreak = true;
                progress = 1.0f;
                previousProgress = 1.0f;
                lastSimulateTime = System.currentTimeMillis();
            }
            return;
        }
        
        // Block respawned - instantly break it client-side with simulation
        if (instantRebreak && System.currentTimeMillis() - lastSimulateTime < 250) {
            // Client-side instant break simulation
            mc.level.destroyBlock(pos, false, mc.player, 512);
            lastSimulateTime = System.currentTimeMillis();
        }
        
        if (rebreakHoldTicks > 0 && pos.equals(rebreakHoldPos)) return;
        
        stopBreak(bestSlot(state, pos));
    }

    private void tickSecondary() {
        if (secondaryPos == null) return;

        BlockState state = mc.level.getBlockState(secondaryPos);
        if (state.isAir()) {
            fireFinish(secondaryPos);
            clearSecondary();
            return;
        }

        secondaryTicks++;
        int slot = bestSlot(state, secondaryPos);
        double delta = InteractionUtil.getBreakDelta(mc.player.getInventory().getItem(slot), state, secondaryPos);
        if (delta <= 0) {
            clearSecondary();
            return;
        }

        int expected = Mth.ceil(1.0 / delta) - 1;
        secondaryPreviousProgress = secondaryProgress;
        secondaryProgress = Math.min(secondaryTicks * delta, 1);

        if (secondaryTicks > expected + SECONDARY_TIMEOUT) {
            clearSecondary();
            return;
        }

        if (secondaryTicks >= expected - 1 && !secondaryHolding) {
            Result result = new Result(slot, mc.player.getInventory().getItem(slot), ResultType.HOTBAR);
            if (!result.holding() && !Pagon.swapManager.isLatched() && Pagon.swapManager.latch(result)) {
                secondaryHolding = true;
            }
        }
    }

    private void clearSecondary() {
        if (secondaryHolding) Pagon.swapManager.release();
        secondaryPos = null;
        secondaryTicks = 0;
        secondaryHolding = false;
        secondaryProgress = 0;
        secondaryPreviousProgress = 0;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (nullCheck()) return;
        
        float partialTicks = event.getDelta();
        
        // Primary block: mining or rebreak
        if (pos != null && started) {
            BlockState state = mc.level.getBlockState(pos);
            
            if (!finished) {
                // Mining in progress
                double currentProgress = Math.min(this.progress / threshold.getValue(), 1.0);
                double prevProgress = Math.min(this.previousProgress / threshold.getValue(), 1.0);
                double interpolatedProgress = prevProgress + (currentProgress - prevProgress) * partialTicks;
                drawBlock(pos, true, interpolatedProgress, partialTicks);
            } else if (rebreak.getValue() && state.isAir()) {
                // Rebreak mode: block is air, show rebreak indicator
                drawRebreakBlock(pos, partialTicks);
            }
        }
        
        // Secondary block (double break)
        if (secondaryPos != null) {
            double interpolatedProgress = secondaryPreviousProgress + (secondaryProgress - secondaryPreviousProgress) * partialTicks;
            drawBlock(secondaryPos, false, interpolatedProgress, partialTicks);
        }
    }

    private void drawRebreakBlock(BlockPos target, float partialTicks) {
        // Draw a full-size box where the air block is (rebreak indicator)
        AABB worldBox = new AABB(target);
        Vec3 center = worldBox.getCenter();
        
        double dx = 0.5;
        double dy = 0.5;
        double dz = 0.5;

        AABB box = new AABB(center, center).inflate(dx, dy, dz); // Full block size

        if (renderMode.getValue() == RenderMode.NATIVE) {
            // Native rebreak: Green color to indicate ready to rebreak
            int fillColor = ARGB.color(40, 0, 255, 0);
            int outlineColor = ARGB.color(150, 0, 255, 0);

            Gizmos.cuboid(box, GizmoStyle.fill(fillColor)).setAlwaysOnTop();
            Gizmos.cuboid(box, GizmoStyle.stroke(outlineColor, 2.0f)).setAlwaysOnTop();
        } else {
            // Custom rebreak: Use line/fill colors with reduced alpha
            Color lineCol, fillCol;
            
            if (colorMode.getValue() == ColorSettingMode.Global) {
                lineCol = Pagon.colorManager.getColor();
                fillCol = new Color(lineCol.getRed(), lineCol.getGreen(), lineCol.getBlue(), 40);
            } else if (colorMode.getValue() == ColorSettingMode.Rainbow) {
                int rainbow = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f + target.hashCode() * 0.01f, 0.8f, 1.0f);
                Color rainbowColor = new Color(rainbow);
                lineCol = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 150);
                fillCol = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 40);
            } else {
                lineCol = lineColor.getValue();
                fillCol = new Color(fillColor.getValue().getRed(), fillColor.getValue().getGreen(), 
                                   fillColor.getValue().getBlue(), 40);
            }

            int fill = ARGB.color(fillCol.getAlpha(), fillCol.getRed(), fillCol.getGreen(), fillCol.getBlue());
            int outline = ARGB.color(lineCol.getAlpha(), lineCol.getRed(), lineCol.getGreen(), lineCol.getBlue());

            Gizmos.cuboid(box, GizmoStyle.fill(fill)).setAlwaysOnTop();
            Gizmos.cuboid(box, GizmoStyle.stroke(outline, lineWidth.getValue())).setAlwaysOnTop();
        }
    }

    private void drawBlock(BlockPos target, boolean primary, double progress, float partialTicks) {
        if (mc.level.getBlockState(target).isAir()) return;

        VoxelShape shape = mc.level.getBlockState(target).getShape(mc.level, target);
        if (shape.isEmpty()) shape = Shapes.block();

        AABB bb = shape.bounds();
        AABB worldBox = new AABB(
            target.getX() + bb.minX, target.getY() + bb.minY, target.getZ() + bb.minZ,
            target.getX() + bb.maxX, target.getY() + bb.maxY, target.getZ() + bb.maxZ
        );

        Vec3 center = worldBox.getCenter();

        if (renderMode.getValue() == RenderMode.NATIVE) {
            // Native mode: Kırmızı→Yeşil gradient, smooth cubic easing
            // Cubic easing out: 1 - (1-t)^3 for smoother deceleration
            float t = (float) progress;
            t = 1.0f - (float) Math.pow(1.0f - t, 3.0); // Cubic ease-out
            float scale = Mth.clamp(t, 0.0f, 1.0f);
            
            double dx = (bb.maxX - bb.minX) / 2.0;
            double dy = (bb.maxY - bb.minY) / 2.0;
            double dz = (bb.maxZ - bb.minZ) / 2.0;

            AABB box = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);

            // Color transition: Red (0.0) → Yellow (0.5) → Green (1.0)
            int red = (int) (255 * (1.0f - t));
            int green = (int) (255 * t);
            
            int fillColor = ARGB.color(60, red, green, 0);
            int outlineColor = ARGB.color(200, red, green, 0);

            Gizmos.cuboid(box, GizmoStyle.fill(fillColor)).setAlwaysOnTop();
            Gizmos.cuboid(box, GizmoStyle.stroke(outlineColor, 2.0f)).setAlwaysOnTop();
        } else {
            // Custom mode: Global/Custom/Rainbow colors, smooth cubic easing
            float t = (float) progress;
            t = 1.0f - (float) Math.pow(1.0f - t, 3.0); // Cubic ease-out
            float scale = Mth.clamp(t, 0.0f, 1.0f);
            
            double dx = (bb.maxX - bb.minX) / 2.0;
            double dy = (bb.maxY - bb.minY) / 2.0;
            double dz = (bb.maxZ - bb.minZ) / 2.0;

            AABB box = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);

            Color lineCol, fillCol;
            
            if (colorMode.getValue() == ColorSettingMode.Global) {
                // Use global color from ColorModule
                lineCol = Pagon.colorManager.getColor();
                fillCol = new Color(lineCol.getRed(), lineCol.getGreen(), lineCol.getBlue(), 60);
            } else if (colorMode.getValue() == ColorSettingMode.Rainbow) {
                // Rainbow based on position and time
                int rainbow = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f + target.hashCode() * 0.01f, 0.8f, 1.0f);
                Color rainbowColor = new Color(rainbow);
                lineCol = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 200);
                fillCol = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 60);
            } else {
                // Custom colors
                lineCol = lineColor.getValue();
                fillCol = fillColor.getValue();
            }

            int fill = ARGB.color(fillCol.getAlpha(), fillCol.getRed(), fillCol.getGreen(), fillCol.getBlue());
            int outline = ARGB.color(lineCol.getAlpha(), lineCol.getRed(), lineCol.getGreen(), lineCol.getBlue());

            Gizmos.cuboid(box, GizmoStyle.fill(fill)).setAlwaysOnTop();
            Gizmos.cuboid(box, GizmoStyle.stroke(outline, lineWidth.getValue())).setAlwaysOnTop();
        }
    }

    private boolean stopBreak(int slot) {
        return stopBreak(slot, true);
    }

    private boolean stopBreak(int slot, boolean cooldown) {
        // A SILENT swap would break an active latch (SwordGap eating, or our own secondary hold) —
        // wait it out instead; the caller just retries next tick.
        if (Pagon.swapManager.isLatched() && (pauseOnEat.getValue() || secondaryHolding)) return false;

        ItemStack stack = mc.player.getInventory().getItem(slot);
        return Pagon.swapManager.withSwap(new Result(slot, stack, ResultType.HOTBAR), SwapMode.SILENT,
                SwapPriority.MINING, () -> {
            if (cooldown) stopCooldown = breakDelay.getValue();
            
            // Apply rotation before sending stop packet
            if (rotate.getValue() != RotateMode.NONE) {
                applyRotation(pos);
            }
            
            // GrimAC: Send extra STOP packet before actual stop
            if (grimAC.getValue()) {
                sendAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, validFace(pos));
            }
            
            sendAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, validFace(pos));
            if (swing.getValue()) {
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        });
    }

    private void abortBreak() {
        sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN);
    }

    private Direction validFace(BlockPos target) {
        AABB box = targetBox(target);

        Direction best = Direction.UP;
        double bestMargin = -Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            double margin = faceMargin(dir, box);
            if (margin > bestMargin) {
                bestMargin = margin;
                best = dir;
            }
        }
        return best;
    }

    private double faceMargin(Direction dir, AABB box) {
        Vec3 now = mc.player.position();
        Vec3 prev = new Vec3(mc.player.xo, mc.player.yo, mc.player.zo);
        return Math.min(feetMargin(dir, box, now), feetMargin(dir, box, prev));
    }

    private double feetMargin(Direction dir, AABB box, Vec3 feet) {
        return switch (dir) {
            case UP -> feet.y + GRIM_MAX_EYE - box.maxY;
            case DOWN -> box.minY - (feet.y + GRIM_MIN_EYE);
            case EAST -> feet.x - box.maxX;
            case WEST -> box.minX - feet.x;
            case SOUTH -> feet.z - box.maxZ;
            case NORTH -> box.minZ - feet.z;
        };
    }

    private AABB targetBox(BlockPos target) {
        VoxelShape shape = mc.level.getBlockState(target).getShape(mc.level, target);
        return shape.isEmpty() ? new AABB(target) : shape.bounds().move(target);
    }

    private int bestSlot(BlockState state, BlockPos target) {
        int best = mc.player.getInventory().getSelectedSlot();
        double bestDelta = InteractionUtil.getBreakDelta(mc.player.getInventory().getItem(best), state, target);
        for (int i = 0; i < 9; i++) {
            double delta = InteractionUtil.getBreakDelta(mc.player.getInventory().getItem(i), state, target);
            if (delta > bestDelta) {
                best = i;
                bestDelta = delta;
            }
        }
        return best;
    }

    private void sendAction(ServerboundPlayerActionPacket.Action action, BlockPos target, Direction face) {
        if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            lastStopMs = System.currentTimeMillis();
        }
        mc.getConnection().send(new ServerboundPlayerActionPacket(action, target, face));
    }

    private void clearMine() {
        pos = null;
        direction = null;
        ticks = 0;
        progress = 0;
        previousProgress = 0;
        started = false;
        finished = false;
        instantRebreak = false;
        lastSimulateTime = 0;
    }
    
    public static BlockPos getActiveRebreakPos() {
        return null; // Not used anymore
    }
    
    public static void markRebreakSimulated() {
        // Not used anymore
    }

    // Rotation helper methods
    private Vec3 getClosestPointToEye(Vec3 eyePos, AABB box) {
        return box.getCenter();
    }

    private Vec3 getClampClosestPoint(Vec3 eyePos, AABB box) {
        double x = Mth.clamp(eyePos.x, box.minX, box.maxX);
        double y = Mth.clamp(eyePos.y, box.minY, box.maxY);
        double z = Mth.clamp(eyePos.z, box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }

    private float getYRotToVec(Vec3 vec) {
        Vec3 eyePos = mc.player.getEyePosition();
        double dx = vec.x - eyePos.x;
        double dz = vec.z - eyePos.z;
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
    }

    private float getXRotToVec(Vec3 vec) {
        Vec3 eyePos = mc.player.getEyePosition();
        double dx = vec.x - eyePos.x;
        double dy = vec.y - eyePos.y;
        double dz = vec.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    private void applyRotation(BlockPos target) {
        if (rotate.getValue() == RotateMode.NONE) return;

        Vec3 eyePos = mc.player.getEyePosition();
        AABB blockBox = new AABB(target);
        Vec3 lookVec = getClosestPointToEye(eyePos, blockBox);

        float yaw = getYRotToVec(lookVec);
        float pitch = getXRotToVec(lookVec);

        // Use RotationManager for server-side only rotation (client view unaffected)
        Pagon.rotationManager.setPlayerRotations(yaw, pitch);
    }
}
