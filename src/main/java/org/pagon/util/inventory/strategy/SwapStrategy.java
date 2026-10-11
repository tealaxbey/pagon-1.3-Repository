package org.pagon.util.inventory.strategy;

import org.pagon.util.inventory.Result;

public interface SwapStrategy {
    boolean swap(Result result);

    boolean swapBack(int last, Result result);
}


