package org.pagon.util.world;

import org.pagon.util.EnchantmentUtil;
import org.pagon.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.BiFunction;

public final class ExplosionUtil implements Util {
    private ExplosionUtil() {
        throw new AssertionError();
    }

    public static double getDamageTo(LivingEntity entity, Vec3 explosion, boolean ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
        return getDamageTo(entity, explosion, ignoreTerrain, Set.of(), extrapolationTicks, assumeBestArmor);
    }

    public static double getDamageTo(LivingEntity entity, Vec3 explosion, boolean ignoreTerrain, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
        Vec3 movement = Vec3.ZERO;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        if (extrapolationTicks != 0) {
            double ox = (x - entity.xOld) * extrapolationTicks;
            double oy = (y - entity.yOld) * extrapolationTicks * 0.3;
            double oz = (z - entity.zOld) * extrapolationTicks;
            x += ox;
            y += oy;
            z += oz;
            movement = new Vec3(ox, oy, oz);
        }

        Vec3 position = new Vec3(x, y, z);
        double distance = Math.sqrt(position.distanceToSqr(explosion));
        if (distance > 12.0) return 0.0;

        double exposure = getExposure(explosion, entity.getBoundingBox().move(movement), ignoreTerrain ? IgnoreTerrain.BLAST : IgnoreTerrain.NONE, ignoreBlocks);
        double scaledDistance = distance / 12.0;
        double impact = (1.0 - scaledDistance) * exposure;
        double damage = (float) ((int) ((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0));
        return Math.max(0.0, getReduction(entity, damage, assumeBestArmor));
    }

    private static double getReduction(LivingEntity entity, double damage, boolean assumeBestArmor) {
        DamageSource source = mc.level.damageSources().explosion(null);
        if (source.scalesWithDifficulty()) {
            switch (mc.level.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2.0 + 1.0, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        damage = CombatRules.getDamageAfterAbsorb(
                entity,
                (float) damage,
                source,
                (float) Math.floor(entity.getAttributeValue(Attributes.ARMOR)),
                (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        );
        damage = getResistanceReduction(entity, damage);
        damage = CombatRules.getDamageAfterMagicAbsorb((float) damage, getProtectionAmount(entity, assumeBestArmor));
        return Math.max(damage, 0.0);
    }

    private static double getResistanceReduction(LivingEntity entity, double damage) {
        var resistance = entity.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            int level = resistance.getAmplifier() + 1;
            damage *= 1.0f - level * 0.2f;
        }
        return Math.max(damage, 0.0);
    }

    private static float getProtectionAmount(LivingEntity entity, boolean assumeBestArmor) {
        int amount = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (assumeBestArmor && !stack.isEmpty()) {
                amount += slot == EquipmentSlot.LEGS ? 8 : 4;
            } else {
                amount += EnchantmentUtil.getLevel(Enchantments.BLAST_PROTECTION, stack) * 2;
                amount += EnchantmentUtil.getLevel(Enchantments.PROTECTION, stack);
            }
        }
        return amount;
    }

    private static float getExposure(Vec3 source, AABB box, IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1.0 / (xDiff * 2.0 + 1.0);
        double yStep = 1.0 / (yDiff * 2.0 + 1.0);
        double zStep = 1.0 / (zDiff * 2.0 + 1.0);
        if (xStep <= 0.0 || yStep <= 0.0 || zStep <= 0.0) return 0.0f;

        int misses = 0;
        int hits = 0;
        double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
        double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
        xStep *= xDiff;
        yStep *= yDiff;
        zStep *= zDiff;

        for (double x = box.minX + xOffset; x <= box.maxX + xOffset; x += xStep) {
            for (double y = box.minY; y <= box.maxY; y += yStep) {
                for (double z = box.minZ + zOffset; z <= box.maxZ + zOffset; z += zStep) {
                    if (raycast(new Vec3(x, y, z), source, ignoreTerrain, ignoreBlocks) == null) misses++;
                    hits++;
                }
            }
        }

        return (float) misses / hits;
    }

    private static BlockHitResult raycast(Vec3 start, Vec3 end, IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
        RaycastContext context = new RaycastContext(start, end, ignoreTerrain, ignoreBlocks);
        return BlockGetter.traverseBlocks(start, end, context, RAYCAST_FACTORY, ctx -> null);
    }

    private static final BiFunction<RaycastContext, BlockPos, BlockHitResult> RAYCAST_FACTORY = (context, pos) -> {
        if (context.ignoreBlocks().contains(pos)) return null;
        BlockState state = mc.level.getBlockState(pos);
        if (context.ignoreTerrain() == IgnoreTerrain.ALL) return null;
        if (context.ignoreTerrain() == IgnoreTerrain.BLAST && state.getBlock().getExplosionResistance() < 600.0f) return null;
        return state.getCollisionShape(mc.level, pos).clip(context.start(), context.end(), pos);
    };

    private record RaycastContext(Vec3 start, Vec3 end, IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
    }

    public enum IgnoreTerrain {
        ALL,
        BLAST,
        NONE
    }
}


