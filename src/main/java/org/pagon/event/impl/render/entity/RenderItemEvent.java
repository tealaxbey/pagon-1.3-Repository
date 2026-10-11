package org.pagon.event.impl.render.entity;

import org.pagon.event.Event;
import net.minecraft.world.entity.item.ItemEntity;

public class RenderItemEvent extends Event {
    private final ItemEntity itemEntity;

    public RenderItemEvent(ItemEntity itemEntity) {
        this.itemEntity = itemEntity;
    }

    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
