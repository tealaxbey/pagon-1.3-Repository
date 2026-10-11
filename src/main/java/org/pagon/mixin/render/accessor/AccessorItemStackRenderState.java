package org.pagon.mixin.render.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStackRenderState.class)
public interface AccessorItemStackRenderState {
    @Accessor("displayContext")
    ItemDisplayContext pagon$getDisplayContext();

    @Invoker("clear")
    void pagon$clear();
}
