package org.virgil.akiasync.mixin.mixins.world.tick;

import org.virgil.akiasync.mixin.util.concurrent.Long2ObjectConcurrentHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> implements LevelTickAccess<T> {
    @Shadow
    private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectConcurrentHashMap<>();

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "sortContainersToTick")
    private void updateStatus(long gameTime, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(gameTime);
        }
    }
}
