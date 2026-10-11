package org.pagon.features.modules.movement;

import org.pagon.Pagon;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.network.PacketEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.inventory.SilentSwapType;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
* by ItsMahir
*
*/

public class FakeFlyModule extends Module {
    
    private static final int CHEST_SLOT_ID = 6;
    private static FakeFlyModule INSTANCE;
    
    private final Setting<Float> fireworkDelay = num("FireworkDelay", 3.0f, 0.2f, 10.0f);
    private final Setting<Double> collisionOffset = num("CollisionOffset", 0.08, 0.0, 0.3);
    
    private long grimFireworkUsedAtMs;
    private int lastElytraSlot = -1;
    
    private Vec3 lastSafePosition;
    private boolean hadCollisionLastTick = false;
    private boolean isProcessingPacket = false;
    
    public FakeFlyModule() {
        super("FakeFly", "Fake elytra flight without wearing elytra - uses silent swap for fireworks", Category.MOVEMENT);
        INSTANCE = this;
    }
    
    public static boolean shouldOverridePose() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
    
    @Override
    public void onDisable() {
        unequipElytra();
        resetGrimControl();
        lastSafePosition = null;
        hadCollisionLastTick = false;
        isProcessingPacket = false;
    }
    
    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        
        if (isProcessingPacket) return;
        
        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
            boolean hasCollision = mc.player.horizontalCollision || mc.player.verticalCollisionBelow;
            
            if (hasCollision && mc.player.isFallFlying()) {
                double offsetY = collisionOffset.getValue();
                
                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float playerYaw = mc.player.getYRot();
                float playerPitch = mc.player.getXRot();
                boolean onGround = packet.isOnGround();
                boolean horizontalCol = packet.horizontalCollision();
                
                ServerboundMovePlayerPacket modifiedPacket;
                
                if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
                    modifiedPacket = new ServerboundMovePlayerPacket.PosRot(
                        packet.getX(playerX),
                        packet.getY(playerY) + offsetY,
                        packet.getZ(playerZ),
                        packet.getYRot(playerYaw),
                        packet.getXRot(playerPitch),
                        onGround,
                        horizontalCol
                    );
                } else if (packet instanceof ServerboundMovePlayerPacket.Pos) {
                    modifiedPacket = new ServerboundMovePlayerPacket.Pos(
                        packet.getX(playerX),
                        packet.getY(playerY) + offsetY,
                        packet.getZ(playerZ),
                        onGround,
                        horizontalCol
                    );
                } else {
                    return;
                }
                
                isProcessingPacket = true;
                event.cancel();
                mc.getConnection().send(modifiedPacket);
                isProcessingPacket = false;
                
                hadCollisionLastTick = true;
            } else {
                hadCollisionLastTick = false;
            }
        }
    }
    
    @Subscribe
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        
        if (!mc.player.horizontalCollision && !mc.player.verticalCollisionBelow && mc.player.isFallFlying()) {
            lastSafePosition = mc.player.position();
        }
        
        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        } else if (!mc.player.isFallFlying()) {
            tryStartGliding();
        }
        
        tickGrimControl();
    }
    
    private void tryStartGliding() {
        if (mc.player.isPassenger() || mc.player.isFallFlying()) {
            return;
        }
        
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            int elytraSlot = getInventorySlot(Items.ELYTRA);
            if (elytraSlot == -1) {
                return;
            }
            
            AbstractContainerMenu handler = mc.player.containerMenu;
            int elytraPacketSlot = getPacketSlotIndex(handler, elytraSlot);
            
            Pagon.inventoryManager.pickupSlot(handler, CHEST_SLOT_ID);
            Pagon.inventoryManager.pickupSlot(handler, elytraPacketSlot);
            Pagon.inventoryManager.pickupSlot(handler, CHEST_SLOT_ID);
            
            lastElytraSlot = elytraSlot;
        }
        
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        
        mc.player.startFallFlying();

        tryUseGrimFirework();
    }
    
    private void unequipElytra() {
        if (mc.player == null || !mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        
        int targetSlot = lastElytraSlot != -1 ? lastElytraSlot : firstFreeInventorySlot();
        if (targetSlot == -1) {
            return;
        }
        
        AbstractContainerMenu handler = mc.player.containerMenu;
        
        Pagon.inventoryManager.pickupSlot(handler, CHEST_SLOT_ID);
        Pagon.inventoryManager.pickupSlot(handler, getPacketSlotIndex(handler, targetSlot));
        
        lastElytraSlot = -1;
    }
    
    private int firstFreeInventorySlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
    
    private void resetGrimControl() {
        grimFireworkUsedAtMs = 0L;
        lastSafePosition = null;
        hadCollisionLastTick = false;
        isProcessingPacket = false;
    }
    
    private void tickGrimControl() {
        
        tryUseGrimFirework();
    }
    
    private void tryUseGrimFirework() {
        if (!mc.player.isFallFlying()) {
            return;
        }
        
        double elapsedSeconds = (System.currentTimeMillis() - grimFireworkUsedAtMs) / 1000.0;
        double delaySeconds = fireworkDelay.getValue();
        
        if (grimFireworkUsedAtMs != 0L && elapsedSeconds < delaySeconds) {
            return;
        }
        
        int fireworkSlot = getItemSlot(Items.FIREWORK_ROCKET, SilentSwapType.HOTBAR);
        
        if (fireworkSlot == -1) return;
        
        int oldSlot = org.pagon.util.inventory.InventoryUtil.selected();
        
        // Swap to firework slot
        org.pagon.util.inventory.InventoryUtil.swap(fireworkSlot);
        
        // Use item
        InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        
        // Swap back
        org.pagon.util.inventory.InventoryUtil.swap(oldSlot);
        
        if (result == InteractionResult.PASS) {
            return;
        }
        
        grimFireworkUsedAtMs = System.currentTimeMillis();
    }
    
    private int getInventorySlot(net.minecraft.world.item.Item item) {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                return i;
            }
        }
        return -1;
    }
    
    private int getItemSlot(net.minecraft.world.item.Item item, SilentSwapType type) {
        if (type == SilentSwapType.INVENTORY) {
            return getInventorySlot(item);
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(item)) {
                    return i;
                }
            }
            return -1;
        }
    }
    
    private int getPacketSlotIndex(AbstractContainerMenu handler, int slot) {
        if (handler instanceof net.minecraft.world.inventory.InventoryMenu) {
            if (slot == 40) {
                return 45;
            } else if (slot > 100) {
                return 108 - slot;
            } else {
                return slot < 9 ? slot + 36 : slot;
            }
        } else {
            return slot;
        }
    }
}
