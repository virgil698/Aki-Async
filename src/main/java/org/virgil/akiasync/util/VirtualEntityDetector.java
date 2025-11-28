package org.virgil.akiasync.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.UUID;
import java.util.logging.Logger;

public class VirtualEntityDetector {

    private static final String ZNPC_PREFIX = "[ZNPC] ";
    private static Logger logger = null;
    private static boolean debugEnabled = false;

    public static void setLogger(Logger log, boolean debug) {
        logger = log;
        debugEnabled = debug;
    }

    public static boolean isVirtualEntity(Entity entity) {
        if (entity == null) return false;

        try {
            UUID uuid = entity.getUUID();
            if (uuid == null) {
                debugLog("Virtual entity detected: null UUID");
                return true;
            }

            Level level = entity.level();
            if (level != null) {
                Entity foundEntity = level.getEntity(entity.getId());
                if (foundEntity == null || foundEntity != entity) {
                    debugLog("Virtual entity detected: not in world entity list, id=" + entity.getId());
                    return true;
                }
            }

            if (isZNPCEntity(entity)) {
                debugLog("Virtual entity detected: ZNPCS NPC, uuid=" + uuid);
                return true;
            }

            if (isQuickShopDisplayItem(entity)) {
                debugLog("Virtual entity detected: QuickShop display item, uuid=" + uuid);
                return true;
            }

            if (hasVirtualEntityMarkers(entity)) {
                debugLog("Virtual entity detected: has virtual markers, uuid=" + uuid);
                return true;
            }

        } catch (Throwable t) {
            debugLog("Virtual entity detection error: " + t.getMessage());
            return true;
        }

        return false;
    }

    private static boolean isZNPCEntity(Entity entity) {
        try {
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                return false;
            }

            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }

            String name = bukkitEntity.getName();
            if (name != null && name.startsWith(ZNPC_PREFIX)) {
                return true;
            }

            if (bukkitEntity.customName() != null) {
                String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(bukkitEntity.customName());
                if (customName != null && customName.startsWith(ZNPC_PREFIX)) {
                    return true;
                }
            }

        } catch (Throwable t) {
            
            debugLog("Error checking ZNPC entity: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static boolean isQuickShopDisplayItem(Entity entity) {
        if (!(entity instanceof ItemEntity)) {
            return false;
        }

        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (!(bukkitEntity instanceof org.bukkit.entity.Item)) {
                return false;
            }

            org.bukkit.entity.Item item = (org.bukkit.entity.Item) bukkitEntity;

            try {
                PersistentDataContainer pdc = item.getPersistentDataContainer();

                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    String keyStr = key.toString().toLowerCase();
                    if (keyStr.contains("display") || keyStr.contains("quickshop")) {
                        return true;
                    }
                }
            } catch (Throwable t) {
                
                debugLog("Error checking item PDC: " + t.getClass().getSimpleName());
            }

            try {
                org.bukkit.inventory.ItemStack itemStack = item.getItemStack();
                if (itemStack != null && itemStack.hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                            String keyStr = key.toString().toLowerCase();
                            if (keyStr.contains("display") || keyStr.contains("quickshop")) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                
                debugLog("Error checking ItemStack meta: " + t.getClass().getSimpleName());
            }

        } catch (Throwable t) {
            
            debugLog("Error checking QuickShop display item: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static boolean hasVirtualEntityMarkers(Entity entity) {
        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }

            try {
                PersistentDataContainer pdc = bukkitEntity.getPersistentDataContainer();

                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    String keyStr = key.toString().toLowerCase();
                    if (keyStr.contains("virtual") || keyStr.contains("fake") ||
                        keyStr.contains("packet") || keyStr.contains("npc") ||
                        keyStr.contains("hologram") || keyStr.contains("marker")) {
                        return true;
                    }
                }
            } catch (Throwable t) {
                
                debugLog("Error checking entity PDC: " + t.getClass().getSimpleName());
            }

        } catch (Throwable t) {
            
            debugLog("Error checking virtual entity markers: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static void debugLog(String message) {
        if (debugEnabled && logger != null) {
            logger.info("[AkiAsync-VirtualEntity] " + message);
        }
    }
}
