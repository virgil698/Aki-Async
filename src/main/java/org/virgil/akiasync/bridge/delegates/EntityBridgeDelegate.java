package org.virgil.akiasync.bridge.delegates;

import org.virgil.akiasync.AkiAsyncPlugin;

/**
 * Delegate class handling entity-related bridge methods like throttling.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class EntityBridgeDelegate {

    private final AkiAsyncPlugin plugin;

    public EntityBridgeDelegate(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean shouldThrottleEntity(Object entity) {
        if (plugin.getThrottlingManager() == null) {
            return false;
        }

        org.bukkit.entity.Entity bukkitEntity = null;

        if (entity instanceof org.bukkit.entity.Entity) {
            bukkitEntity = (org.bukkit.entity.Entity) entity;
        } else if (entity instanceof net.minecraft.world.entity.Entity) {
            try {
                net.minecraft.world.entity.Entity mcEntity = (net.minecraft.world.entity.Entity) entity;
                bukkitEntity = mcEntity.getBukkitEntity();
            } catch (Exception e) {
                return false;
            }
        }

        if (bukkitEntity == null) {
            return false;
        }

        return plugin.getThrottlingManager().shouldThrottle(bukkitEntity);
    }

    public boolean isVirtualEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            return org.virgil.akiasync.util.VirtualEntityDetector.isVirtualEntity(bukkitEntity);
        } catch (Exception e) {
            return false;
        }
    }
}
