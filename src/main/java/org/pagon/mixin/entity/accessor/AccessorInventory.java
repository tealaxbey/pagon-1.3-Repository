package org.pagon.mixin.entity.accessor;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface AccessorInventory {
    
    @Accessor("selected")
    int getSelected();
    
    @Accessor("selected")
    void setSelected(int selected);
}
