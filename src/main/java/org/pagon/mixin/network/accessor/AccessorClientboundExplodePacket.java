package org.pagon.mixin.network.accessor;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(ClientboundExplodePacket.class)
public interface AccessorClientboundExplodePacket {
    @Accessor("playerKnockback")
    @Mutable
    void setPlayerKnockback(Optional<Vec3> playerKnockback);
}


