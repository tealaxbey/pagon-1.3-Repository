package org.pagon.event.impl.render;

import org.pagon.event.Event;

import java.awt.Color;

public class AmbientColorEvent extends Event {
    private Color color;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
