package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = ServerLevel.class)
public abstract class ServerLevelTickBlockMixin {

    @Unique
    private static final boolean ENABLED = true;

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$conservativeAsyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (!ENABLED) return;

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);

        if (!blockState.is(block)) {
            ci.cancel();
            return;
        }

        String blockName = block.getDescriptionId().toLowerCase();
        if (isUnsafeBlock(blockName)) {
            return;
        }

        final ServerLevel taskLevel = level;
        final BlockPos taskPos = pos;
        final BlockState taskState = blockState;

        Thread asyncThread = new Thread(() -> {
            try {
                taskState.tick(taskLevel, taskPos, taskLevel.random);
            } catch (Throwable t) {
                if (isAsyncError(t)) {
                    taskLevel.getServer().execute(() -> {
                        try {
                            BlockState current = taskLevel.getBlockState(taskPos);
                            if (current.is(block)) {
                                current.tick(taskLevel, taskPos, taskLevel.random);
                            }
                        } catch (Throwable ignored) {}
                    });
                }
            }
        }, "AkiAsync-Conservative");

        asyncThread.setDaemon(true);
        asyncThread.start();

        ci.cancel();
    }

    @Unique
    private boolean isUnsafeBlock(String blockName) {
        return blockName.contains("leaves") ||
                blockName.contains("redstone") ||
                blockName.contains("piston") ||
                blockName.contains("water") ||
                blockName.contains("lava") ||
                blockName.contains("command") ||
                blockName.contains("structure") ||
                blockName.contains("observer") ||
                blockName.contains("comparator") ||
                blockName.contains("repeater") ||
                blockName.contains("bubble") ||
                blockName.contains("magma") ||
                blockName.contains("soul") ||
                blockName.contains("fire") ||
                blockName.contains("portal") ||
                blockName.contains("sculk") ||
                blockName.contains("spawner") ||
                blockName.contains("bed") ||
                blockName.contains("door") ||
                blockName.contains("chest") ||
                blockName.contains("furnace") ||
                blockName.contains("hopper") ||
                blockName.contains("dispenser") ||
                blockName.contains("dropper") ||
                blockName.contains("brewing") ||
                blockName.contains("beacon") ||
                blockName.contains("conduit") ||
                blockName.contains("enchant") ||
                blockName.contains("ender") ||
                blockName.contains("shulker") ||
                blockName.contains("respawn") ||
                blockName.contains("lodestone") ||
                blockName.contains("target") ||
                blockName.contains("bee") ||
                blockName.contains("honey") ||
                blockName.contains("crying") ||
                blockName.contains("pointed") ||
                blockName.contains("amethyst") ||
                blockName.contains("copper") ||
                blockName.contains("lightning") ||
                blockName.contains("candle") ||
                blockName.contains("cake") ||
                blockName.contains("farmland") ||
                blockName.contains("grass") ||
                blockName.contains("mycelium") ||
                blockName.contains("nylium") ||
                blockName.contains("coral") ||
                blockName.contains("sea") ||
                blockName.contains("kelp") ||
                blockName.contains("azalea") ||
                blockName.contains("mangrove") ||
                blockName.contains("frog") ||
                blockName.contains("sculk") ||
                blockName.contains("reinforced") ||
                blockName.contains("spore") ||
                blockName.contains("hanging") ||
                blockName.contains("decorated") ||
                blockName.contains("chiseled") ||
                blockName.contains("infested") ||
                blockName.contains("tnt") ||
                blockName.contains("sponge");
    }

    @Unique
    private boolean isAsyncError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        String className = t.getClass().getName();
        return (msg != null && (
                msg.contains("async") ||
                        msg.contains("main thread") ||
                        msg.contains("thread")
        )) || className.contains("AsyncCatcher");
    }
}
