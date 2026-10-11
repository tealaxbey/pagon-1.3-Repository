package org.pagon.util.world;

import org.pagon.util.traits.Util;
import net.minecraft.world.level.Level;

public class DimensionUtil implements Util {
    public static int getDimension() {
        if (mc.level == null) {
            return 1;
        }
        if (mc.level.dimension() == Level.OVERWORLD) {
            return 1;
        }
        if (mc.level.dimension() == Level.NETHER) {
            return 0;
        }
        if (mc.level.dimension() == Level.END) {
            return -1;
        }
        return 1;
    }
}
