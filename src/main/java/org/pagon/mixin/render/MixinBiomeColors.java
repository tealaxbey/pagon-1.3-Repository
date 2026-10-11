package org.pagon.mixin.render;

import org.pagon.event.impl.world.BiomeColorEvent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(BiomeColors.class)
public class MixinBiomeColors {
    @Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true)
    private static void hookGetAverageGrassColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Grass event = new BiomeColorEvent.Grass();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }

    @Inject(method = "getAverageFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void hookGetAverageFoliageColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Foliage event = new BiomeColorEvent.Foliage();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }

    @Inject(method = "getAverageDryFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void hookGetAverageDryFoliageColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Foliage event = new BiomeColorEvent.Foliage();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }

    @Inject(method = "getAverageWaterColor", at = @At("HEAD"), cancellable = true)
    private static void hookGetAverageWaterColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BiomeColorEvent.Water event = new BiomeColorEvent.Water();
        if (EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getColor());
        }
    }
}
