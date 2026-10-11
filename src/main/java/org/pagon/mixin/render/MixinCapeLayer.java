package org.pagon.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.Pagon;
import org.pagon.features.modules.client.CapeModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.mc;

@Mixin(CapeLayer.class)
public class MixinCapeLayer {
    
    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD")
    )
    private void pagon$enableLocalCape(PoseStack poseStack, SubmitNodeCollector submitter, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (shouldRenderLocalCape(state)) {
            state.showCape = true;
        }
    }

    @Redirect(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/PlayerSkin;cape()Lnet/minecraft/core/ClientAsset$Texture;")
    )
    private ClientAsset.Texture pagon$replaceCapeTexture(PlayerSkin skin, PoseStack poseStack, SubmitNodeCollector submitter, int light, AvatarRenderState state, float yRot, float xRot) {
        if (!shouldRenderLocalCape(state)) {
            return skin.cape();
        }

        CapeModule module = Pagon.moduleManager.getModuleByClass(CapeModule.class);
        Identifier texturePath = module != null ? module.getCapeTexture() : null;

        if (texturePath == null) {
            return skin.cape();
        }

        return new CapeTexture(texturePath);
    }

    private boolean shouldRenderLocalCape(AvatarRenderState state) {
        if (Pagon.moduleManager == null || mc.player == null || state == null) {
            return false;
        }

        CapeModule module = Pagon.moduleManager.getModuleByClass(CapeModule.class);
        return module != null && module.isEnabled() && state.id == mc.player.getId();
    }

    private record CapeTexture(Identifier texturePath) implements ClientAsset.Texture {
        @Override
        public Identifier id() {
            return texturePath;
        }
    }
}
