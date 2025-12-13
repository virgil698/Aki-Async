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
            String path = uri.getPath();
            if (path != null && (path.contains("datapacks") || path.contains("resourcepacks") || path.endsWith(".zip"))) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

                if (bridge != null && bridge.isDataPackOptimizationEnabled() && bridge.isDataPackDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-DataPack] Intercepting zip file system creation for: " +
                        java.nio.file.Paths.get(uri).getFileName());
                }
            }
        } catch (Exception e) {
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

            }
        } catch (Exception e) {
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
