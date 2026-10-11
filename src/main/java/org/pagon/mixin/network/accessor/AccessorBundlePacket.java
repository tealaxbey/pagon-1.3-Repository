package org.pagon.mixin.network.accessor;

import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BundlePacket.class)
public interface AccessorBundlePacket {
    @Accessor("packets")
    @Mutable
    void setPackets(Iterable<Packet<?>> packets);
}


