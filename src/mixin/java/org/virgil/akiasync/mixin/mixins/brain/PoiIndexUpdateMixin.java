package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndex;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Optional;

@Mixin(PoiManager.class)
public abstract class PoiIndexUpdateMixin {
    
    @Shadow
    @Final
    private ServerLevel world;
    
    @Shadow
    protected abstract Optional<PoiSection> getOrLoad(long pos);
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean enabled = true;
    
    @Inject(method = "add", at = @At("RETURN"), require = 0)
    private void akiasync$onPoiAdd(BlockPos pos, Holder<PoiType> typeHolder, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled || this.world == null) return;
        
        try {
            
            PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(this.world);
            if (index == null) return;
            
            long sectionPos = SectionPos.asLong(pos);
            Optional<PoiSection> sectionOpt = this.getOrLoad(sectionPos);
            
            if (sectionOpt.isPresent()) {
                PoiSection section = sectionOpt.get();
                
                PoiRecord record = akiasync$createPoiRecord(pos, typeHolder);
                if (record != null) {
                    index.addPoi(record);
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    @Inject(method = "remove", at = @At("RETURN"), require = 0)
    private void akiasync$onPoiRemove(BlockPos pos, CallbackInfo ci) {
        if (!enabled || this.world == null) return;
        
        try {
            
            PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(this.world);
            if (index == null) return;
            
            index.removePoi(pos);
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
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isAiSpatialIndexEnabled() && 
                     bridge.isAiSpatialIndexPoiIndexEnabled();
            
            if (enabled) {
                bridge.debugLog("[AkiAsync] POI Index Update Mixin initialized: enabled=true");
            }
        } else {
            enabled = true;
        }
        
        initialized = true;
    }
}
