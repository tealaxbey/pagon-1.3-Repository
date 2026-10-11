package org.pagon.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    
    private static final Map<Integer, TeleportAnimation> animations = new ConcurrentHashMap<>();
    private static final Map<Integer, Vec3> lastPositions = new ConcurrentHashMap<>();
    private static final int ANIMATION_TICKS = 3;
    private static final Minecraft mc = Minecraft.getInstance();
    private static final double MIN_DISTANCE = 3.0;
    private static final double MAX_DISTANCE = 200.0;
    
    public static void checkTeleport(Entity entity) {
        if (entity == null) return;
        
        Vec3 currentPos = entity.position();
        Vec3 lastPos = lastPositions.get(entity.getId());
        
        if (lastPos != null) {
            double distance = lastPos.distanceTo(currentPos);
            if (distance >= MIN_DISTANCE && distance <= MAX_DISTANCE) {
                TeleportAnimation existingAnim = animations.get(entity.getId());
                if (existingAnim == null || existingAnim.isFinished()) {
                    TeleportAnimation anim = new TeleportAnimation(lastPos, currentPos, ANIMATION_TICKS);
                    animations.put(entity.getId(), anim);
                }
            }
        }
        
        lastPositions.put(entity.getId(), currentPos);
    }
    
    public static void onTeleport(Entity entity, Vec3 from, Vec3 to) {
        if (entity == null || from == null || to == null) return;
        
        double distance = from.distanceTo(to);
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) return;
        
        TeleportAnimation anim = new TeleportAnimation(from, to, ANIMATION_TICKS);
        animations.put(entity.getId(), anim);
        lastPositions.put(entity.getId(), to);
    }
    
    public static void tick() {
        animations.entrySet().removeIf(entry -> {
            TeleportAnimation anim = entry.getValue();
            if (anim.isFinished()) {
                return true;
            }
            anim.tick();
            return false;
        });
    }
    
    public static Vec3 getRenderPosition(Entity entity, float partialTicks) {
        TeleportAnimation anim = animations.get(entity.getId());
        if (anim == null || anim.isFinished()) {
            return null;
        }
        return anim.getRenderPosition(partialTicks);
    }
    
    public static boolean isAnimating(Entity entity) {
        TeleportAnimation anim = animations.get(entity.getId());
        return anim != null && !anim.isFinished();
    }
    
    public static void clear() {
        animations.clear();
        lastPositions.clear();
    }
    
    private static class TeleportAnimation {
        private final Vec3 startPos;
        private final Vec3 endPos;
        private final int duration;
        private int currentTick;
        
        public TeleportAnimation(Vec3 start, Vec3 end, int duration) {
            this.startPos = start;
            this.endPos = end;
            this.duration = duration;
            this.currentTick = 0;
        }
        
        public void tick() {
            if (!isFinished()) {
                currentTick++;
            }
        }
        
        public boolean isFinished() {
            return currentTick >= duration;
        }
        
        public Vec3 getRenderPosition(float partialTicks) {
            if (isFinished()) {
                return null;
            }
            
            float progress = (currentTick + partialTicks) / (float) duration;
            progress = Math.min(progress, 1.0f);
            
            double eased = easeOutCubic(progress);
            
            double x = lerp(startPos.x, endPos.x, eased);
            double y = lerp(startPos.y, endPos.y, eased);
            double z = lerp(startPos.z, endPos.z, eased);
            
            return new Vec3(x, y, z);
        }
        
        private double lerp(double start, double end, double t) {
            return start + (end - start) * t;
        }
        
        private double easeOutCubic(double t) {
            return 1.0 - Math.pow(1.0 - t, 3.0);
        }
    }
}
