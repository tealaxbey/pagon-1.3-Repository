package org.pagon.event.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.pagon.event.Event;
import org.joml.Matrix4f;

public class Render3DEvent extends Event {
    private final PoseStack matrix;
    private final Matrix4f projectionMatrix;
    private final float delta;

    public Render3DEvent(PoseStack matrix, Matrix4f projectionMatrix, float delta) {
        this.matrix = matrix;
        this.projectionMatrix = projectionMatrix;
        this.delta = delta;
    }

    public PoseStack getMatrix() {
        return matrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public float getDelta() {
        return delta;
    }
}


