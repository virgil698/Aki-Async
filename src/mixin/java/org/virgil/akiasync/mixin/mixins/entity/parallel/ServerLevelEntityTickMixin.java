package org.virgil.akiasync.mixin.mixins.entity.parallel;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.parallel.ParallelEntityProcessor;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("all")
@Mixin(value = ServerLevel.class, priority = 1500)
public abstract class ServerLevelEntityTickMixin {

    @Shadow @Final public EntityTickList entityTickList;
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Mutable @Final private List<ServerPlayer> players;

    @Unique private ConcurrentLinkedQueue<BlockEventData> aki$syncedBlockEventQueue;
    @Unique private static volatile boolean aki$initialized = false;
    @Unique private static volatile boolean aki$isFolia = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aki$init(CallbackInfo ci) {
        if (!aki$initialized) {
            aki$checkEnvironment();
        }

        aki$syncedBlockEventQueue = new ConcurrentLinkedQueue<>();

        ParallelEntityProcessor.init();
    }

    @Unique
    private static synchronized void aki$checkEnvironment() {
        if (aki$initialized) return;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            aki$isFolia = true;
            BridgeConfigCache.debugLog("[ServerLevelEntityTick] Folia detected, parallel entity tick disabled");
        } catch (ClassNotFoundException e) {
            aki$isFolia = false;
            BridgeConfigCache.debugLog("[ServerLevelEntityTick] Standard Paper detected, parallel entity tick enabled");
        }
        aki$initialized = true;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void aki$parallelEntityTick(EntityTickList entityTickList, Consumer<Entity> consumer) {
        if (aki$isFolia || ParallelEntityProcessor.isDisabled()) {

            entityTickList.forEach(consumer);
            return;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            int minEntities = bridge.getMinEntitiesForParallel();

            int entityCount = ((ServerLevel) (Object) this).moonrise$getEntityLookup().getEntityCount();
            if (entityCount < minEntities) {

                entityTickList.forEach(consumer);
                return;
            }
        }

        ProfilerFiller profiler = Profiler.get();
        ServerLevel level = (ServerLevel) (Object) this;

        entityTickList.forEach(entity -> {

            if (entity.isRemoved()) return;

            if (entity instanceof Mob mob && mob.isDeadOrDying()) {
                return;
            }

            if (level.tickRateManager().isEntityFrozen(entity)) return;

            profiler.push("checkDespawn");
            ParallelEntityProcessor.asyncDespawn(entity);
            profiler.pop();

            if (!this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) {
                return;
            }

            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                if (!vehicle.isRemoved() && vehicle.hasPassenger(entity)) {
                    return;
                }
                entity.stopRiding();
            }

            profiler.push("tick");
            ParallelEntityProcessor.callEntityTick(level, entity);
            profiler.pop();
        });

        profiler.push("waitAsyncTick");
        ParallelEntityProcessor.postEntityTick();
        profiler.pop();
    }

    @Redirect(method = "blockEvent", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;add(Ljava/lang/Object;)Z", remap = false))
    private boolean aki$blockEventAdd(ObjectLinkedOpenHashSet<BlockEventData> set, Object obj) {
        return aki$syncedBlockEventQueue.add((BlockEventData) obj);
    }

    @Redirect(method = "clearBlockEvents", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeIf(Ljava/util/function/Predicate;)Z", remap = false))
    private boolean aki$blockEventRemoveIf(ObjectLinkedOpenHashSet<BlockEventData> set, Predicate<BlockEventData> filter) {
        return aki$syncedBlockEventQueue.removeIf(filter);
    }

    @Redirect(method = "runBlockEvents", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;isEmpty()Z", remap = false))
    private boolean aki$blockEventIsEmpty(ObjectLinkedOpenHashSet<BlockEventData> set) {
        return aki$syncedBlockEventQueue.isEmpty();
    }

    @Redirect(method = "runBlockEvents", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeFirst()Ljava/lang/Object;", remap = false))
    private Object aki$blockEventRemoveFirst(ObjectLinkedOpenHashSet<BlockEventData> set) {
        return aki$syncedBlockEventQueue.poll();
    }

    @Redirect(method = "runBlockEvents", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;addAll(Ljava/util/Collection;)Z", remap = false))
    private boolean aki$blockEventAddAll(ObjectLinkedOpenHashSet<BlockEventData> set, Collection<? extends BlockEventData> c) {
        return aki$syncedBlockEventQueue.addAll(c);
    }
}
