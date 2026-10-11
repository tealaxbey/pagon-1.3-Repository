package org.pagon.mixin.render;

import org.pagon.event.impl.world.SkyboxEvent;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(CloudRenderer.class)
public class MixinCloudRenderer {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 hookCloudColor(Vec3 color) {
        SkyboxEvent.Cloud event = new SkyboxEvent.Cloud();
        if (EVENT_BUS.post(event)) {
            return event.getColorVec();
        }
        return color;
    }
}
