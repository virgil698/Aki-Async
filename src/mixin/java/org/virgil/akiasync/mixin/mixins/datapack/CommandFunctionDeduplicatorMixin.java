package org.virgil.akiasync.mixin.mixins.datapack;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(targets = "net.minecraft.commands.functions.FunctionBuilder", remap = false)
public abstract class CommandFunctionDeduplicatorMixin {
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    @Unique
    private static volatile boolean akiasync$enabled = false;
    @Unique
    private static final Object2ObjectOpenHashMap<String, UnboundEntryAction<?>> akiasync$actionCache = new Object2ObjectOpenHashMap<>();
    @Unique
    private static int akiasync$totalCommands = 0;
    @Unique
    private static long akiasync$savedMemoryBytes = 0L;
    @Unique
    private static long akiasync$usedMemoryBytes = 0L;
    
    @ModifyVariable(
        method = "addCommand(Lnet/minecraft/commands/execution/UnboundEntryAction;)V",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> deduplicateCommand(UnboundEntryAction<T> command) {
        
        if (!akiasync$initialized) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isCommandDeduplicationEnabled();
                
                if (akiasync$enabled && bridge.isDebugLoggingEnabled() && bridge.isCommandDeduplicationDebugEnabled()) {
                    bridge.debugLog("[AkiAsync] Command deduplication enabled");
                
                    akiasync$initialized = true;
                }
            }
        }
        
        if (!akiasync$enabled) return command;
        
        if (command == null) return command;
        
        String commandString = command.toString();
        
        synchronized (akiasync$actionCache) {
            akiasync$totalCommands++;
            
            @SuppressWarnings("unchecked")
            UnboundEntryAction<T> cachedAction = (UnboundEntryAction<T>) akiasync$actionCache.get(commandString);
            
            if (cachedAction != null) {
                
                akiasync$savedMemoryBytes += akiasync$estimateObjectSize(commandString);
                
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled() && bridge.isCommandDeduplicationDebugEnabled() && akiasync$totalCommands % 1000 == 0) {
                    akiasync$logStatistics();
                }
                
                return cachedAction;
            } else {
                
                akiasync$usedMemoryBytes += akiasync$estimateObjectSize(commandString);
                akiasync$actionCache.put(commandString, command);
                return command;
            }
        }
    }
    
    @Unique
    private static long akiasync$estimateObjectSize(String commandString) {
        
        return commandString.length() * 2L + 24L;
    }
    
    @Unique
    private static void akiasync$clearCache() {
        synchronized (akiasync$actionCache) {
            if (akiasync$totalCommands > 0) {
                akiasync$logStatistics();
            }
            
            akiasync$actionCache.clear();
            akiasync$actionCache.trim();
            akiasync$totalCommands = 0;
            akiasync$savedMemoryBytes = 0L;
            akiasync$usedMemoryBytes = 0L;
        }
    }
    
    @Unique
    private static void akiasync$logStatistics() {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            int uniqueCommands = akiasync$actionCache.size();
            int duplicateCommands = akiasync$totalCommands - uniqueCommands;
            double deduplicationRate = akiasync$totalCommands > 0 ? (duplicateCommands * 100.0 / akiasync$totalCommands) : 0.0;
            double savedMemoryMB = akiasync$savedMemoryBytes / (1024.0 * 1024.0);
            double totalMemoryMB = (akiasync$savedMemoryBytes + akiasync$usedMemoryBytes) / (1024.0 * 1024.0);
            
            bridge.debugLog("[AkiAsync] Command Deduplication Statistics:");
            bridge.debugLog("  - Total commands: %d", akiasync$totalCommands);
            bridge.debugLog("  - Unique commands: %d", uniqueCommands);
            bridge.debugLog("  - Duplicate commands: %d (%.2f%%)", duplicateCommands, deduplicationRate);
            bridge.debugLog("  - Memory saved: %.2f MB / %.2f MB (%.2f%%)", 
                savedMemoryMB, totalMemoryMB, 
                totalMemoryMB > 0 ? (savedMemoryMB * 100.0 / totalMemoryMB) : 0.0);
        }
    }
    
    @Unique
    private static String akiasync$getStatistics() {
        synchronized (akiasync$actionCache) {
            int uniqueCommands = akiasync$actionCache.size();
            int duplicateCommands = akiasync$totalCommands - uniqueCommands;
            double deduplicationRate = akiasync$totalCommands > 0 ? (duplicateCommands * 100.0 / akiasync$totalCommands) : 0.0;
            double savedMemoryMB = akiasync$savedMemoryBytes / (1024.0 * 1024.0);
            
            return String.format(
                "Commands: %d total, %d unique, %d duplicates (%.2f%%), Memory saved: %.2f MB",
                akiasync$totalCommands, uniqueCommands, duplicateCommands, deduplicationRate, savedMemoryMB
            );
        }
    }
}
