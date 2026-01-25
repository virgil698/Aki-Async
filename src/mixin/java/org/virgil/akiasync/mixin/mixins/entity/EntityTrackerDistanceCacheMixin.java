package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(ChunkMap.TrackedEntity.class)
public abstract class EntityTrackerDistanceCacheMixin {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    abstract int getEffectiveRange();

    @Unique
    private int akiasync$lastDistanceUpdateTick = 0;

    @Unique
    private int akiasync$cachedMaxDistance = 0;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Unique
    private static volatile boolean akiasync$enabled = true;

    @Redirect(
        method = "updatePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;getEffectiveRange()I"
        ),
        require = 0
    )
    private int akiasync$redirectGetEffectiveRange(ChunkMap.TrackedEntity instance) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }

        if (!akiasync$enabled) {
            return this.getEffectiveRange();
        }

        try {
            if (this.entity == null || this.entity.level() == null) {
                return this.getEffectiveRange();
            }

            int currentTick = this.entity.level().getServer().getTickCount();

            if (akiasync$lastDistanceUpdateTick != currentTick || akiasync$cachedMaxDistance == 0) {
                akiasync$cachedMaxDistance = this.getEffectiveRange();
                akiasync$lastDistanceUpdateTick = currentTick;
            }

            return akiasync$cachedMaxDistance;
        } catch (Exception e) {
            return this.getEffectiveRange();
        }
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (akiasync$initialized) {
            return;
        }

        try {
            var bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isEntityTrackerDistanceCacheEnabled();
                BridgeConfigCache.debugLog("[AkiAsync-TrackerDistanceCache] VMP-style optimization enabled: %s", akiasync$enabled);
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$enabled = true;
        }
    }
}
