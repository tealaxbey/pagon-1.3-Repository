package org.pagon.util.inventory.strategy;

import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.ResultType;

/**
 * Latch strategy that visibly switches to a slot and holds it.
 */
public final class LatchStrategy implements SwapStrategy {
    public static final LatchStrategy INSTANCE = new LatchStrategy();

    private LatchStrategy() {}

    @Override
    public boolean swap(Result result) {
        if (result.type() == ResultType.OFFHAND) return true;
        if (result.type() != ResultType.HOTBAR) return false;
        if (result.holding()) return true;
        
        InventoryUtil.swap(result.slot());
        return true;
    }

    @Override
    public boolean swapBack(int last, Result result) {
        if (result.type() == ResultType.OFFHAND) return true;
        if (result.type() != ResultType.HOTBAR) return false;
        
        InventoryUtil.swap(last);
        return true;
    }
}
