package org.virgil.akiasync.command;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.language.LanguageManager;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

@NullMarked
public class VersionCommand implements BasicCommand {

    private final AkiAsyncPlugin plugin;

    public VersionCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    private String onOff(boolean value) {
        return value ? lang().getRaw("common.on") : lang().getRaw("common.off");
    }

    private void sendOptimization(CommandSourceStack source, String name, String status) {
        source.getSender().sendMessage(lang().get("command.version.optimization-item", "name", name, "status", status));
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        source.getSender().sendMessage(lang().get("command.version.header"));
        source.getSender().sendMessage(lang().get("command.version.plugin", "value", plugin.getDescription().getName()));
        source.getSender().sendMessage(lang().get("command.version.version", "value", plugin.getDescription().getVersion()));
        source.getSender().sendMessage(lang().get("command.version.authors", "value", String.join(", ", plugin.getDescription().getAuthors())));
        source.getSender().sendMessage(lang().get("command.version.config-version", "value", String.valueOf(plugin.getConfigManager().getCurrentConfigVersion())));
        source.getSender().sendMessage(net.kyori.adventure.text.Component.empty());
        source.getSender().sendMessage(lang().get("command.version.server", "value", Bukkit.getName() + " " + Bukkit.getVersion()));
        source.getSender().sendMessage(lang().get("command.version.minecraft", "value", Bukkit.getMinecraftVersion()));
        source.getSender().sendMessage(lang().get("command.version.java", "value", System.getProperty("java.version")));
        source.getSender().sendMessage(lang().get("command.version.os", "value", System.getProperty("os.name") + " " + System.getProperty("os.version")));
        source.getSender().sendMessage(net.kyori.adventure.text.Component.empty());
        source.getSender().sendMessage(lang().get("command.version.active-optimizations"));
        sendOptimization(source, "Multithreaded Entity Tracker", onOff(plugin.getConfigManager().isMultithreadedEntityTrackerEnabled()));
        sendOptimization(source, "Mob Spawning", onOff(plugin.getConfigManager().isMobSpawningEnabled()));
        sendOptimization(source, "Entity Tick Parallel", onOff(plugin.getConfigManager().isEntityTickParallel()));
        sendOptimization(source, "Block Entity Parallel", onOff(plugin.getConfigManager().isBlockEntityParallelTickEnabled()));
        sendOptimization(source, "Async Lighting", onOff(plugin.getConfigManager().isAsyncLightingEnabled()));
        sendOptimization(source, "Async Pathfinding", onOff(plugin.getConfigManager().isAsyncPathfindingEnabled()));
        sendOptimization(source, "Chunk Tick Async", onOff(plugin.getConfigManager().isChunkTickAsyncEnabled()));
        sendOptimization(source, "Brain Throttle", onOff(plugin.getConfigManager().isBrainThrottleEnabled()));
        sendOptimization(source, "TNT Optimization", onOff(plugin.getConfigManager().isTNTOptimizationEnabled()));
        sendOptimization(source, "BeeFix", onOff(plugin.getConfigManager().isBeeFixEnabled()));
        sendOptimization(source, "Structure Location Async", onOff(plugin.getConfigManager().isStructureLocationAsyncEnabled()));

        if (plugin.getConfigManager().isSeedEncryptionEnabled()) {
            String scheme = plugin.getConfigManager().getSeedEncryptionScheme();
            if ("quantum".equalsIgnoreCase(scheme)) {
                sendOptimization(source, "Seed Encryption", "QuantumSeed (Level " + plugin.getConfigManager().getQuantumSeedEncryptionLevel() + ")");
            } else {
                sendOptimization(source, "Seed Encryption", "SecureSeed (" + plugin.getConfigManager().getSecureSeedBits() + " bits)");
            }
        } else {
            sendOptimization(source, "Seed Encryption", onOff(false));
        }
        sendOptimization(source, "Falling Block Parallel", onOff(plugin.getConfigManager().isFallingBlockParallelEnabled()));
        sendOptimization(source, "Item Entity Smart Merge", onOff(plugin.getConfigManager().isItemEntityMergeOptimizationEnabled()));
        sendOptimization(source, "Item Entity Age Optimization", onOff(plugin.getConfigManager().isItemEntityAgeOptimizationEnabled()));
        sendOptimization(source, "Item Entity Inactive Tick", onOff(plugin.getConfigManager().isItemEntityInactiveTickEnabled()));
        sendOptimization(source, "Minecart Cauldron Destruction", onOff(plugin.getConfigManager().isMinecartCauldronDestructionEnabled()));
        sendOptimization(source, "Fast Movement Chunk Load", onOff(plugin.getConfigManager().isFastMovementChunkLoadEnabled()));
        sendOptimization(source, "Center Offset Loading", onOff(plugin.getConfigManager().isCenterOffsetEnabled()));
        source.getSender().sendMessage(lang().get("command.version.header"));
    }
    @Override
    public @Nullable String permission() {
        return "akiasync.version";
    }
}
