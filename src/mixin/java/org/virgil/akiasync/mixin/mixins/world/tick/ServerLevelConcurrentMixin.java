package org.virgil.akiasync.mixin.mixins.world.tick;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"unused", "unchecked"})
@Mixin(value = ServerLevel.class, priority = 900)
public abstract class ServerLevelConcurrentMixin {

    @Shadow
    @Mutable
    @Final
    List<ServerPlayer> players;

    @Unique
    private static volatile boolean akiasync$isFolia = false;
    @Unique
    private static volatile boolean akiasync$checkedFolia = false;
    @Unique
    private static volatile Field akiasync$navigatingMobsField = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void akiasync$initConcurrentCollections(CallbackInfo ci) {

        if (!akiasync$checkedFolia) {
            akiasync$checkFoliaEnvironment();
        }

        List<ServerPlayer> oldPlayers = this.players;
        this.players = new CopyOnWriteArrayList<>();
        if (oldPlayers != null && !oldPlayers.isEmpty()) {
            this.players.addAll(oldPlayers);
        }

        if (!akiasync$isFolia && akiasync$navigatingMobsField != null) {
            try {
                Set<Mob> oldNavigatingMobs = (Set<Mob>) akiasync$navigatingMobsField.get(this);
                Set<Mob> newNavigatingMobs = ConcurrentHashMap.newKeySet();
                if (oldNavigatingMobs != null && !oldNavigatingMobs.isEmpty()) {
                    newNavigatingMobs.addAll(oldNavigatingMobs);
                }
                akiasync$navigatingMobsField.set(this, newNavigatingMobs);
            } catch (Exception e) {

            }
        }
    }

    @Unique
    private static synchronized void akiasync$checkFoliaEnvironment() {
        if (akiasync$checkedFolia) return;

        try {

            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            akiasync$isFolia = true;
            org.virgil.akiasync.mixin.util.BridgeConfigCache.debugLog(
                "[ServerLevelConcurrent] Folia/Luminol detected, skipping navigatingMobs replacement");
        } catch (ClassNotFoundException e) {
            akiasync$isFolia = false;

            try {
                akiasync$navigatingMobsField = ServerLevel.class.getDeclaredField("navigatingMobs");
                akiasync$navigatingMobsField.setAccessible(true);
                org.virgil.akiasync.mixin.util.BridgeConfigCache.debugLog(
                    "[ServerLevelConcurrent] Standard Paper detected, will replace navigatingMobs");
            } catch (NoSuchFieldException ex) {

                org.virgil.akiasync.mixin.util.BridgeConfigCache.debugLog(
                    "[ServerLevelConcurrent] navigatingMobs field not found, skipping");
            }
        }

        akiasync$checkedFolia = true;
    }
}
