package org.pagon.util.render;

import org.pagon.util.traits.Util;

/**
 * Animation utility for smooth GUI transitions
 */
public class Animation implements Util {
    private float value;
    private float target;
    private final float speed;
    private final Easing easing;

    public Animation(float speed, Easing easing) {
        this.speed = speed;
        this.easing = easing;
        this.value = 0.0f;
        this.target = 0.0f;
    }

    public Animation(float speed) {
        this(speed, Easing.EASE_OUT_CUBIC);
    }

    public void update(float delta) {
        if (Math.abs(target - value) < 0.001f) {
            value = target;
            return;
        }
        
        float difference = target - value;
        float step = difference * speed;
        value += step;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public float getValue() {
        return easing.apply(value);
    }

    public float getRawValue() {
        return value;
    }

    public boolean isFinished() {
        return Math.abs(target - value) < 0.001f;
    }

    public void reset() {
        value = 0.0f;
        target = 0.0f;
    }

    public void setInstant(float value) {
        this.value = value;
        this.target = value;
    }

    /**
     * Easing functions for smooth animations
     */
    public enum Easing {
        LINEAR {
            @Override
            public float apply(float t) {
                return t;
            }
        },
        EASE_IN_QUAD {
            @Override
            public float apply(float t) {
                return t * t;
            }
        },
        EASE_OUT_QUAD {
            @Override
            public float apply(float t) {
                return t * (2.0f - t);
            }
        },
        EASE_IN_OUT_QUAD {
            @Override
            public float apply(float t) {
                return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
            }
        },
        EASE_IN_CUBIC {
            @Override
            public float apply(float t) {
                return t * t * t;
            }
        },
        EASE_OUT_CUBIC {
            @Override
            public float apply(float t) {
                float f = t - 1.0f;
                return f * f * f + 1.0f;
            }
        },
        EASE_IN_OUT_CUBIC {
            @Override
            public float apply(float t) {
                return t < 0.5f ? 4.0f * t * t * t : (t - 1.0f) * (2.0f * t - 2.0f) * (2.0f * t - 2.0f) + 1.0f;
            }
        },
        EASE_OUT_EXPO {
            @Override
            public float apply(float t) {
                return t == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
            }
        };

        public abstract float apply(float t);
    }
}
