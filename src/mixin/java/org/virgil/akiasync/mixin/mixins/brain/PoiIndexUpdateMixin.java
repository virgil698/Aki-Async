package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndex;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;

@Mixin(value = PoiManager.class, priority = 1500)
public abstract class PoiIndexUpdateMixin {
    
    @Shadow
    private ServerLevel world;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    
    @Unique
    private static volatile boolean akiasync$enabled = true;
    
    @Inject(method = "add(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;)V", 
            at = @At("TAIL"), 
            require = 0)
    @SuppressWarnings("unused")
    private void akiasync$onPoiAddVoid(BlockPos pos, Holder<PoiType> typeHolder, CallbackInfo ci) {
        akiasync$handlePoiAdd(pos, typeHolder);
    }
    
    @Inject(method = "add(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/entity/ai/village/poi/PoiRecord;", 
            at = @At("RETURN"), 
            require = 0)
    @SuppressWarnings("unused")
    private void akiasync$onPoiAddReturn(BlockPos pos, Holder<PoiType> typeHolder, 
                                         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<PoiRecord> cir) {
        akiasync$handlePoiAdd(pos, typeHolder);
    }
    
    @Inject(method = "remove(Lnet/minecraft/core/BlockPos;)V", 
            at = @At("TAIL"), 
            require = 0)
    @SuppressWarnings("unused")
    private void akiasync$onPoiRemove(BlockPos pos, CallbackInfo ci) {
        if (!akiasync$enabled || world == null) return;
        
        try {
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
        
        if (!akiasync$enabled || world == null) return;
        
        try {
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
