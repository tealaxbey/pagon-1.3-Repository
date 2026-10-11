package org.pagon.mixin.world;

import org.pagon.Pagon;
import org.pagon.event.impl.world.AttackBlockEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;
import static org.pagon.util.traits.Util.mc;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (mc.level == null) return;
        AttackBlockEvent event = new AttackBlockEvent(pos, direction, mc.level.getBlockState(pos));
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(false);
        }
    }
}
