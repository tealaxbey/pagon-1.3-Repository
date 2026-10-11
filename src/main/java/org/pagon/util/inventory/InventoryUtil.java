package org.pagon.util.inventory;

import org.pagon.util.inventory.strategy.HoldingStrategy;
import org.pagon.util.inventory.strategy.HotbarStrategy;
import org.pagon.util.inventory.strategy.InventoryStrategy;
import org.pagon.util.inventory.strategy.SwapStrategy;
import org.pagon.util.traits.Util;
import org.pagon.Pagon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class InventoryUtil implements Util {
    static final Result NONE = new Result(-1, ItemStack.EMPTY, ResultType.NONE);

    public static final int OFFHAND_SWAP_BUTTON = 40;

    public static final EnumSet<ResultType> HOTBAR_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.HOTBAR);
    public static final EnumSet<ResultType> INVENTORY_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.INVENTORY);
    public static final EnumSet<ResultType> FULL_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.HOTBAR, ResultType.INVENTORY);
    public static final EnumSet<ResultType> PLACE_SCOPE = EnumSet.of(ResultType.HOTBAR, ResultType.INVENTORY);

    private static final List<SwapStrategy> STRATEGIES = List.of(
            HoldingStrategy.INSTANCE,
            HotbarStrategy.INSTANCE,
            InventoryStrategy.INSTANCE
    );

    private InventoryUtil() {
        throw new AssertionError();
    }

    public static ItemStack cursor() {
        return mc.player.containerMenu.getCarried();
    }

    public static int selected() {
        return ((org.pagon.mixin.entity.accessor.AccessorInventory) mc.player.getInventory()).getSelected();
    }

    public static int fireworkRefireTicks(ItemStack stack) {
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        int flight = fireworks != null ? fireworks.flightDuration() : 1;
        return 10 * (1 + flight) - 1;
    }

    public static void click(int slot, int button, ClickType type) {
        int id = mc.player.containerMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(id, slot, button, type, mc.player);
    }

    public static void withSwap(Result result, Runnable action) {
        withSwap(result, r -> action.run());
    }

    public static void withSwap(Result result, Consumer<Result> action) {
        int lastSlot = selected();
        if (InventoryUtil.swap(result)) {
            action.accept(result);
            InventoryUtil.swapBack(result, lastSlot);
        }
    }

    public static void swap(int to) {
        if (Pagon.phasePause) return;
        if (to < 0 || to > 8) return;
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(to));
        } else {
            ((org.pagon.mixin.entity.accessor.AccessorInventory) mc.player.getInventory()).setSelected(to);
            mc.gameMode.ensureHasSentCarriedItem();
        }
    }

    public static boolean swap(Result result) {
        if (Pagon.phasePause) return false;
        for (SwapStrategy strategy : STRATEGIES) {
            if (strategy.swap(result))
                return true;
        }
        return false;
    }

    public static boolean swapBack(Result result, int last) {
        if (Pagon.phasePause) return false;
        for (SwapStrategy strategy : STRATEGIES) {
            if (strategy.swapBack(last, result))
                return true;
        }
        return false;
    }

    public static void swapToOffhand(int inventorySlot) {
        int containerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        click(containerSlot, OFFHAND_SWAP_BUTTON, ClickType.SWAP);
    }

    public static void swapToHotbarSlot(int inventorySlot, int hotbarSlot) {
        int containerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        click(containerSlot, hotbarSlot, ClickType.SWAP);
    }

    public static Result find(Item target, EnumSet<ResultType> scopes) {
        return find(stack -> stack.is(target), scopes);
    }

    public static Result find(Predicate<ItemStack> predicate, EnumSet<ResultType> scopes) {
        return find((item, scope) -> scopes.contains(scope) && predicate.test(item));
    }

    public static Result find(BiPredicate<ItemStack, ResultType> predicate) {
        ItemStack offhand = mc.player.getOffhandItem();
        if (predicate.test(offhand, ResultType.OFFHAND)) {
            return Result.fromOffhand(offhand);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack item = mc.player.getInventory().getItem(i);
            ResultType type = i < 9 ? ResultType.HOTBAR : ResultType.INVENTORY;
            if (predicate.test(item, type)) {
                return new Result(i, item, type);
            }
        }

        return NONE;
    }
    
    // Additional methods from Homovore's InventoryUtil
    
    public static int getPacketSlotIndex(AbstractContainerMenu handler, int slot) {
        if (handler instanceof net.minecraft.world.inventory.InventoryMenu) {
            if (slot == 40) {
                return 45;
            } else if (slot > 100) {
                return 108 - slot;
            } else {
                return slot < 9 ? slot + 36 : slot;
            }
        } else {
            List<Slot> slots = handler.slots;
            for (int id = 0; id < slots.size(); id++) {
                Slot s = slots.get(id);
                if (s.getContainerSlot() == slot) {
                    return id;
                }
            }
            return slot;
        }
    }
    
    public static int convertToPacketSlot(AbstractContainerMenu handler, int slot) {
        return getPacketSlotIndex(handler, slot);
    }
}



