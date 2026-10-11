package org.pagon.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.impl.render.entity.RenderArmorEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;
import static org.pagon.util.traits.Util.mc;

@Mixin(HumanoidArmorLayer.class)
public class MixinHumanoidArmorLayer {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRenderArmorPiece(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                             net.minecraft.world.item.ItemStack itemStack,
                                             net.minecraft.world.entity.EquipmentSlot equipmentSlot, int light,
                                             HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState) || mc.level == null) {
            return;
        }
        if (mc.level.getEntity(avatarState.id) instanceof LivingEntity entity
                && EVENT_BUS.post(new RenderArmorEvent(entity))) {
            ci.cancel();
        }
    }
}
