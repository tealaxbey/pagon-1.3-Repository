package org.pagon.event.impl.render;

import org.pagon.event.Event;
import net.minecraft.world.item.ItemStack;

public class RenderFloatingItemEvent extends Event {
    private final ItemStack floatingItem;

    public RenderFloatingItemEvent(ItemStack floatingItem) {
        this.floatingItem = floatingItem;
    }

    public ItemStack getFloatingItem() {
        return floatingItem;
    }
}
