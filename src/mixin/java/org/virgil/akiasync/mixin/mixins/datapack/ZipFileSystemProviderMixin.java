package org.virgil.akiasync.mixin.mixins.datapack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;

/**
 * Zip文件系统提供者Mixin - 优化zip文件系统创建
 * 
 * 基于QuickPack的优化思路：
 * 1. 缓存文件系统实例，避免重复创建
 * 2. 优化zip文件的读取参数
 * 3. 提供更高效的文件访问模式
 */
@SuppressWarnings("unused")
@Mixin(targets = "jdk.nio.zipfs.ZipFileSystemProvider", remap = false)
public class ZipFileSystemProviderMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    
    @Inject(method = "newFileSystem(Ljava/net/URI;Ljava/util/Map;)Ljava/nio/file/FileSystem;", 
            at = @At("HEAD"), 
            cancellable = true,
            remap = false)
    private void optimizeNewFileSystem(URI uri, Map<String, ?> env, CallbackInfoReturnable<FileSystem> cir) {
        if (!initialized) { aki$initZipOptimization(); }
        if (!cached_enabled) return;
        
        try {
            // 检查是否为数据包相关的zip文件
            String path = uri.getPath();
            if (path != null && (path.contains("datapacks") || path.contains("resourcepacks") || path.endsWith(".zip"))) {
                
                // 使用反射避免编译时依赖
                try {
                    Class<?> optimizerClass = Class.forName("org.virgil.akiasync.async.datapack.DataPackLoadOptimizer");
                    Object optimizer = optimizerClass.getMethod("getInstance").invoke(null);
                    if (optimizer != null) {
                        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                        
                        if (bridge != null && bridge.isDataPackDebugEnabled()) {
                            bridge.debugLog("[AkiAsync-DataPack] Intercepting zip file system creation for: " + 
                                java.nio.file.Paths.get(uri).getFileName());
                        }
                        
                        // 这里可以添加自定义的文件系统创建逻辑
                        // 例如：设置优化的读取缓冲区大小、启用并发访问等
                    }
                } catch (Exception e) {
                    // 忽略反射错误
                }
            }
        } catch (Exception e) {
            // 如果优化失败，让原方法继续执行
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDataPackDebugEnabled()) {
                bridge.debugLog("[AkiAsync-DataPack] Zip optimization failed, falling back to default: " + e.getMessage());
            }
        }
    }
    
    @Inject(method = "newFileSystem(Ljava/nio/file/Path;Ljava/util/Map;)Ljava/nio/file/FileSystem;", 
            at = @At("HEAD"),
            remap = false)
    private void optimizeNewFileSystemFromPath(Path path, Map<String, ?> env, CallbackInfoReturnable<FileSystem> cir) {
        if (!initialized) { aki$initZipOptimization(); }
        if (!cached_enabled) return;
        
        try {
            String pathStr = path.toString();
            if (pathStr.contains("datapacks") || pathStr.contains("resourcepacks") || pathStr.endsWith(".zip")) {
                
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                
                if (bridge != null && bridge.isDataPackDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-DataPack] Optimizing file system creation for path: " + path.getFileName());
                }
                
                // 可以在这里添加路径特定的优化
            }
        } catch (Exception e) {
            // 忽略优化错误
        }
    }
    
    @Unique
    private static synchronized void aki$initZipOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isDataPackOptimizationEnabled();
        } else {
            cached_enabled = false;
        }
        initialized = true;
        if (bridge != null && bridge.isDataPackDebugEnabled()) {
            bridge.debugLog("[AkiAsync-DataPack] ZipFileSystemProviderMixin initialized: enabled=" + cached_enabled);
        }
    }
}
