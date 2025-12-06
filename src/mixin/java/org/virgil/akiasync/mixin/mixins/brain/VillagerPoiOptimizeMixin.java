package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.poi.BatchPoiManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.npc.Villager;

@Mixin(value = Villager.class, priority = 1100)
public class VillagerPoiOptimizeMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int cached_poiSearchInterval = 20;
    
    @Unique
    private long aki$lastPoiSearchTime = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void preloadPoiCache(CallbackInfo ci) {
        if (!initialized) {
            aki$initPoiOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        Villager villager = (Villager) (Object) this;
        ServerLevel level = (ServerLevel) villager.level();
        long currentTime = level.getGameTime();
        
        if (currentTime - aki$lastPoiSearchTime < cached_poiSearchInterval) {
            return;
        }
        
        aki$lastPoiSearchTime = currentTime;
        
        try {
            BlockPos pos = villager.blockPosition();

            BatchPoiManager.getPoiInRange(level, pos, 48);
        } catch (Exception e) {

        }
    }
    
    @Unique
    private static void aki$initPoiOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isVillagerOptimizationEnabled();

            cached_poiSearchInterval = 20;
            
            bridge.debugLog("[AkiAsync] VillagerPoiOptimizeMixin initialized: enabled=" + 
                cached_enabled + " | interval=" + cached_poiSearchInterval + " ticks");
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
