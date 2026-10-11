package org.pagon.mixin.render.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface AccessorRenderSetup {
    @Accessor("pipeline")
    RenderPipeline pagon$getPipeline();

    @Accessor("textures")
    Map<String, Object> pagon$getTextures();

    @Accessor("textureTransform")
    TextureTransform pagon$getTextureTransform();

    @Accessor("useLightmap")
    boolean pagon$usesLightmap();

    @Accessor("useOverlay")
    boolean pagon$usesOverlay();

    @Accessor("affectsCrumbling")
    boolean pagon$affectsCrumbling();

    @Accessor("sortOnUpload")
    boolean pagon$sortOnUpload();

    @Accessor("bufferSize")
    int pagon$getBufferSize();

    @Accessor("layeringTransform")
    LayeringTransform pagon$getLayeringTransform();

    @Accessor("outputTarget")
    OutputTarget pagon$getOutputTarget();

    @Accessor("outlineProperty")
    RenderSetup.OutlineProperty pagon$getOutlineProperty();
}


