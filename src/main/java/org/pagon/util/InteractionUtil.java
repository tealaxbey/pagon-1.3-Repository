package org.pagon.util;

import org.pagon.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class InteractionUtil implements Util {
    public static boolean canBreak(BlockPos blockPos, BlockState state) {
        if (!mc.player.isCreative() && state.getDestroySpeed(mc.level, blockPos) < 0) return false;
        return state.getShape(mc.level, blockPos) != Shapes.empty();
    }

    public static boolean isPlaceable(BlockPos pos, boolean entityCheck) {
        if (!mc.level.getBlockState(pos).canBeReplaced()) return false;
        for (Entity e : mc.level.getEntitiesOfClass(Entity.class, new AABB(pos), e -> !(e instanceof ThrownExperienceBottle || e instanceof ItemEntity || e instanceof ExperienceOrb))) {
            if (e instanceof Player) return false;
            return !entityCheck;
        }
        return true;
    }

    public static boolean breakBlock(BlockPos pos) {
        if (!canBreak(pos, mc.level.getBlockState(pos))) return false;
        BlockPos bp = pos instanceof BlockPos.MutableBlockPos ? new BlockPos(pos) : pos;

        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, bp, Direction.UP));
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.UP));

        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        return true;
    }

    public static void useItem(BlockPos pos) {
        useItem(pos, InteractionHand.MAIN_HAND);
    }

    public static void useItem(BlockPos pos, InteractionHand hand) {
        if (mc.level == null || mc.player == null || mc.gameMode == null) return;
        Direction direction = resolveHitDirection(Direction.DOWN);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(
                Vec3.atCenterOf(pos), direction, pos, false
        ));
        if (result instanceof InteractionResult.Success success && success.swingSource() != InteractionResult.SwingSource.NONE) {
            mc.player.connection.send(new ServerboundSwingPacket(hand));
        }
    }

    public static boolean place(BlockPos pos, boolean airPlace) {
        return place(pos, airPlace, InteractionHand.MAIN_HAND);
    }

    public static boolean place(BlockPos pos, boolean airPlace, InteractionHand hand) {
        if (mc.level == null || mc.player == null || mc.gameMode == null) return false;
        if (!isPlaceable(pos, false)) return false;
        Direction direction = calcSide(pos);
        if (direction == null) {
            if (airPlace)
                direction = resolveHitDirection(Direction.DOWN);
            else return false;
        }
        BlockPos bp = airPlace ? pos : pos.relative(direction);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(
                airPlace ? Vec3.atCenterOf(pos) : Vec3.atCenterOf(bp).relative(direction.getOpposite(), 0.5),
                airPlace ? direction : direction.getOpposite(), bp, false
        ));
        if (result instanceof InteractionResult.Success success && success.swingSource() != InteractionResult.SwingSource.NONE) {
            mc.player.connection.send(new ServerboundSwingPacket(hand));
        }
        return true;
    }

    public static Direction calcSide(BlockPos pos) {
        for (Direction d : Direction.values())
            if (!mc.level.getBlockState(pos.offset(d.getUnitVec3i())).canBeReplaced()) return d;
        return null;
    }

    private static Direction resolveHitDirection(Direction fallback) {
        if (mc.hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getDirection();
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            return fallback;
        }
        return fallback;
    }

    public static double getBlockBreakingSpeed(int slot, BlockPos pos) {
        return getBlockBreakingSpeed(slot, mc.level.getBlockState(pos));
    }

    public static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().getNonEquipmentItems().get(slot).getDestroySpeed(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getItem(slot);

            int efficiency = EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, tool);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            speed *= 1 + (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isEyeInFluid(FluidTags.WATER) && EnchantmentUtil.has(Enchantments.AQUA_AFFINITY, EquipmentSlot.HEAD)) {
            speed /= 5.0F;
        }

        if (!mc.player.onGround()) {
            speed /= 5.0F;
        }

        float hardness = block.getDestroySpeed(null, null);
        if (hardness == -1) return 0;

        speed /= hardness / (!block.requiresCorrectToolForDrops() || mc.player.getInventory().getNonEquipmentItems().get(slot).isCorrectToolForDrops(block) ? 30 : 100);

        float ticks = (float) (Math.floor(1.0f / speed) + 1.0f);

        return (long) ((ticks / 20.0f) * 1000);
    }

    public static Direction right(Direction direction) {
        return switch (direction) {
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    public static Direction left(Direction direction) {
        return switch (direction) {
            case EAST -> Direction.NORTH;
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    /**
     * Calculates the break progress per tick for the given tool and block.
     * Returns a value between 0 and 1, where 1/delta is the number of ticks needed to break.
     */
    public static double getBreakDelta(ItemStack tool, BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(mc.level, pos);
        if (hardness == -1) return 0;
        
        float speedMultiplier = tool.getDestroySpeed(state);
        
        if (speedMultiplier > 1.0f) {
            int efficiency = EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, tool);
            if (efficiency > 0 && !tool.isEmpty()) {
                speedMultiplier += efficiency * efficiency + 1;
            }
        }
        
        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            speedMultiplier *= 1.0f + (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2f;
        }
        
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float multiplier = switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            };
            speedMultiplier *= multiplier;
        }
        
        if (mc.player.isEyeInFluid(FluidTags.WATER) && !EnchantmentUtil.has(Enchantments.AQUA_AFFINITY, EquipmentSlot.HEAD)) {
            speedMultiplier /= 5.0f;
        }
        
        if (!mc.player.onGround()) {
            speedMultiplier /= 5.0f;
        }
        
        float damage = speedMultiplier / hardness;
        boolean canHarvest = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        if (canHarvest) {
            damage /= 30.0f;
        } else {
            damage /= 100.0f;
        }
        
        return damage;
    }
}


