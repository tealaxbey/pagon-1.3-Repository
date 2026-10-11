package org.pagon.util.inventory.strategy;

import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import org.pagon.util.inventory.ResultType;
import net.minecraft.world.inventory.ClickType;

/**
 * Alternative silent swap strategy that uses window clicks to move items
 * without changing the selected slot. Can reach the full inventory.
 */
public final class AltSilentStrategy implements SwapStrategy {
    public static final AltSilentStrategy INSTANCE = new AltSilentStrategy();

    private AltSilentStrategy() {}

    @Override
    public boolean swap(Result result) {
        if (result.type() == ResultType.OFFHAND) return true;
        if (result.holding()) return true;
        
        int selected = InventoryUtil.selected();
        int targetSlot = convertToWindowSlot(result.slot(), result.type());
        int selectedSlot = convertToWindowSlot(selected, ResultType.HOTBAR);
        
        // Pick up target item
        InventoryUtil.click(targetSlot, 0, ClickType.PICKUP);
        // Place it in selected slot
        InventoryUtil.click(selectedSlot, 0, ClickType.PICKUP);
        // Put original item back
        InventoryUtil.click(targetSlot, 0, ClickType.PICKUP);
        
        return true;
    }

    @Override
    public boolean swapBack(int last, Result result) {
        if (result.type() == ResultType.OFFHAND) return true;
        
        int selected = InventoryUtil.selected();
        int targetSlot = convertToWindowSlot(result.slot(), result.type());
        int selectedSlot = convertToWindowSlot(selected, ResultType.HOTBAR);
        
        // Reverse the swap
        InventoryUtil.click(selectedSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(targetSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(selectedSlot, 0, ClickType.PICKUP);
        
        return true;
    }

    /**
     * Converts inventory slot index to window slot index for click operations.
     * Hotbar: 0-8 -> 36-44
     * Inventory: 9-35 -> 9-35
     */
    private int convertToWindowSlot(int slot, ResultType type) {
        if (type == ResultType.HOTBAR) {
            return slot + 36;
        }
        return slot;
    }
}
