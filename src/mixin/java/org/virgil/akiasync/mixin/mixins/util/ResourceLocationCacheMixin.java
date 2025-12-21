package org.virgil.akiasync.mixin.mixins.util;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;


@Mixin(ResourceLocation.class)
public class ResourceLocationCacheMixin {
    
    @Unique
    private static boolean akiasync$initialized = false;
    
    @Unique
    private static boolean akiasync$enabled = true;
    
    @Shadow @Final private String namespace;
    @Shadow @Final private String path;
    
    @Unique
    private String akiasync$cachedString = null;
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isResourceLocationCacheEnabled();
            }
        } catch (Exception e) {
            
            akiasync$enabled = true;
        }
        
        akiasync$initialized = true;
    }
    
    
    @Overwrite
    public String toString() {
        
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        
        if (!akiasync$enabled) {
            return this.namespace + ":" + this.path;
        }
        
        if (this.akiasync$cachedString != null) {
            return this.akiasync$cachedString;
        }
        
        String result = this.namespace + ":" + this.path;
        this.akiasync$cachedString = result;
        return result;
    }
}
