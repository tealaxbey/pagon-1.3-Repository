package org.pagon.mixin.render.gui;

import org.pagon.event.impl.gui.hud.RenderOverlayEvent;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.event.impl.render.RenderNauseaEvent;
import org.pagon.util.font.Fonts;
import org.pagon.util.font.FormattingUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(Gui.class)
public class MixinGui {
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

    @Inject(method = "render", at = @At("RETURN"))
    public void render(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen()) return;

        Render2DEvent event = new Render2DEvent(context, tickCounter.getGameTimeDeltaPartialTick(true));
        EVENT_BUS.post(event);
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRenderPortalOverlay(GuiGraphics guiGraphics, float alpha, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderOverlayEvent.Portal(guiGraphics))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRenderConfusionOverlay(GuiGraphics guiGraphics, float distortionStrength, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderNauseaEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRenderSpyglassOverlay(GuiGraphics guiGraphics, float scale, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderOverlayEvent.Spyglass(guiGraphics))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void pagon$hookRenderTextureOverlay(GuiGraphics guiGraphics, Identifier texture, float alpha, CallbackInfo ci) {
        String path = texture.getPath();
        if (path.contains("pumpkin")) {
            if (EVENT_BUS.post(new RenderOverlayEvent.Pumpkin(guiGraphics))) {
                ci.cancel();
            }
        } else if (texture.equals(POWDER_SNOW_OUTLINE_LOCATION)) {
            if (EVENT_BUS.post(new RenderOverlayEvent.Frostbite(guiGraphics))) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "renderSelectedItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void pagon$hookRenderSelectedItemName(GuiGraphics graphics, Font font, Component text, int x, int y, int color, int backgroundColor) {
        if (!Fonts.isCustomEnabled()) {
            graphics.drawStringWithBackdrop(font, text, x, y, color, backgroundColor);
            return;
        }
        Fonts.CLIENT.drawStringWithShadow(graphics, FormattingUtil.toString(text), x, y, color);
    }

}


