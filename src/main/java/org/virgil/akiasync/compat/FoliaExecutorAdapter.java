package org.virgil.akiasync.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FoliaExecutorAdapter implements ExecutorService {
    
    private final Plugin plugin;
    private final ExecutorService fallbackExecutor;
    private final boolean isFolia;
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    
    private static Method globalScheduleMethod;
    private static Method regionScheduleMethod;
    private static Object globalScheduler;
    private static Object regionScheduler;
    
    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            
            Object server = Bukkit.getServer();
            globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
            regionScheduler = server.getClass().getMethod("getRegionScheduler").invoke(server);
            
            globalScheduleMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            regionScheduleMethod = regionScheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
            
        } catch (Exception e) {
        }
    }
    
    public FoliaExecutorAdapter(Plugin plugin, int threads, String poolName) {
        this.plugin = plugin;
        this.isFolia = FoliaSchedulerAdapter.isFolia();
        
        if (!isFolia) {
            this.fallbackExecutor = new ThreadPoolExecutor(
                threads, threads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                r -> {
                    Thread t = new Thread(r, poolName + "-" + taskCounter.incrementAndGet());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        } else {
            this.fallbackExecutor = null;
        }
    }
    
    public void executeEntityTask(Entity entity, Runnable task) {
        if (isFolia) {
            FoliaSchedulerAdapter.runEntityTask(plugin, entity, task);
        } else {
            execute(task);
        }
    }
    
    public void executeLocationTask(Location location, Runnable task) {
        if (isFolia) {
            FoliaSchedulerAdapter.runLocationTask(plugin, location, task);
        } else {
            execute(task);
        }
    }
    
    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException("Command cannot be null");
        }
        
        if (isFolia) {
            try {
                if (globalScheduler != null && globalScheduleMethod != null) {
                    globalScheduleMethod.invoke(globalScheduler, plugin, command);
                } else {
                    plugin.getLogger().warning("[FoliaExecutorAdapter] Folia scheduler not available, running synchronously");
                    command.run();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[FoliaExecutorAdapter] Failed to schedule global task: " + e.getMessage());
                try {
                    plugin.getServer().getScheduler().runTask(plugin, command);
                } catch (Exception fallbackException) {
                    plugin.getLogger().severe("[FoliaExecutorAdapter] Fallback execution also failed: " + fallbackException.getMessage());
                    command.run();
                }
            }
        } else {
            if (fallbackExecutor != null && !fallbackExecutor.isShutdown()) {
                try {
                    fallbackExecutor.execute(command);
                } catch (Exception e) {
                    plugin.getLogger().warning("[FoliaExecutorAdapter] ThreadPool execution failed: " + e.getMessage());
                    command.run();
                }
            } else {
                command.run();
            }
        }
    }
    
    @Override
    public void shutdown() {
        if (fallbackExecutor != null) {
            fallbackExecutor.shutdown();
        }
    }
    
    @Override
    public java.util.List<Runnable> shutdownNow() {
        if (fallbackExecutor != null) {
            return fallbackExecutor.shutdownNow();
        }
        return java.util.Collections.emptyList();
    }
    
    @Override
    public boolean isShutdown() {
        return fallbackExecutor != null && fallbackExecutor.isShutdown();
    }
    
    @Override
    public boolean isTerminated() {
        return fallbackExecutor != null && fallbackExecutor.isTerminated();
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (fallbackExecutor != null) {
            return fallbackExecutor.awaitTermination(timeout, unit);
        }
        return true;
    }
    
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (isFolia) {
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return new FutureAdapter<>(future);
        } else {
            return fallbackExecutor.submit(task);
        }
    }
    
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (isFolia) {
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    task.run();
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return new FutureAdapter<>(future);
        } else {
            return fallbackExecutor.submit(task, result);
        }
    }
    
    @Override
    public Future<?> submit(Runnable task) {
        if (isFolia) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return new FutureAdapter<>(future);
        } else {
            return fallbackExecutor.submit(task);
        }
    }
    
    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (isFolia) {
            java.util.List<Future<T>> results = new java.util.ArrayList<>();
            for (Callable<T> task : tasks) {
                results.add(submit(task));
            }
            return results;
        } else {
            return fallbackExecutor.invokeAll(tasks);
        }
    }
    
    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (isFolia) {
            return invokeAll(tasks);
        } else {
            return fallbackExecutor.invokeAll(tasks, timeout, unit);
        }
    }
    
    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (isFolia) {
            if (tasks.isEmpty()) {
                throw new IllegalArgumentException("Task collection cannot be empty");
            }
            
            Callable<T> firstTask = tasks.iterator().next();
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    future.complete(firstTask.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            
            try {
                return future.get();
            } catch (ExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        } else {
            if (fallbackExecutor != null) {
                return fallbackExecutor.invokeAny(tasks);
            } else {
                throw new IllegalStateException("Fallback executor is null");
            }
        }
    }
    
    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isFolia) {
            if (tasks.isEmpty()) {
                throw new IllegalArgumentException("Task collection cannot be empty");
            }
            
            Callable<T> firstTask = tasks.iterator().next();
            CompletableFuture<T> future = new CompletableFuture<>();
            execute(() -> {
                try {
                    future.complete(firstTask.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            
            try {
                return future.get(timeout, unit);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw e;
            } catch (ExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        } else {
            if (fallbackExecutor != null) {
                return fallbackExecutor.invokeAny(tasks, timeout, unit);
            } else {
                throw new IllegalStateException("Fallback executor is null");
            }
        }
    }
    
    public boolean isFoliaMode() {
        return isFolia;
    }
    
    public boolean isHealthy() {
        if (isFolia) {
            return globalScheduler != null && globalScheduleMethod != null;
        } else {
            return fallbackExecutor != null && !fallbackExecutor.isShutdown();
        }
    }
    
    public String getStatus() {
        if (isFolia) {
            return "Folia mode - Global scheduler: " + (globalScheduler != null ? "available" : "unavailable");
        } else {
            if (fallbackExecutor == null) {
                return "ThreadPool mode - Executor: null";
            } else if (fallbackExecutor.isShutdown()) {
                return "ThreadPool mode - Executor: shutdown";
            } else if (fallbackExecutor.isTerminated()) {
                return "ThreadPool mode - Executor: terminated";
            } else {
                return "ThreadPool mode - Executor: active";
            }
        }
    }
    
    private static class FutureAdapter<T> implements Future<T> {
        private final CompletableFuture<T> future;
        
        FutureAdapter(CompletableFuture<T> future) {
            this.future = future;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }
        
        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }
        
        @Override
        public boolean isDone() {
            return future.isDone();
        }
        
        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }
        
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }
}
