package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndex;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(value = PoiManager.class, priority = 1500)
public abstract class PoiIndexUpdateMixin {
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    
    @Unique
    private static volatile boolean akiasync$enabled = true;
    
    @Inject(method = "add", at = @At("RETURN"), require = 0, remap = false)
    private void akiasync$onPoiAdd(BlockPos pos, Holder<PoiType> typeHolder, CallbackInfoReturnable<PoiRecord> cir) {
        akiasync$handlePoiAdd(pos, typeHolder);
    }
    
    @Inject(method = "remove", at = @At("RETURN"), require = 0)
    private void akiasync$onPoiRemove(BlockPos pos, CallbackInfo ci) {
        if (!akiasync$enabled) return;
        
        try {
            ServerLevel world = akiasync$getWorld();
            if (world == null) return;
            
            PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(world);
            if (index == null) return;
            
            index.removePoi(pos);
        } catch (Exception e) {
            
        }
    }
    
    @Unique
    private void akiasync$handlePoiAdd(BlockPos pos, Holder<PoiType> typeHolder) {
        if (!akiasync$initialized) {
            akiasync$init();
        }
        
        if (!akiasync$enabled) return;
        
        try {
            ServerLevel world = akiasync$getWorld();
            if (world == null) return;
            
            PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(world);
            if (index == null) return;
            
            PoiRecord record = akiasync$createPoiRecord(pos, typeHolder);
            if (record != null) {
                index.addPoi(record);
            }
        } catch (Exception e) {
            
        }
    }
    
    @Unique
    private ServerLevel akiasync$getWorld() {
        try {
            PoiManager manager = (PoiManager) (Object) this;
            java.lang.reflect.Field worldField = manager.getClass().getDeclaredField("world");
            worldField.setAccessible(true);
            return (ServerLevel) worldField.get(manager);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Unique
    private PoiRecord akiasync$createPoiRecord(BlockPos pos, Holder<PoiType> type) {
        try {
            Runnable emptyRunnable = () -> {};
            return new PoiRecord(pos, type, emptyRunnable);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (akiasync$initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            akiasync$enabled = bridge.isAiSpatialIndexEnabled() && 
                     bridge.isAiSpatialIndexPoiIndexEnabled();
            
            if (akiasync$enabled) {
                bridge.debugLog("[AkiAsync] POI Index Update Mixin initialized: enabled=true");
            }
        } else {
            akiasync$enabled = true;
        }
        
        akiasync$initialized = true;
    }
}
