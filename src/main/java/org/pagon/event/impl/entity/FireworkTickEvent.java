package org.pagon.event.impl.entity;

import org.pagon.event.Event;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;

public class FireworkTickEvent extends Event {
    private final FireworkRocketEntity fireworkRocketEntity;

    public FireworkTickEvent(FireworkRocketEntity fireworkRocketEntity) {
        this.fireworkRocketEntity = fireworkRocketEntity;
    }

    public FireworkRocketEntity getFireworkRocketEntity() {
        return fireworkRocketEntity;
    }
}
