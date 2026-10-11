package org.pagon.event.impl.world;

import org.pagon.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCollisionEvent extends Event {
    private final BlockPos pos;
    private final BlockState state;
    private VoxelShape voxelShape;

    public BlockCollisionEvent(VoxelShape voxelShape, BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
        this.voxelShape = voxelShape;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    public Block getBlock() {
        return state.getBlock();
    }

    public VoxelShape getVoxelShape() {
        return voxelShape;
    }

    public void setVoxelShape(VoxelShape voxelShape) {
        this.voxelShape = voxelShape;
    }
}


