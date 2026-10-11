package org.pagon.manager;

import org.pagon.event.impl.entity.DeathEvent;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.network.PacketEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiningManager implements Util {
    private final Map<BlockPos, MiningData> miningBlocks = new ConcurrentHashMap<>();

    public void init() {
        EVENT_BUS.register(this);
    }

    public void onUnload() {
        EVENT_BUS.unregister(this);
        miningBlocks.clear();
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) return;

        for (MiningData data : miningBlocks.values()) {
            if (mc.player.distanceToSqr(Vec3.atCenterOf(data.getBlockPos())) > 144.0f || data.tick() > 40) {
                miningBlocks.remove(data.getBlockPos());
            }
        }
    }

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (event.getEntity() == mc.player) {
            miningBlocks.clear();
        }
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) return;
        if (!(event.getPacket() instanceof ClientboundBlockDestructionPacket packet)) return;

        BlockPos pos = packet.getPos().immutable();
        if (packet.getProgress() < 0 || packet.getProgress() >= 10) {
            miningBlocks.remove(pos);
            return;
        }
        if (miningBlocks.containsKey(pos)) {
            MiningData data = miningBlocks.get(pos);
            data.setProgress(packet.getProgress() / 10.0f);
            if (data.getBlockState().isAir()) {
                data.setBlockState(mc.level.getBlockState(pos));
            }
            return;
        }

        Entity entity = mc.level.getEntity(packet.getId());
        if (!(entity instanceof Player player)) return;
        if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 144.0f) return;

        if (getMiningCount(player) >= 2) {
            for (MiningData data : miningBlocks.values()) {
                if (data.getPlayer().equals(player)) {
                    miningBlocks.remove(data.getBlockPos());
                    break;
                }
            }
        }

        MiningData data = new MiningData(pos, player, mc.level.getBlockState(pos));
        data.setProgress(packet.getProgress() / 10.0f);
        miningBlocks.put(pos, data);
    }

    public MiningData getData(BlockPos pos) {
        return miningBlocks.get(pos);
    }

    public int getMiningCount(Player player) {
        int count = 0;
        for (MiningData data : miningBlocks.values()) {
            if (data.getPlayer().equals(player) && ++count >= 2) {
                break;
            }
        }
        return count;
    }

    public Collection<MiningData> getMiningBlocks() {
        return miningBlocks.values();
    }

    public static class MiningData {
        private final BlockPos blockPos;
        private final Player player;
        private BlockState blockState;
        private int ticksMining;
        private float progress;

        private MiningData(BlockPos blockPos, Player player, BlockState blockState) {
            this.blockPos = blockPos;
            this.player = player;
            this.blockState = blockState;
        }

        private int tick() {
            return ++ticksMining;
        }

        public BlockPos getBlockPos() {
            return blockPos;
        }

        public Player getPlayer() {
            return player;
        }

        public float getProgress() {
            return progress;
        }

        public BlockState getBlockState() {
            return blockState;
        }

        private void setProgress(float progress) {
            this.progress = progress;
        }

        private void setBlockState(BlockState blockState) {
            this.blockState = blockState;
        }
    }
}


