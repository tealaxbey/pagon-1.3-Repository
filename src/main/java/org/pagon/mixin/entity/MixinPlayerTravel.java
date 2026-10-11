package org.pagon.mixin.entity;

import org.pagon.event.impl.entity.player.PlayerMoveEvent;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;
import static org.pagon.util.traits.Util.mc;

@Mixin(Player.class)
public class MixinPlayerTravel {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void pagon$hookTravel(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }

        // GrimThrow module removed - travel hook disabled
    }
}


