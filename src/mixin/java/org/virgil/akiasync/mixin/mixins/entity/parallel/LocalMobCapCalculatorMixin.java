package org.virgil.akiasync.mixin.mixins.entity.parallel;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(LocalMobCapCalculator.class)
public class LocalMobCapCalculatorMixin {

    @Shadow
    private final Map<?, ?> playerMobCounts = new ConcurrentHashMap<>();

    @Shadow
    private final Long2ObjectMap<List<ServerPlayer>> playersNearChunk = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    @Inject(method = "getPlayersNear", at = @At("RETURN"), cancellable = true)
    private void aki$safeGetPlayersNear(ChunkPos pos, CallbackInfoReturnable<List<ServerPlayer>> cir) {
        if (cir.getReturnValue() == null) {
            cir.setReturnValue(Collections.emptyList());
        }
    }
}
