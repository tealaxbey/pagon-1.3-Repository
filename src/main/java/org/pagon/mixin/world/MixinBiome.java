package org.pagon.mixin.world;

import org.pagon.event.impl.world.BiomeColorEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(Biome.class)
public class MixinBiome {
    @Shadow
    @Final
    private BiomeSpecialEffects specialEffects;

    @Inject(method = "getWaterColor", at = @At("HEAD"), cancellable = true)
    private void hookGetWaterColor(CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Water event = new BiomeColorEvent.Water();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }

    @Inject(method = "getGrassColor", at = @At("HEAD"), cancellable = true)
    private void hookGetGrassColor(double x, double z, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Grass event = new BiomeColorEvent.Grass();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(specialEffects.grassColorModifier().modifyColor(x, z, event.getColor()));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("HEAD"), cancellable = true)
    private void hookGetFoliageColor(CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Foliage event = new BiomeColorEvent.Foliage();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }

    @Inject(method = "getDryFoliageColor", at = @At("HEAD"), cancellable = true)
    private void hookGetDryFoliageColor(CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Foliage event = new BiomeColorEvent.Foliage();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }
}
