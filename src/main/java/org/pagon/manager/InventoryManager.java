package org.pagon.manager;

import org.pagon.Pagon;
import org.pagon.event.impl.network.PacketEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.Feature;
import org.pagon.mixin.entity.accessor.AccessorInventory;
import org.pagon.util.inventory.SilentSwapType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public class InventoryManager extends Feature {
    
    private final Deque<SwapFrame> swapStack = new ArrayDeque<>();
    private int serverSlot = -1;
    
    public void init() {
        EVENT_BUS.register(this);
    }
    
    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
            int packetSlot = packet.getSlot();
            if (serverSlot == packetSlot) {
                event.cancel();
                return;
            }
            serverSlot = packetSlot;
        }
    }
    
    public boolean isSilentSwapping() {
        return mc.player != null && ((AccessorInventory) mc.player.getInventory()).getSelected() != serverSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (mc.player == null) return;
        
        if (slot != ((AccessorInventory) mc.player.getInventory()).getSelected()) {
            ((AccessorInventory) mc.player.getInventory()).setSelected(slot);
        }
        
        if (slot != serverSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }
    
    public boolean startSwap(int itemSlot, SilentSwapType swapType) {
        if (mc.player == null) return false;
        
        Inventory playerInventory = mc.player.getInventory();
        
        if (swapType == SilentSwapType.HOTBAR && !Inventory.isHotbarSlot(itemSlot)) {
            return false;
        }
        
        SwapFrame top = swapStack.peek();
        int activeSlot = top == null ? ((AccessorInventory) playerInventory).getSelected() : top.slotTo;
        
        if (activeSlot == itemSlot) {
            swapStack.push(new SwapFrame(activeSlot, activeSlot, swapType));
            return true;
        }
        
        int fromSlot = activeSlot;
        swapStack.push(new SwapFrame(fromSlot, itemSlot, swapType));
        
        switch (swapType) {
            case HOTBAR -> mc.getConnection().send(new ServerboundSetCarriedItemPacket(itemSlot));
            case INVENTORY -> {
                // CLIENT-SIDE swap first
                ((AccessorInventory) playerInventory).setSelected(itemSlot);
                // Then send packet
                internalSwapSlot(itemSlot, fromSlot);
            }
        }
        
        return true;
    }
    
    public void endSwap(SilentSwapType swapType) {
        if (swapStack.isEmpty() || mc.player == null) return;
        
        SwapFrame frame = swapStack.pop();
        if (frame.slotFrom == frame.slotTo) return;
        
        Inventory playerInventory = mc.player.getInventory();
        
        switch (frame.swapType) {
            case HOTBAR -> {
                if (isSilentSwapping()) {
                    SwapFrame outer = swapStack.peek();
                    int returnSlot = outer != null ? outer.slotTo : ((AccessorInventory) playerInventory).getSelected();
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(returnSlot));
                }
            }
            case INVENTORY -> {
                // CLIENT-SIDE swap back first
                ((AccessorInventory) playerInventory).setSelected(frame.slotFrom);
                // Then send packet
                internalSwapSlot(frame.slotTo, frame.slotFrom);
            }
        }
    }
    
    public void pickupSlot(AbstractContainerMenu handler, int slot) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(handler.containerId, slot, 0, ClickType.PICKUP, mc.player);
    }
    
    public void swapSlot(AbstractContainerMenu handler, int slot1, int slot2) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(handler.containerId, slot1, slot2, ClickType.SWAP, mc.player);
    }
    
    private void internalSwapSlot(int slot1, int slot2) {
        if (mc.player == null) return;
        
        AbstractContainerMenu screenHandler = mc.player.inventoryMenu;
        ItemStack stack1 = screenHandler.slots.get(slot1).getItem();
        ItemStack stack2 = screenHandler.slots.get(slot2).getItem();
        
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        changedSlots.put(slot1, stack2.copy());
        changedSlots.put(slot2, stack1.copy());
        
        // Send swap packet (this is a simplification - real implementation would need proper packet)
        swapSlot(screenHandler, slot1, slot2);
    }
    
    public boolean isHolding(Item item, InteractionHand hand) {
        if (mc.player == null) return false;
        
        ItemStack holdingStack = isSilentSwapping() && hand == InteractionHand.MAIN_HAND 
                ? getServerStack() 
                : mc.player.getItemInHand(hand);
        
        return holdingStack.is(item);
    }
    
    public ItemStack getServerStack() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getItem(serverSlot);
    }
    
    public int getServerSlot() {
        return serverSlot;
    }
    
    private record SwapFrame(int slotFrom, int slotTo, SilentSwapType swapType) {}
}
