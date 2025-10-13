package org.virgil.akiasync.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin configuration plugin for AkiAsync
 * Allows dynamic mixin loading and configuration
 * 
 * @author Virgil
 */
public class AkiAsyncMixinPlugin implements IMixinConfigPlugin {
    
    @Override
    public void onLoad(String mixinPackage) {
        System.out.println("[AkiAsync] Mixin plugin loaded: " + mixinPackage);
        
        // No-op: Mixins will fetch config from plugin via Bridge at first use.
        System.out.println("[AkiAsync] Using Bridge pattern for mixin configuration (lazy loading from plugin)");
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // You can add conditional mixin loading here
        // For now, all mixins are enabled
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No-op
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }
}


