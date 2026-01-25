package org.virgil.akiasync.cache;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;

import java.util.HashMap;
import java.util.Map;

public class SakuraCacheStatistics {

    public static Map<String, Object> getAllStatistics(AkiAsyncPlugin plugin) {
        if (plugin == null) {
            return new HashMap<>();
        }

        AkiAsyncBridge bridge = plugin.getBridge();
        if (bridge == null) {
            return new HashMap<>();
        }

        return bridge.getSakuraCacheStatistics();
    }

    public static String formatStatistics(AkiAsyncPlugin plugin) {
        Map<String, Object> stats = getAllStatistics(plugin);
        if (stats.isEmpty()) {
            return "§cSakura optimization cache statistics unavailable§r\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Sakura Optimization Cache Statistics ===§r\n");

        sb.append("§e[TNT Density Cache]§r\n");
        @SuppressWarnings("unchecked")
        Map<String, String> densityStats = (Map<String, String>) stats.get("density_cache");
        if (densityStats != null) {
            for (Map.Entry<String, String> entry : densityStats.entrySet()) {
                sb.append(String.format("  §7%s: §f%s§r%n", entry.getKey(), entry.getValue()));
            }
        }

        sb.append("§e[Async Density Cache]§r\n");
        @SuppressWarnings("unchecked")
        Map<String, String> asyncStats = (Map<String, String>) stats.get("async_density_cache");
        if (asyncStats != null) {
            for (Map.Entry<String, String> entry : asyncStats.entrySet()) {
                sb.append(String.format("  §7%s: §f%s§r%n", entry.getKey(), entry.getValue()));
            }
        }

        Object evaluatorCount = stats.get("pandawire_evaluators");
        if (evaluatorCount != null) {
            sb.append(String.format("§e[PandaWire Evaluators]§r%n  §7Cache count: §f%s§r%n", evaluatorCount));
        }

        sb.append("§e[Redstone Network Cache]§r\n");
        @SuppressWarnings("unchecked")
        Map<String, String> networkStats = (Map<String, String>) stats.get("network_cache");
        if (networkStats != null) {
            for (Map.Entry<String, String> entry : networkStats.entrySet()) {
                sb.append(String.format("  §7%s: §f%s§r%n", entry.getKey(), entry.getValue()));
            }
        }

        return sb.toString();
    }

    public static void performPeriodicCleanup(AkiAsyncPlugin plugin) {
        if (plugin == null) {
            return;
        }

        AkiAsyncBridge bridge = plugin.getBridge();
        if (bridge != null) {
            bridge.performSakuraCacheCleanup();
        }
    }
}
