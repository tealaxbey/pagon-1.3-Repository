package org.pagon.event.impl.world;

import org.pagon.event.Event;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public class SkyboxEvent extends Event {
    private Color color;

    public Color getColor() {
        return color;
    }

    public Vec3 getColorVec() {
        return new Vec3(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0);
    }

    public int getRGB() {
        return color.getRGB();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public static class Sky extends SkyboxEvent {
    }

    public static class Cloud extends SkyboxEvent {
    }

    public static class Fog extends SkyboxEvent {
        private final float tickDelta;

        public Fog(float tickDelta) {
            this.tickDelta = tickDelta;
        }

        public float getTickDelta() {
            return tickDelta;
        }
    }
}
