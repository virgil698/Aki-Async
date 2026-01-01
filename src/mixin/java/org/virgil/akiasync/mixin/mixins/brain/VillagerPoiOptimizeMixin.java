package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

@Mixin(value = Villager.class, priority = 1100)
public class VillagerPoiOptimizeMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean cached_debugEnabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static long queryCount = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void optimizedPoiQuery(CallbackInfo ci) {
        if (!initialized) {
            aki$initPoiOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        Villager villager = (Villager) (Object) this;
        ServerLevel level = (ServerLevel) villager.level();
        
        try {
            
            List<PoiRecord> nearbyPoi = AiQueryHelper.getNearbyPoi(villager, 48);
            
            queryCount++;
            
            if (cached_debugEnabled && queryCount % 1000 == 0) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(
                        "[AkiAsync-Villager] POI Query #%d: Found %d POIs using spatial index",
                        queryCount, nearbyPoi.size()
                    );
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "VillagerPoiOptimize", "optimizedPoiQuery", e);
        }
    }
    
    @Unique
    private static synchronized void aki$initPoiOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isVillagerOptimizationEnabled() && 
                           bridge.isAiSpatialIndexEnabled();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            bridge.debugLog("[AkiAsync] VillagerPoiOptimizeMixin (Upgraded) initialized: enabled=" + 
                cached_enabled + " | Using AI Spatial Index");
        
            initialized = true;
        } else {
            cached_enabled = false;
        }
    }
}
