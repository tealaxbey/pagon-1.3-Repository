package org.pagon.mixin.render.accessor;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface AccessorTextureBinding {
    @Accessor("location")
    Identifier pagon$getLocation();

    @Accessor("sampler")
    Supplier<GpuSampler> pagon$getSampler();
}


