package org.pagon.event.impl.render;

import org.pagon.event.Event;

public class RenderFogEvent extends Event {
    private final float viewDistance;
    private float start;
    private float end;

    public RenderFogEvent(float viewDistance) {
        this.viewDistance = viewDistance;
        this.start = 0.0f;
        this.end = viewDistance;
    }

    public float getViewDistance() {
        return viewDistance;
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }
}
