package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Delegate class handling explosion event related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class ExplosionBridgeDelegate {

    private ConfigManager config;

    public ExplosionBridgeDelegate(ConfigManager config) {
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public List<net.minecraft.core.BlockPos> fireEntityExplodeEvent(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.phys.Vec3 center,
            List<net.minecraft.core.BlockPos> blocks,
            float yield) {

        org.bukkit.World bukkitWorld = level.getWorld();
        org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
        org.bukkit.Location bukkitLocation = new org.bukkit.Location(
            bukkitWorld, center.x, center.y, center.z
        );

        List<org.bukkit.block.Block> bukkitBlocks = new ArrayList<>();
        for (net.minecraft.core.BlockPos pos : blocks) {
            bukkitBlocks.add(bukkitWorld.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
        }

        org.bukkit.event.entity.EntityExplodeEvent explodeEvent =
            new org.bukkit.event.entity.EntityExplodeEvent(
                bukkitEntity, bukkitLocation, bukkitBlocks, yield,
                org.bukkit.ExplosionResult.DESTROY
            );
        org.bukkit.Bukkit.getPluginManager().callEvent(explodeEvent);

        if (explodeEvent.isCancelled()) {
            if (isTNTDebugEnabled()) {
                org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync-TNT] EntityExplodeEvent was cancelled by a plugin");
            }
            return Collections.emptyList();
        }

        List<net.minecraft.core.BlockPos> result = new ArrayList<>();
        for (org.bukkit.block.Block block : explodeEvent.blockList()) {
            result.add(new net.minecraft.core.BlockPos(block.getX(), block.getY(), block.getZ()));
        }

        if (isTNTDebugEnabled()) {
            int removed = blocks.size() - result.size();
            if (removed > 0) {
                org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync-TNT] EntityExplodeEvent: %d blocks removed by protection plugins", removed);
            }
        }

        return result;
    }

    private boolean isTNTDebugEnabled() {
        return config != null && config.isTNTDebugEnabled();
    }
}
