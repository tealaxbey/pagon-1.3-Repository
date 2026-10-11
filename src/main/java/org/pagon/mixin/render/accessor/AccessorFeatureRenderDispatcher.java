package org.pagon.mixin.render.accessor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.resources.model.AtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface AccessorFeatureRenderDispatcher {
    @Accessor("blockRenderDispatcher")
    BlockRenderDispatcher pagon$getBlockRenderDispatcher();

    @Accessor("bufferSource")
    MultiBufferSource.BufferSource pagon$getBufferSource();

    @Accessor("atlasManager")
    AtlasManager pagon$getAtlasManager();

    @Accessor("outlineBufferSource")
    OutlineBufferSource pagon$getOutlineBufferSource();

    @Accessor("crumblingBufferSource")
    MultiBufferSource.BufferSource pagon$getCrumblingBufferSource();

    @Accessor("font")
    Font pagon$getFont();
}


