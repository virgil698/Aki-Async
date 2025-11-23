package org.virgil.akiasync.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 虚拟实体检测器 - 用于检测ZNPCS、QuickShop等插件创建的发包类实体
 * 
 * 虚拟实体特征:
 * 1. ZNPCS NPC: 名称以"[ZNPC]"开头，使用GameProfile，通过数据包显示
 * 2. QuickShop展示物品: ItemEntity带有特殊的PersistentData标记
 * 3. 通用特征: 不在服务端实体列表中，或UUID为null
 */
public class VirtualEntityDetector {
    
    private static final String ZNPC_PREFIX = "[ZNPC] ";
    private static Logger logger = null;
    private static boolean debugEnabled = false;
    
    public static void setLogger(Logger log, boolean debug) {
        logger = log;
        debugEnabled = debug;
    }
    
    /**
     * 检测实体是否为虚拟实体（发包类实体）
     * 
     * @param entity 要检测的实体
     * @return true表示是虚拟实体，应该跳过优化
     */
    public static boolean isVirtualEntity(Entity entity) {
        if (entity == null) return false;
        
        try {
            // 1. 检查UUID - 虚拟实体可能没有UUID
            UUID uuid = entity.getUUID();
            if (uuid == null) {
                debugLog("Virtual entity detected: null UUID");
                return true;
            }
            
            // 2. 检查是否在世界实体列表中
            Level level = entity.level();
            if (level != null) {
                Entity foundEntity = level.getEntity(entity.getId());
                if (foundEntity == null || foundEntity != entity) {
                    debugLog("Virtual entity detected: not in world entity list, id=" + entity.getId());
                    return true;
                }
            }
            
            // 3. 检查ZNPCS NPC特征
            if (isZNPCEntity(entity)) {
                debugLog("Virtual entity detected: ZNPCS NPC, uuid=" + uuid);
                return true;
            }
            
            // 4. 检查QuickShop展示物品特征
            if (isQuickShopDisplayItem(entity)) {
                debugLog("Virtual entity detected: QuickShop display item, uuid=" + uuid);
                return true;
            }
            
            // 5. 检查其他虚拟实体特征
            if (hasVirtualEntityMarkers(entity)) {
                debugLog("Virtual entity detected: has virtual markers, uuid=" + uuid);
                return true;
            }
            
        } catch (Throwable t) {
            // 检测出错时保守处理，认为是虚拟实体
            debugLog("Virtual entity detection error: " + t.getMessage());
            return true;
        }
        
        return false;
    }
    
    /**
     * 检测是否为ZNPCS创建的NPC实体
     */
    private static boolean isZNPCEntity(Entity entity) {
        try {
            // ZNPCS的NPC通常是玩家类型
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                return false;
            }
            
            // 获取Bukkit实体
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }
            
            // 检查名称是否以[ZNPC]开头
            String name = bukkitEntity.getName();
            if (name != null && name.startsWith(ZNPC_PREFIX)) {
                return true;
            }
            
            // 检查自定义名称
            if (bukkitEntity.customName() != null) {
                String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(bukkitEntity.customName());
                if (customName != null && customName.startsWith(ZNPC_PREFIX)) {
                    return true;
                }
            }
            
        } catch (Throwable t) {
            // 忽略异常
        }
        
        return false;
    }
    
    /**
     * 检测是否为QuickShop的展示物品
     */
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
            
            // 检查PersistentData标记
            try {
                PersistentDataContainer pdc = item.getPersistentDataContainer();
                
                // 遍历所有keys，查找包含"display"或"quickshop"的标记
                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    String keyStr = key.toString().toLowerCase();
                    if (keyStr.contains("display") || keyStr.contains("quickshop")) {
                        return true;
                    }
                }
            } catch (Throwable t) {
                // 忽略异常
            }
            
            // 检查物品元数据
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
                // 忽略异常
            }
            
        } catch (Throwable t) {
            // 忽略异常
        }
        
        return false;
    }
    
    /**
     * 检测其他虚拟实体标记
     */
    private static boolean hasVirtualEntityMarkers(Entity entity) {
        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }
            
            // 检查PersistentData中的常见虚拟实体标记
            try {
                PersistentDataContainer pdc = bukkitEntity.getPersistentDataContainer();
                
                // 遍历所有keys，查找常见的虚拟实体标记
                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    String keyStr = key.toString().toLowerCase();
                    if (keyStr.contains("virtual") || keyStr.contains("fake") || 
                        keyStr.contains("packet") || keyStr.contains("npc") ||
                        keyStr.contains("hologram") || keyStr.contains("marker")) {
                        return true;
                    }
                }
            } catch (Throwable t) {
                // 忽略异常
            }
            
        } catch (Throwable t) {
            // 忽略异常
        }
        
        return false;
    }
    
    /**
     * 调试日志输出
     */
    private static void debugLog(String message) {
        if (debugEnabled && logger != null) {
            logger.info("[AkiAsync-VirtualEntity] " + message);
        }
    }
}
