package org.pagon.features.modules.combat;

import org.pagon.Pagon;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.features.settings.DisplayNameProvider;
import org.pagon.util.InteractionUtil;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.ResultType;
import org.pagon.util.models.Timer;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public final class PearlBlockerModule extends Module {
    private static final int OFFHAND_SLOT = 40;
    private static final double PREDICTION_SCAN_RANGE = 102.0;
    private static final double ACTIVE_RANGE_PADDING = 0.5;
    private static final int MIN_LEAD_TICKS = 2;
    private static final int MAX_LEAD_TICKS = 12;
    private static final long ROTATE_HOLD_MS = 120L;

    private final Setting<SwapMode> swapMode = mode("SwapMode", SwapMode.AIRPLACE);
    private final Setting<Float> range = num("Range", 4.0f, 1.0f, 6.0f);
    private final Setting<Boolean> multitask = bool("Multitask", true);
    private final Setting<Boolean> rotate = bool("Rotate", false);

    private final Timer rotateHoldTimer = new Timer();
    private Vec3 rotateTarget;

    public PearlBlockerModule() {
        super("PearlBlocker", "Places obsidian into hostile ender pearls", Category.COMBAT);
    }

    @Override
    public String getDisplayInfo() {
        return swapMode.currentEnumName();
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

        List<ThrownEnderpearl> pearls = mc.level.getEntitiesOfClass(
                ThrownEnderpearl.class,
                mc.player.getBoundingBox().inflate(PREDICTION_SCAN_RANGE),
                this::shouldHandlePearl
        );
        if (pearls.isEmpty()) {
            return;
        }

        pearls.sort(Comparator.comparingDouble(pearl -> mc.player.distanceToSqr(pearl)));

        if (swapMode.getValue() == SwapMode.AIRPLACE) {
            tickAirPlace(pearls);
        } else {
            tickNormal(pearls);
        }
    }

    private void tickAirPlace(List<ThrownEnderpearl> pearls) {
        Result obsidian = InventoryUtil.find(Items.OBSIDIAN, InventoryUtil.FULL_SCOPE);
        if (!obsidian.found()) {
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
            for (ThrownEnderpearl pearl : pearls) {
                PlacementTarget target = predictPlacement(pearl);
                if (target == null) {
                    continue;
                }
                boolean airPlace = InteractionUtil.calcSide(target.pos()) == null;
                if (rotate.getValue()) {
                    rotateToPlacement(target.pos(), airPlace);
                }
                InteractionUtil.place(target.pos(), airPlace, InteractionHand.OFF_HAND);
                if (rotate.getValue()) {
                    rotateTarget = placementVec(target.pos(), airPlace);
                    rotateHoldTimer.reset();
                }
            }
        } finally {
            if (swappedToOffhand) {
                InventoryUtil.click(toMenuSlot(obsidian.slot()), OFFHAND_SLOT, ClickType.SWAP);
            }
        }
    }

    private void tickNormal(List<ThrownEnderpearl> pearls) {
        Result obsidian = InventoryUtil.find(Items.OBSIDIAN, InventoryUtil.HOTBAR_SCOPE);
        if (!obsidian.found() || obsidian.type() != ResultType.HOTBAR) {
            return;
        }

        int previousSlot = InventoryUtil.selected();
        InventoryUtil.swap(obsidian.slot());

        try {
            for (ThrownEnderpearl pearl : pearls) {
                PlacementTarget target = predictPlacement(pearl);
                if (target == null) {
                    continue;
                }

                if (rotate.getValue()) {
                    rotateToPlacement(target.pos(), false);
                }

                if (placeNormal(target.pos())) {
                    if (rotate.getValue()) {
                        rotateTarget = placementVec(target.pos(), false);
                        rotateHoldTimer.reset();
                    }
                }
            }
        } finally {
            InventoryUtil.swap(previousSlot);
        }
    }

    @Override
    public void onDisable() {
        rotateTarget = null;
        rotateHoldTimer.reset();
    }

    private boolean shouldHandlePearl(ThrownEnderpearl pearl) {
        if (pearl == null || !pearl.isAlive()) {
            return false;
        }
        Entity owner = pearl.getOwner();
        if (owner == mc.player) {
            return false;
        }
        if (owner instanceof Player player
                && Pagon.friendManager != null
                && Pagon.friendManager.isFriend(player)) {
            return false;
        }
        return pearl.distanceTo(mc.player) <= PREDICTION_SCAN_RANGE;
    }

    private PlacementTarget predictPlacement(ThrownEnderpearl pearl) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        Vec3 position = pearl.position();
        Vec3 motion = pearl.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            return null;
        }

        int leadTicks = getLeadTicks();
        double dangerRange = range.getValue() + ACTIVE_RANGE_PADDING;
        double dangerRangeSq = dangerRange * dangerRange;
        boolean startedInsideRange = position.distanceToSqr(mc.player.position()) <= dangerRangeSq;
        double traveled = 0.0;
        List<Vec3> path = new ArrayList<>();
        path.add(position);

        for (int tick = 0; tick < 120 && traveled <= PREDICTION_SCAN_RANGE; tick++) {
            Vec3 next = position.add(motion);
            path.add(next);
            traveled += position.distanceTo(next);

            if (next.distanceToSqr(mc.player.position()) <= dangerRangeSq) {
                if (startedInsideRange) {
                    Vec3 placeVec = path.get(path.size() - 1);
                    BlockPos pos = BlockPos.containing(placeVec.x, placeVec.y, placeVec.z);
                    return new PlacementTarget(pos, tick);
                }
                int placeIndex = Math.max(0, path.size() - 1 - leadTicks);
                Vec3 placeVec = path.get(placeIndex);
                BlockPos pos = BlockPos.containing(placeVec.x, placeVec.y, placeVec.z);
                return new PlacementTarget(pos, tick);
            }

            if (mc.level.clip(new ClipContext(
                    position,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    pearl
            )).getType() != HitResult.Type.MISS) {
                break;
            }

            position = next;
            motion = motion.scale(0.99).subtract(0.0, 0.03, 0.0);
            if (motion.lengthSqr() < 1.0E-5) {
                break;
            }
        }

        return null;
    }

    private int getLeadTicks() {
        int ping = Pagon.serverManager == null ? 0 : Pagon.serverManager.getPing();
        float tpsFactor = Pagon.serverManager == null ? 1.0f : Pagon.serverManager.getTpsFactor();
        int lead = (int) Math.ceil((ping / 50.0f) * Math.max(1.0f, tpsFactor)) + 1;
        return Mth.clamp(lead, MIN_LEAD_TICKS, MAX_LEAD_TICKS);
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

    private boolean placeNormal(BlockPos pos) {
        if (InteractionUtil.place(pos, false, InteractionHand.MAIN_HAND)) {
            return true;
        }

        List<BlockPos> helpers = findHelperChain(pos);
        if (helpers.isEmpty()) {
            return false;
        }

        for (BlockPos helper : helpers) {
            if (rotate.getValue()) {
                rotateToPlacement(helper, false);
            }
            if (!InteractionUtil.place(helper, false, InteractionHand.MAIN_HAND)) {
                return false;
            }
        }

        if (rotate.getValue()) {
            rotateToPlacement(pos, false);
        }
        return InteractionUtil.place(pos, false, InteractionHand.MAIN_HAND);
    }

    private List<BlockPos> findHelperChain(BlockPos target) {
        for (Direction firstDirection : Direction.values()) {
            BlockPos firstHelper = target.relative(firstDirection);
            if (canPlaceWithSupport(firstHelper, null) && hasSupport(firstHelper, null)) {
                return List.of(firstHelper);
            }

            for (Direction secondDirection : Direction.values()) {
                BlockPos secondHelper = firstHelper.relative(secondDirection);
                if (secondHelper.equals(target)) {
                    continue;
                }
                if (!canPlaceWithSupport(secondHelper, null) || !hasSupport(secondHelper, null)) {
                    continue;
                }
                if (hasSupport(firstHelper, secondHelper)) {
                    return List.of(secondHelper, firstHelper);
                }
            }
        }

        return List.of();
    }

    private boolean canPlaceWithSupport(BlockPos pos, BlockPos extraSupport) {
        if (!InteractionUtil.isPlaceable(pos, false)) {
            return false;
        }
        return hasSupport(pos, extraSupport);
    }

    private boolean hasSupport(BlockPos pos, BlockPos extraSupport) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (neighbor.equals(extraSupport) || !mc.level.getBlockState(neighbor).canBeReplaced()) {
                return true;
            }
        }
        return false;
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

    private record PlacementTarget(BlockPos pos, int ticksAhead) {
    }

    public enum SwapMode implements DisplayNameProvider {
        AIRPLACE("AirPlace"),
        NORMAL("Normal");
        //режим Normal не работает если что

        private final String displayName;

        SwapMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
}
