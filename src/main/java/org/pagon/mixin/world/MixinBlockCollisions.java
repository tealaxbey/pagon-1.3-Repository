package org.pagon.mixin.world;

import org.pagon.event.impl.world.BlockCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.pagon.util.traits.Util.EVENT_BUS;
import static org.pagon.util.traits.Util.mc;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisions {
    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hookGetCollisionShape(CollisionContext context, BlockState state, CollisionGetter level, BlockPos pos) {
        VoxelShape voxelShape = context.getCollisionShape(state, level, pos);
        if (level != mc.level) {
            return voxelShape;
        }

        BlockCollisionEvent event = new BlockCollisionEvent(voxelShape, pos, state);
        if (EVENT_BUS.post(event)) {
            return event.getVoxelShape();
        }
        return voxelShape;
    }
}


