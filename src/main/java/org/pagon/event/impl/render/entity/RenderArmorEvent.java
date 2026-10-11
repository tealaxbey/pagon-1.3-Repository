package org.pagon.event.impl.render.entity;

import org.pagon.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class RenderArmorEvent extends Event {
    private final LivingEntity entity;

    public RenderArmorEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}
