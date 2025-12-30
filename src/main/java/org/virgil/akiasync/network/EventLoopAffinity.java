package org.virgil.akiasync.network;

import io.netty.channel.EventLoop;
import org.bukkit.Bukkit;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EventLoopAffinity {
    
    private static boolean strictChecking = false;
    private static boolean debugMode = false;
    
    public static void initialize(boolean enableStrictChecking, boolean enableDebug) {
        strictChecking = enableStrictChecking;
        debugMode = enableDebug;
        
        if (debugMode) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[EventLoopAffinity] Initialized - strict=%s, debug=%s", 
                    strictChecking, debugMode);
            }
        }
    }
    
    public static boolean isInEventLoop(EventLoop eventLoop) {
        return eventLoop != null && eventLoop.inEventLoop();
    }
    
    public static void ensureInEventLoop(EventLoop eventLoop, String context) {
        if (strictChecking && eventLoop != null && !eventLoop.inEventLoop()) {
            String error = String.format(
                "[EventLoopAffinity] Operation '%s' must be called from event loop, but called from %s",
                context, Thread.currentThread().getName()
            );
            
            if (debugMode) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog(error);
                }
            }
            
            throw new IllegalStateException(error);
        }
    }
    
    public static void executeInEventLoop(EventLoop eventLoop, Runnable task) {
        if (eventLoop == null) {
            executeOnMainThread(task);
            return;
        }
        
        if (eventLoop.inEventLoop()) {
            task.run();
        } else {
            eventLoop.execute(task);
        }
    }
    
    public static <T> CompletableFuture<T> supplyInEventLoop(EventLoop eventLoop, Supplier<T> supplier) {
        if (eventLoop == null) {
            return supplyOnMainThread(supplier);
        }
        
        if (eventLoop.inEventLoop()) {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Exception e) {
                CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        } else {
            CompletableFuture<T> future = new CompletableFuture<>();
            eventLoop.execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }
    }
    
    public static <T> void supplyInEventLoopWithCallback(EventLoop eventLoop, 
                                                         Supplier<T> supplier,
                                                         Consumer<T> callback) {
        if (eventLoop == null) {
            executeOnMainThread(() -> {
                T result = supplier.get();
                callback.accept(result);
            });
            return;
        }
        
        if (eventLoop.inEventLoop()) {
            try {
                T result = supplier.get();
                callback.accept(result);
            } catch (Exception e) {
                logError("Error in EventLoop task", e);
            }
        } else {
            eventLoop.execute(() -> {
                try {
                    T result = supplier.get();
                    callback.accept(result);
                } catch (Exception e) {
                    logError("Error in EventLoop task", e);
                }
            });
        }
    }
    
    public static void scheduleInEventLoop(EventLoop eventLoop, Runnable task, long delayTicks) {
        if (eventLoop == null) {
            try {
                org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("AkiAsync");
                if (plugin != null && plugin.isEnabled()) {
                    Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
                } else {
                    task.run();
                }
            } catch (Exception e) {
                logError("Failed to schedule task, executing directly", e);
                task.run();
            }
            return;
        }
        
        long delayMs = delayTicks * 50L;
        eventLoop.schedule(task, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    private static void executeOnMainThread(Runnable task) {
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("AkiAsync");
            if (plugin != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, task);
            } else {
                task.run();
            }
        } catch (Exception e) {
            logError("Failed to execute task on main thread, executing directly", e);
            task.run();
        }
    }
    
    private static <T> CompletableFuture<T> supplyOnMainThread(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("AkiAsync");
            if (plugin != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {
                        try {
                            future.complete(supplier.get());
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }
                );
            } else {
                try {
                    future.complete(supplier.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
    
    private static void logError(String message, Exception e) {
        if (debugMode) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog("[EventLoopAffinity] %s: %s", message, e.getMessage());
            }
        }
    }
    
    public static Executor asExecutor(EventLoop eventLoop) {
        if (eventLoop == null) {
            return task -> executeOnMainThread(task);
        }
        return eventLoop;
    }
}
