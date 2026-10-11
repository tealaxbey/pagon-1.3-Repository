package org.pagon.event.impl.render;

import org.pagon.event.Event;

public class LightmapGammaEvent extends Event {
    private float gamma;

    public LightmapGammaEvent(float gamma) {
        this.gamma = gamma;
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }
}
