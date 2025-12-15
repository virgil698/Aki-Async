package org.virgil.akiasync.mixin.mixins.datapack;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(targets = "net.minecraft.commands.functions.FunctionBuilder", remap = false)
public abstract class CommandFunctionDeduplicatorMixin {
    
    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;
    private static final Object2ObjectOpenHashMap<String, UnboundEntryAction<?>> actionCache = new Object2ObjectOpenHashMap<>();
    private static int totalCommands = 0;
    private static long savedMemoryBytes = 0L;
    private static long usedMemoryBytes = 0L;
    
    @ModifyVariable(
        method = "addCommand(Lnet/minecraft/commands/execution/UnboundEntryAction;)V",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> deduplicateCommand(UnboundEntryAction<T> command) {
        
        if (!initialized) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                enabled = bridge.isCommandDeduplicationEnabled();
                
                if (enabled && bridge.isDebugLoggingEnabled() && bridge.isCommandDeduplicationDebugEnabled()) {
                    bridge.debugLog("[AkiAsync] Command deduplication enabled");
                }
                
                initialized = true;
            }
        }
        
        if (!enabled) return command;
        
        if (command == null) return command;
        
        String commandString = command.toString();
        
        synchronized (actionCache) {
            totalCommands++;
            
            @SuppressWarnings("unchecked")
            UnboundEntryAction<T> cachedAction = (UnboundEntryAction<T>) actionCache.get(commandString);
            
            if (cachedAction != null) {
                
                savedMemoryBytes += estimateObjectSize(commandString);
                
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled() && bridge.isCommandDeduplicationDebugEnabled() && totalCommands % 1000 == 0) {
                    logStatistics();
                }
                
                return cachedAction;
            } else {
                
                usedMemoryBytes += estimateObjectSize(commandString);
                actionCache.put(commandString, command);
                return command;
            }
        }
    }
    
    private static long estimateObjectSize(String commandString) {
        
        return commandString.length() * 2L + 24L;
    }
    
    
    private static void clearCache() {
        synchronized (actionCache) {
            if (totalCommands > 0) {
                logStatistics();
            }
            
            actionCache.clear();
            actionCache.trim();
            totalCommands = 0;
            savedMemoryBytes = 0L;
            usedMemoryBytes = 0L;
        }
    }
    
    private static void logStatistics() {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            int uniqueCommands = actionCache.size();
            int duplicateCommands = totalCommands - uniqueCommands;
            double deduplicationRate = totalCommands > 0 ? (duplicateCommands * 100.0 / totalCommands) : 0.0;
            double savedMemoryMB = savedMemoryBytes / (1024.0 * 1024.0);
            double totalMemoryMB = (savedMemoryBytes + usedMemoryBytes) / (1024.0 * 1024.0);
            
            bridge.debugLog("[AkiAsync] Command Deduplication Statistics:");
            bridge.debugLog("  - Total commands: %d", totalCommands);
            bridge.debugLog("  - Unique commands: %d", uniqueCommands);
            bridge.debugLog("  - Duplicate commands: %d (%.2f%%)", duplicateCommands, deduplicationRate);
            bridge.debugLog("  - Memory saved: %.2f MB / %.2f MB (%.2f%%)", 
                savedMemoryMB, totalMemoryMB, 
                totalMemoryMB > 0 ? (savedMemoryMB * 100.0 / totalMemoryMB) : 0.0);
        }
    }
    
    private static String getStatistics() {
        synchronized (actionCache) {
            int uniqueCommands = actionCache.size();
            int duplicateCommands = totalCommands - uniqueCommands;
            double deduplicationRate = totalCommands > 0 ? (duplicateCommands * 100.0 / totalCommands) : 0.0;
            double savedMemoryMB = savedMemoryBytes / (1024.0 * 1024.0);
            
            return String.format(
                "Commands: %d total, %d unique, %d duplicates (%.2f%%), Memory saved: %.2f MB",
                totalCommands, uniqueCommands, duplicateCommands, deduplicationRate, savedMemoryMB
            );
        }
    }
}
