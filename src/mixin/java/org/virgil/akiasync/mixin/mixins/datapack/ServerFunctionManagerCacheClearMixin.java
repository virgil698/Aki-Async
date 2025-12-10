package org.virgil.akiasync.mixin.mixins.datapack;

import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

/**
 * 数据包重载时清理命令去重缓存
 * Clear command deduplication cache on datapack reload
 * 
 * @author AkiAsync
 */
@Mixin(ServerFunctionManager.class)
public class ServerFunctionManagerCacheClearMixin {
    
    /**
     * 在数据包重载后清理缓存
     * Clear cache after datapack reload
     */
    @Inject(method = "replaceLibrary", at = @At("RETURN"))
    private void clearDeduplicationCache(ServerFunctionLibrary reloader, CallbackInfo ci) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isCommandDeduplicationEnabled()) {
            
            try {
                Class<?> mixinClass = Class.forName("org.virgil.akiasync.mixin.mixins.datapack.CommandFunctionDeduplicatorMixin");
                java.lang.reflect.Method clearMethod = mixinClass.getDeclaredMethod("clearCache");
                clearMethod.setAccessible(true);
                clearMethod.invoke(null);
                
                if (bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync] Command deduplication cache cleared after datapack reload");
                }
            } catch (Exception e) {
                if (bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync] Failed to clear command deduplication cache: %s", e.getMessage());
                }
            }
        }
    }
}
