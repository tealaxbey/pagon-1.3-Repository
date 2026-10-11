package org.pagon.event.impl.render;

import org.pagon.event.Event;
import net.minecraft.world.entity.Entity;

public class RenderEntityInWorldEvent extends Event {
    private final Entity entity;

    public RenderEntityInWorldEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
