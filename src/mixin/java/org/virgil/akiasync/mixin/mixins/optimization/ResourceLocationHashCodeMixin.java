package org.virgil.akiasync.mixin.mixins.optimization;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceLocation.class)
public class ResourceLocationHashCodeMixin {
    
    @Unique
    private int akiasync$cachedHashCode = 0;
    
    @Unique
    private boolean akiasync$hashCodeComputed = false;
    
    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;)V", at = @At("RETURN"))
    private void cacheHashCodeOnInit(String namespace, String path, CallbackInfo ci) {
        
        ResourceLocation self = (ResourceLocation) (Object) this;
        akiasync$cachedHashCode = computeHashCode(namespace, path);
        akiasync$hashCodeComputed = true;
    }
    
    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void returnCachedHashCode(CallbackInfoReturnable<Integer> cir) {
        if (akiasync$hashCodeComputed) {
            cir.setReturnValue(akiasync$cachedHashCode);
        }
    }
    
    @Unique
    private int computeHashCode(String namespace, String path) {
        
        int result = namespace.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
