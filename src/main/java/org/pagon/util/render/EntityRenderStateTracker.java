package org.pagon.util.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class EntityRenderStateTracker {
    private static final Map<EntityRenderState, Entity> RENDER_ENTITIES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private EntityRenderStateTracker() {
    }

    public static void put(EntityRenderState state, Entity entity) {
        RENDER_ENTITIES.put(state, entity);
    }

    public static Entity get(EntityRenderState state) {
        return RENDER_ENTITIES.get(state);
    }

    public static void remove(EntityRenderState state) {
        RENDER_ENTITIES.remove(state);
    }
}
