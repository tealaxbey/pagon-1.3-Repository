package org.pagon.mixin.render;

import org.pagon.impl.imixin.IEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements IEntityRenderState {
    @Unique
    private Entity pagon$entity;

    @Override
    public Entity pagon$getEntity() {
        return pagon$entity;
    }

    @Override
    public void pagon$setEntity(Entity entity) {
        pagon$entity = entity;
    }
}
