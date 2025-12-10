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

/**
 * POI索引更新Mixin
 * 
 * 监听POI的添加和移除，自动更新POI空间索引
 * 
 * 策略：
 * 1. 在 PoiManager.add 之后，从 PoiSection 获取刚添加的 PoiRecord
 * 2. 在 PoiManager.remove 之前，记录要删除的 POI 位置
 * 
 * 注入点分析（PoiManager源码）：
 * - add(BlockPos, Holder<PoiType>): 第197行
 *   调用链: PoiManager.add -> PoiSection.add -> 创建PoiRecord
 * - remove(BlockPos): 第201行
 *   调用链: PoiManager.remove -> PoiSection.remove
 * 
 * @author AkiAsync
 */
@Mixin(PoiManager.class)
public abstract class PoiIndexUpdateMixin {
    
    /**
     * Shadow字段：PoiManager中的world字段
     * 来自源码第26行: private final net.minecraft.server.level.ServerLevel world;
     */
    @Shadow
    @Final
    private ServerLevel world;
    
    /**
     * Shadow方法：获取或加载PoiSection
     */
    @Shadow
    protected abstract Optional<PoiSection> getOrLoad(long pos);
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean enabled = true;
    
    /**
     * POI添加时更新索引
     * 
     * 注入点：add方法的RETURN（在POI已经添加到PoiSection之后）
     * 源码第197行: public void add(BlockPos pos, Holder<PoiType> type)
     * 
     * 策略：
     * 1. POI已经添加到PoiSection
     * 2. 从PoiSection获取刚添加的PoiRecord
     * 3. 添加到空间索引
     */
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
    
    /**
     * POI移除时更新索引
     * 
     * 注入点：remove方法的RETURN
     * 源码第201行: public void remove(BlockPos pos)
     */
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
    
    /**
     * 创建PoiRecord
     * 
     * 注意：这里创建的PoiRecord与PoiSection中的不是同一个实例
     * 但包含相同的位置和类型信息，足够用于空间索引
     */
    @Unique
    private PoiRecord akiasync$createPoiRecord(BlockPos pos, Holder<PoiType> type) {
        try {
            
            Runnable emptyRunnable = () -> {};
            return new PoiRecord(pos, type, emptyRunnable);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 初始化配置
     */
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
