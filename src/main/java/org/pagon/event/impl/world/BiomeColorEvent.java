package org.pagon.event.impl.world;

import org.pagon.event.Event;

import java.awt.Color;

public class BiomeColorEvent extends Event {
    private int color;

    public int getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color.getRGB();
    }

    public void setColor(int color) {
        this.color = color;
    }

    public static class Grass extends BiomeColorEvent {
    }

    public static class Foliage extends BiomeColorEvent {
    }

    public static class Water extends BiomeColorEvent {
    }

    public static class Lava extends BiomeColorEvent {
    }
}
