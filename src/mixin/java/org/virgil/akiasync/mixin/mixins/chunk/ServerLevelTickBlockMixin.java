package org.virgil.akiasync.mixin.mixins.chunk;

import java.util.concurrent.ExecutorService;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
@Mixin(value = ServerLevel.class, priority = 1200)
public abstract class ServerLevelTickBlockMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    
    @Unique
    private static final ExecutorService ASYNC_BLOCK_TICK_EXECUTOR = 
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AkiAsync-BlockTick");
            t.setDaemon(true);
            return t;
        });
    
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$asyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (!initialized) aki$initBlockTickAsync();
        if (!cached_enabled) return;
        
        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);
        
        if (!blockState.is(block)) {
            ci.cancel();
            return;
        }
        
        ASYNC_BLOCK_TICK_EXECUTOR.execute(() -> {
            try {
                blockState.tick(level, pos, level.random);
            } catch (IllegalStateException e) {
                level.getServer().execute(() -> {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(block)) state.tick(level, pos, level.random);
                });
            } catch (Exception e) {
            }
        });
        
        ci.cancel();
    }
    
    @Unique
    private static synchronized void aki$initBlockTickAsync() {
        if (initialized) return;
        
        var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        cached_enabled = bridge != null && bridge.isChunkTickAsyncEnabled();
        
        initialized = true;
        System.out.println("[AkiAsync] ServerLevelTickBlockMixin initialized: enabled=" + cached_enabled);
        System.out.println("[AkiAsync]   ✅ Hooked: ServerLevel#tickBlock()");
        System.out.println("[AkiAsync]   💡 Strategy: Offload blockState.tick() to thread pool");
        System.out.println("[AkiAsync]   ⚠️  Risk: Thread safety depends on block implementation");
    }
}