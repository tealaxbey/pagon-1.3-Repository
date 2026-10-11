package org.pagon.event.impl.entity.player;

import org.pagon.event.Event;
import net.minecraft.world.entity.Entity;

public class PushEntityEvent extends Event {
    private final Entity pushed;
    private final Entity pusher;

    public PushEntityEvent(Entity pushed, Entity pusher) {
        this.pushed = pushed;
        this.pusher = pusher;
    }

    public Entity getPushed() {
        return pushed;
    }

    public Entity getPusher() {
        return pusher;
    }
}


