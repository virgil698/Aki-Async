package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;

public final class VirtualEntityCheck {
    
    public static boolean is(Entity entity) {
        if (entity == null) return false;
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            return bridge != null && bridge.isVirtualEntity(entity);
        } catch (Throwable t) {
            return false;
        }
    }
    
    public static boolean is(EntityAccess entity) {
        if (entity == null) return false;
        try {
            if (entity instanceof Entity realEntity) {
                return is(realEntity);
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }
    
    public static boolean isAny(Entity entity1, Entity entity2) {
        return is(entity1) || is(entity2);
    }
    
    private VirtualEntityCheck() {
        throw new UnsupportedOperationException("Utility class");
    }
}
