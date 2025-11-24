package org.virgil.akiasync.command;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
@NullMarked
public class VersionCommand implements BasicCommand {
    private final AkiAsyncPlugin plugin;
    public VersionCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        String prefix = "[AkiAsync] ";
        source.getSender().sendMessage(prefix + "========================================");
        source.getSender().sendMessage(prefix + "Plugin: " + plugin.getDescription().getName());
        source.getSender().sendMessage(prefix + "Version: " + plugin.getDescription().getVersion());
        source.getSender().sendMessage(prefix + "Authors: " + String.join(", ", plugin.getDescription().getAuthors()));
        source.getSender().sendMessage(prefix + "");
        source.getSender().sendMessage(prefix + "Server: " + Bukkit.getName() + " " + Bukkit.getVersion());
        source.getSender().sendMessage(prefix + "Minecraft: " + Bukkit.getMinecraftVersion());
        source.getSender().sendMessage(prefix + "Java: " + System.getProperty("java.version"));
        source.getSender().sendMessage(prefix + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        source.getSender().sendMessage(prefix + "");
        source.getSender().sendMessage(prefix + "Active Optimizations:");
        source.getSender().sendMessage(prefix + "  Entity Tracker: " + (plugin.getConfigManager().isEntityTrackerEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Mob Spawning: " + (plugin.getConfigManager().isMobSpawningEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Entity Tick Parallel: " + (plugin.getConfigManager().isEntityTickParallel() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Block Entity Parallel: " + (plugin.getConfigManager().isBlockEntityParallelTickEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Async Lighting: " + (plugin.getConfigManager().isAsyncLightingEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Chunk Tick Async: " + (plugin.getConfigManager().isChunkTickAsyncEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Brain Throttle: " + (plugin.getConfigManager().isBrainThrottleEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  TNT Optimization: " + (plugin.getConfigManager().isTNTOptimizationEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Universal AI: " + (plugin.getConfigManager().isUniversalAiOptimizationEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  BeeFix: " + (plugin.getConfigManager().isBeeFixEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  Structure Location Async: " + (plugin.getConfigManager().isStructureLocationAsyncEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "  SecureSeed: " + (plugin.getConfigManager().isSecureSeedEnabled() ? "ON" : "OFF"));
        source.getSender().sendMessage(prefix + "========================================");
    }
    @Override
    public @Nullable String permission() {
        return "akiasync.version";
    }
}

