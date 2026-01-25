package org.virgil.akiasync.mixin.mixins.entity;

import ca.spottedleaf.moonrise.common.list.IteratorSafeOrderedReferenceSet;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.collision.NativeCollisionPusher;

@Mixin(value = ServerLevel.class, priority = 1100)
public abstract class NativeCollisionTickMixin {

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean enabled = false;

    @Unique
    private static volatile boolean nativeCollisionsSupported = false;

    @Inject(
        method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "Lca/spottedleaf/moonrise/patches/chunk_system/level/ChunkSystemServerLevel;getRegionizedWorldData()Lca/spottedleaf/moonrise/patches/chunk_system/level/ChunkSystemServerLevel$RegionizedWorldData;",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void processNativeCollisionsInPreTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initNativeCollisions();
        }

        if (!enabled || !nativeCollisionsSupported) {
            return;
        }

        try {
            ServerLevel level = (ServerLevel) (Object) this;

            Object regionizedData = akiasync$getRegionizedWorldData(level);
            if (regionizedData == null) {
                return;
            }

            IteratorSafeOrderedReferenceSet tickList = akiasync$getTickList(regionizedData);
            if (tickList == null || tickList.getListSize() <= 1) {
                return;
            }

            Object[] listRaw = tickList.getListRaw();
            int listSize = tickList.getListSize();

            if (listRaw == null || listSize == 0) {
                return;
            }

            EntityLookup lookup = ((ChunkSystemLevel) level).moonrise$getEntityLookup();
            if (lookup == null) {
                return;
            }

            NativeCollisionPusher.processCollisionBatch(listRaw, lookup, listSize);

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "NativeCollisionTickMixin", "processNativeCollisions", e);
        }
    }

    @Unique
    private static synchronized void akiasync$initNativeCollisions() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isNativeCollisionsEnabled();
                nativeCollisionsSupported = NativeCollisionPusher.isSupported();

                bridge.debugLog("[AkiAsync] NativeCollisionTickMixin initialized:");
                bridge.debugLog("[AkiAsync]   - Native Collisions Enabled: " + enabled);
                bridge.debugLog("[AkiAsync]   - AVX/Vector API Supported: " + nativeCollisionsSupported);
                bridge.debugLog("[AkiAsync]   - Mode: Off-heap memory + SIMD vectorization");

                if (enabled && !nativeCollisionsSupported) {
                    bridge.errorLog("[AkiAsync] WARNING: Native collisions enabled but Vector API not available!");
                    bridge.errorLog("[AkiAsync] Make sure you're running JDK 17+ with --add-modules jdk.incubator.vector");
                }

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "NativeCollisionTickMixin", "init", e);
            enabled = false;
        }
    }

    @Unique
    private Object akiasync$getRegionizedWorldData(ServerLevel level) {
        try {

            var method = level.getClass().getMethod("getRegionizedWorldData");
            method.setAccessible(true);
            return method.invoke(level);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private IteratorSafeOrderedReferenceSet akiasync$getTickList(Object regionizedData) {
        try {
            var method = regionizedData.getClass().getMethod("getTickList");
            method.setAccessible(true);
            return (IteratorSafeOrderedReferenceSet) method.invoke(regionizedData);
        } catch (Exception e) {
            return null;
        }
    }
}
