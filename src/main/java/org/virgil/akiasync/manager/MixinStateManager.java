package org.virgil.akiasync.manager;

public final class MixinStateManager {
    
    private MixinStateManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static void resetAllMixinStates() {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("AkiAsync");
        logger.info("[AkiAsync-Debug] Starting global Mixin state reset...");
        
        long startTime = System.currentTimeMillis();
        
        resetVillagerBreedMixin();
        
        long resetTime = System.currentTimeMillis() - startTime;
        logger.info("[AkiAsync-Debug] All Mixin states reset successfully in " + resetTime + "ms");
    }
    
    private static void resetVillagerBreedMixin() {
        try {
            Class<?> villagerClass = net.minecraft.world.entity.npc.Villager.class;
            
            java.lang.reflect.Field[] fields = villagerClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    (field.getName().contains("initialized") || 
                     field.getName().contains("cached_enabled") ||
                     field.getName().contains("cached_ageThrottle") ||
                     field.getName().contains("cached_interval"))) {
                    
                    field.setAccessible(true);
                    if (field.getType() == boolean.class) {
                        field.set(null, false);
                    } else if (field.getType() == int.class) {
                        field.set(null, field.getName().contains("interval") ? 5 : 0);
                    }
                }
            }
            
            java.util.logging.Logger.getLogger("AkiAsync").info("[AkiAsync-Debug] VillagerBreedAsyncMixin state reset via reflection");
            
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("AkiAsync").warning("[AkiAsync-Debug] Failed to reset VillagerBreedAsyncMixin: " + e.getMessage());
        }
    }
    
}
