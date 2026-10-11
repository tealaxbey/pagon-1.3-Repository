package org.pagon.features.modules.combat;

import org.pagon.Pagon;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.InteractionUtil;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.ResultType;
import org.pagon.util.models.Timer;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ElytraBlockerModule extends Module {
    private static final int OFFHAND_SLOT = 40;
    private static final double TARGET_SCAN_RANGE = 48.0;
    private static final double PATH_SCAN_RANGE = 28.0;
    private static final int MAX_PREDICTION_TICKS = 40;
    private static final int MIN_LEAD_TICKS = 3;
    private static final int MAX_LEAD_TICKS = 16;
    private static final long ROTATE_HOLD_MS = 120L;
    private static final long PLACE_COOLDOWN_MS = 60L;
    private static final double RANGE_PADDING = 0.5;
    private static final double MAX_POINT_ERROR = 1.05;

    private final Setting<Float> range = num("Range", 4.0f, 1.0f, 6.0f);
    private final Setting<Boolean> multitask = bool("Multitask", true);
    private final Setting<Boolean> rotate = bool("Rotate", false);

    private final Timer rotateHoldTimer = new Timer();
    private final Timer placeCooldownTimer = new Timer();
    private Vec3 rotateTarget;
    private BlockPos lastPlacedPos;

    public ElytraBlockerModule() {
        super("ElytraBlocker", "Places blocks into predicted elytra flight paths", Category.COMBAT, true);
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(range.getValue().intValue());
    }

    @Override
    public void onTick() {
        if (nullCheck() || mc.player.isSpectator() || mc.gameMode == null) {
            return;
        }

        maintainRotation();

        if (!multitask.getValue() && mc.player.isUsingItem()) {
            return;
        }

        Result obsidian = InventoryUtil.find(Items.OBSIDIAN, InventoryUtil.FULL_SCOPE);
        if (!obsidian.found()) {
            return;
        }

        List<? extends Player> targets = mc.level.players().stream()
                .filter(this::shouldHandlePlayer)
                .sorted(Comparator.comparingDouble(player -> mc.player.distanceToSqr(player)))
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        boolean swappedToOffhand = false;
        if (obsidian.type() != ResultType.OFFHAND) {
            if (!InventoryUtil.cursor().isEmpty()) {
                return;
            }
            InventoryUtil.click(toMenuSlot(obsidian.slot()), OFFHAND_SLOT, ClickType.SWAP);
            swappedToOffhand = true;
        }

        try {
            PlacementTarget placement = null;
            for (Player target : targets) {
                placement = predictPlacement(target);
                if (placement != null) {
                    break;
                }
            }

            if (placement == null) {
                return;
            }

            if (lastPlacedPos != null
                    && lastPlacedPos.equals(placement.pos())
                    && !placeCooldownTimer.passedMs(PLACE_COOLDOWN_MS)) {
                return;
            }

            if (rotate.getValue()) {
                rotateToPlacement(placement.pos(), placement.airPlace());
            }

            if (InteractionUtil.place(placement.pos(), placement.airPlace(), InteractionHand.OFF_HAND)) {
                lastPlacedPos = placement.pos();
                placeCooldownTimer.reset();
                if (rotate.getValue()) {
                    rotateTarget = placementVec(placement.pos(), placement.airPlace());
                    rotateHoldTimer.reset();
                }
            }
        } finally {
            if (swappedToOffhand) {
                InventoryUtil.click(toMenuSlot(obsidian.slot()), OFFHAND_SLOT, ClickType.SWAP);
            }
        }
    }

    @Override
    public void onDisable() {
        rotateTarget = null;
        lastPlacedPos = null;
        rotateHoldTimer.reset();
        placeCooldownTimer.reset();
    }

    private boolean shouldHandlePlayer(Player player) {
        if (player == null || !player.isAlive() || !player.isFallFlying() || player == mc.player) {
            return false;
        }
        if (player.distanceTo(mc.player) > TARGET_SCAN_RANGE) {
            return false;
        }
        return Pagon.friendManager == null || !Pagon.friendManager.isFriend(player);
    }

    private PlacementTarget predictPlacement(Player player) {
        Vec3 motion = player.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            motion = player.getLookAngle().scale(0.15);
        }
        if (motion.lengthSqr() < 1.0E-6) {
            return null;
        }

        Vec3 origin = player.position();
        Vec3 current = origin;
        Vec3 velocity = motion;
        double maxRange = range.getValue() + RANGE_PADDING;
        double maxRangeSq = maxRange * maxRange;
        int leadTicks = getLeadTicks(player);
        boolean startedInsideRange = origin.distanceToSqr(mc.player.position()) <= maxRangeSq;
        List<Vec3> path = new ArrayList<>(MAX_PREDICTION_TICKS + 1);
        List<Vec3> velocities = new ArrayList<>(MAX_PREDICTION_TICKS + 1);
        double traveled = 0.0;
        path.add(current);
        velocities.add(velocity);

        for (int tick = 0; tick < MAX_PREDICTION_TICKS; tick++) {
            Vec3 next = current.add(velocity);
            if (mc.level.clip(new ClipContext(
                    current,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            )).getType() != HitResult.Type.MISS) {
                break;
            }

            traveled += current.distanceTo(next);
            if (traveled > PATH_SCAN_RANGE) {
                break;
            }

            current = next;
            path.add(current);
            velocities.add(velocity);

            if (startedInsideRange) {
                PlacementTarget target = findFarthestPlacementAlongPath(path, velocities, origin, maxRangeSq);
                if (target != null) {
                    return target;
                }
            } else {
                int placeIndex = Math.max(0, path.size() - 1 - leadTicks);
                Vec3 placePoint = path.get(placeIndex);
                if (placePoint.distanceToSqr(mc.player.position()) > maxRangeSq) {
                    continue;
                }

                Vec3 travelDirection = velocities.get(placeIndex).normalize();
                PlacementTarget target = findPlacementAround(placePoint, origin, travelDirection, maxRangeSq);
                if (target != null) {
                    return target;
                }
            }

            velocity = predictNextVelocity(player, velocity);
            if (velocity.lengthSqr() < 1.0E-6) {
                break;
            }
        }

        return null;
    }

    private PlacementTarget findFarthestPlacementAlongPath(List<Vec3> path, List<Vec3> velocities, Vec3 origin, double maxRangeSq) {
        for (int i = path.size() - 1; i >= 0; i--) {
            Vec3 placePoint = path.get(i);
            if (placePoint.distanceToSqr(mc.player.position()) > maxRangeSq) {
                continue;
            }
            Vec3 travelDirection = velocities.get(i).normalize();
            PlacementTarget target = findPlacementAround(placePoint, origin, travelDirection, maxRangeSq);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private PlacementTarget findPlacementAround(Vec3 point, Vec3 origin, Vec3 direction, double maxRangeSq) {
        BlockPos center = BlockPos.containing(point);
        PlacementTarget best = null;
        double bestScore = Double.MAX_VALUE;
        double pointErrorLimitSq = MAX_POINT_ERROR * MAX_POINT_ERROR;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    Vec3 centerVec = Vec3.atCenterOf(pos);
                    if (mc.player.distanceToSqr(centerVec) > maxRangeSq) {
                        continue;
                    }
                    if (!InteractionUtil.isPlaceable(pos, false)) {
                        continue;
                    }

                    double pointError = centerVec.distanceToSqr(point);
                    if (pointError > pointErrorLimitSq) {
                        continue;
                    }

                    double forward = Math.max(0.0, direction.dot(centerVec.subtract(origin)));
                    double lateralError = centerVec.subtract(origin.add(direction.scale(forward))).length();
                    double score = pointError + lateralError * 0.75;
                    if (InteractionUtil.calcSide(pos) == null) {
                        score += 0.1;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        best = new PlacementTarget(pos, InteractionUtil.calcSide(pos) == null);
                    }
                }
            }
        }

        return best;
    }

    private int getLeadTicks(Player player) {
        int ping = Pagon.serverManager == null ? 0 : Pagon.serverManager.getPing();
        float tpsFactor = Pagon.serverManager == null ? 1.0f : Pagon.serverManager.getTpsFactor();
        double speed = Math.max(0.15, mc.player.getDeltaMovement().length());
        int networkLead = (int) Math.ceil((ping / 50.0f) * Math.max(1.0f, tpsFactor));
        int speedLead = (int) Math.ceil(Mth.clamp(speed * 3.0, 2.0, 6.0));
        double distance = player.distanceTo(mc.player);
        int distanceLead = (int) Math.ceil(Mth.clamp(distance / 3.5, 1.0, 9.0));
        int lead = networkLead + speedLead + distanceLead;
        return Mth.clamp(lead, MIN_LEAD_TICKS, MAX_LEAD_TICKS);
    }

    private Vec3 predictNextVelocity(Player player, Vec3 velocity) {
        Vec3 look = player.getLookAngle();
        Vec3 adjusted = velocity.add(look.scale(0.08));
        adjusted = adjusted.scale(0.99).subtract(0.0, 0.03, 0.0);
        if (adjusted.lengthSqr() < 1.0E-6) {
            adjusted = look.scale(0.15);
        }
        return adjusted;
    }

    private void maintainRotation() {
        if (!rotate.getValue() || rotateTarget == null) {
            return;
        }
        if (rotateHoldTimer.passedMs(ROTATE_HOLD_MS)) {
            rotateTarget = null;
            return;
        }
        Pagon.rotationManager.updateRotations();
        Pagon.rotationManager.lookAtVec3d(rotateTarget);
    }

    private void rotateToPlacement(BlockPos pos, boolean airPlace) {
        Pagon.rotationManager.updateRotations();
        Pagon.rotationManager.lookAtVec3d(placementVec(pos, airPlace));
    }

    private Vec3 placementVec(BlockPos pos, boolean airPlace) {
        if (airPlace) {
            return Vec3.atCenterOf(pos);
        }
        Direction direction = InteractionUtil.calcSide(pos);
        if (direction == null) {
            return Vec3.atCenterOf(pos);
        }
        BlockPos neighbor = pos.relative(direction.getOpposite());
        return Vec3.atCenterOf(neighbor).add(
                direction.getStepX() * 0.5,
                direction.getStepY() * 0.5,
                direction.getStepZ() * 0.5
        );
    }

    private int toMenuSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    private record PlacementTarget(BlockPos pos, boolean airPlace) {
    }
}
