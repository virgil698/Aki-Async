package org.virgil.akiasync.mixin.mixins.memory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * BlockPos object pool to reduce allocations (FerriteCore inspired).
 * PathNavigation creates many BlockPos - reuse MutableBlockPos instead.
 */
@SuppressWarnings("unused")
@Mixin(PathNavigation.class)
public abstract class BlockPosPoolMixin {

    @Shadow protected Mob mob;
    
    // Thread-local BlockPos pool (FerriteCore pattern)
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS_POOL = 
        ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    
    private static volatile boolean enabled;
    private static volatile boolean initialized = false;

    /**
     * Reuse MutableBlockPos for Vec3 -> BlockPos conversion.
     * Avoids allocating new BlockPos on every createPath call.
     */
    @Inject(method = "createPath(Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void reuseBlockPos(Vec3 target, int accuracy, CallbackInfoReturnable<Path> cir) {
        if (!initialized) { akiasync$initBlockPosPool(); }
        if (!enabled) return;
        
        // Use pooled MutableBlockPos instead of BlockPos.containing()
        BlockPos.MutableBlockPos pooled = POS_POOL.get();
        pooled.set(target.x, target.y, target.z);
        
        // Call createPath(BlockPos, int) with pooled pos
        PathNavigation nav = (PathNavigation) (Object) this;
        Path result = nav.createPath(pooled, accuracy);
        cir.setReturnValue(result);
    }
    
    private static synchronized void akiasync$initBlockPosPool() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isBlockPosPoolEnabled();
        } else {
            enabled = true;
        }
        initialized = true;
        System.out.println("[AkiAsync] BlockPosPoolMixin initialized: enabled=" + enabled);
    }
}

