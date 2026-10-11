package org.pagon.mixin.entity.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface AccessorLocalPlayer {
    @Invoker("itemUseSpeedMultiplier")
    float pagon$itemUseSpeedMultiplier();
}
