package org.pagon.mixin.client.accessor;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionInstance.class)
public interface AccessorOptionInstance {
    @Accessor("value")
    Object pagon$getRawValue();

    @Accessor("value")
    void pagon$setRawValue(Object value);
}
