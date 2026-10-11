package org.pagon.event.impl.world;

import org.pagon.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class AttackBlockEvent extends Event {
    private final BlockPos pos;
    private final Direction direction;
    private final BlockState state;

    public AttackBlockEvent(BlockPos pos, Direction direction, BlockState state) {
        this.pos = pos;
        this.direction = direction;
        this.state = state;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getDirection() {
        return direction;
    }

    public BlockState getState() {
        return state;
    }
}


