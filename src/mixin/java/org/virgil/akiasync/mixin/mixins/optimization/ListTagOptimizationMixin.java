package org.virgil.akiasync.mixin.mixins.optimization;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.List;

@Mixin(value = ListTag.class, priority = 1100)
public abstract class ListTagOptimizationMixin {
    
    @Shadow @Final
    private List<Tag> list;
    
    @Shadow
    public abstract byte identifyRawElementType();
    
    @Unique
    private static volatile boolean akiasync$enabled = true;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isNbtOptimizationEnabled();
            
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$enabled = true;
        }
    }
    
    @Overwrite
    public ListTag copy() {
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        ListTag result = new ListTag();
        
        if (!akiasync$enabled) {
            for (Tag tag : this.list) {
                result.add(tag.copy());
            }
            return result;
        }
        
        for (Tag tag : this.list) {
            result.add(tag.copy());
        }
        
        try {
            ObjectArrayList<Tag> optimizedList = new ObjectArrayList<>(this.list.size());
            for (int i = 0; i < result.size(); i++) {
                optimizedList.add(result.get(i));
            }
            
            java.lang.reflect.Field listField = ListTag.class.getDeclaredField("list");
            listField.setAccessible(true);
            listField.set(result, optimizedList);
        } catch (Exception e) {
        }
        
        return result;
    }
}
