package org.pagon.event.impl.world;

import org.pagon.event.Event;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

public class PlaySoundEvent extends Event {
    private final Vec3 pos;
    private final SoundEvent soundEvent;

    public PlaySoundEvent(Vec3 pos, SoundEvent soundEvent) {
        this.pos = pos;
        this.soundEvent = soundEvent;
    }

    public Vec3 getPos() {
        return pos;
    }

    public SoundEvent getSoundEvent() {
        return soundEvent;
    }
}
