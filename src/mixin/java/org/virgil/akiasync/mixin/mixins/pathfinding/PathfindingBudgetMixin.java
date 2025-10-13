package org.virgil.akiasync.mixin.mixins.pathfinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Pathfinding budget - reads from BridgeManager (Leaves template pattern).
 */
@SuppressWarnings("unused")
@Mixin(PathNavigation.class)
public abstract class PathfindingBudgetMixin {

    private static volatile int cached_budget;
    private static volatile boolean initialized = false;

    @Shadow protected Mob mob;

    private static long akiasync$lastTick;
    private static int akiasync$remaining;
    private static final java.util.Map<Mob, Object[]> DEFERRED = new java.util.WeakHashMap<>();
    private static final int KIND_BLOCK = 0, KIND_VEC = 1, KIND_ENTITY = 2;

    @Inject(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void akiasync$budgetBlock(BlockPos target, int accuracy, CallbackInfoReturnable<Path> cir) {
        if (!initialized) { akiasync$initPathfindingBudget(); }
        if (shouldSkip()) {
            DEFERRED.put(this.mob, new Object[]{KIND_BLOCK, target.immutable(), null, null, accuracy});
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "createPath(Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void akiasync$budgetVec(Vec3 target, int accuracy, CallbackInfoReturnable<Path> cir) {
        if (shouldSkip()) {
            DEFERRED.put(this.mob, new Object[]{KIND_VEC, null, target, null, accuracy});
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void akiasync$budgetEntity(Entity target, int accuracy, CallbackInfoReturnable<Path> cir) {
        if (shouldSkip()) {
            DEFERRED.put(this.mob, new Object[]{KIND_ENTITY, null, null, target, accuracy});
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void akiasync$processDeferred(CallbackInfo ci) {
        try {
            if (DEFERRED.isEmpty() || cached_budget <= 0) return;
            long tick = ((ServerLevel) this.mob.level()).getGameTime();
            if (akiasync$lastTick != tick) { akiasync$lastTick = tick; akiasync$remaining = cached_budget; }
            if (akiasync$remaining <= 0) return;
            Object[] def = DEFERRED.remove(this.mob);
            if (def == null) return;
            akiasync$remaining--;
            PathNavigation nav = (PathNavigation) (Object) this;
            int kind = (Integer) def[0];
            switch (kind) {
                case KIND_BLOCK -> nav.createPath((BlockPos) def[1], (Integer) def[4]);
                case KIND_VEC -> nav.createPath(BlockPos.containing((Vec3) def[2]), (Integer) def[4]);
                case KIND_ENTITY -> nav.createPath((Entity) def[3], (Integer) def[4]);
            }
        } catch (Throwable ignored) {}
    }

    private boolean shouldSkip() {
        try {
            if (cached_budget <= 0) return false;
            long tick = ((ServerLevel) this.mob.level()).getGameTime();
            if (akiasync$lastTick != tick) { akiasync$lastTick = tick; akiasync$remaining = cached_budget; }
            if (akiasync$remaining <= 0) return true;
            akiasync$remaining--;
            return false;
        } catch (Throwable t) { return false; }
    }
    
    private static synchronized void akiasync$initPathfindingBudget() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_budget = bridge.getPathfindingTickBudget();
        } else {
            cached_budget = 0;
        }
        initialized = true;
        System.out.println("[AkiAsync] PathfindingBudgetMixin initialized: budget=" + cached_budget);
    }
}

