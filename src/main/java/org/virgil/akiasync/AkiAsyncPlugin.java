package org.virgil.akiasync;

import org.bukkit.plugin.java.JavaPlugin;
import org.virgil.akiasync.bootstrap.PluginBootstrapper;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.cache.CacheManager;
import org.virgil.akiasync.chunk.ChunkLoadPriorityScheduler;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.executor.AsyncExecutorManager;
import org.virgil.akiasync.language.LanguageManager;

@SuppressWarnings("unused")
public final class AkiAsyncPlugin extends JavaPlugin {

    private static AkiAsyncPlugin instance;
    private PluginBootstrapper bootstrapper;

    @SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Override
    public void onEnable() {
        synchronized (AkiAsyncPlugin.class) {
            instance = this;
        }

        bootstrapper = new PluginBootstrapper(this);
        bootstrapper.bootstrap();
    }

    @SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Override
    public void onDisable() {
        if (bootstrapper != null) {
            bootstrapper.shutdown();
        }

        synchronized (AkiAsyncPlugin.class) {
            instance = null;
        }
    }

    @SuppressWarnings("MS_EXPOSE_REP")
    public static AkiAsyncPlugin getInstance() {
        return instance;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public ConfigManager getConfigManager() {
        return bootstrapper != null ? bootstrapper.getConfigManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public AsyncExecutorManager getExecutorManager() {
        return bootstrapper != null ? bootstrapper.getExecutorManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public CacheManager getCacheManager() {
        return bootstrapper != null ? bootstrapper.getCacheManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public AkiAsyncBridge getBridge() {
        return bootstrapper != null ? bootstrapper.getBridge() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public org.virgil.akiasync.throttling.EntityThrottlingManager getThrottlingManager() {
        return bootstrapper != null ? bootstrapper.getThrottlingManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public ChunkLoadPriorityScheduler getChunkLoadScheduler() {
        return bootstrapper != null ? bootstrapper.getChunkLoadScheduler() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public org.virgil.akiasync.compat.VirtualEntityCompatManager getVirtualEntityCompatManager() {
        return bootstrapper != null ? bootstrapper.getVirtualEntityCompatManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public org.virgil.akiasync.crypto.QuantumSeedManager getQuantumSeedManager() {
        return bootstrapper != null ? bootstrapper.getQuantumSeedManager() : null;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public LanguageManager getLanguageManager() {
        return bootstrapper != null ? bootstrapper.getLanguageManager() : null;
    }

    public void restartMetricsScheduler() {
        if (bootstrapper != null) {
            bootstrapper.restartMetricsScheduler();
        }
    }

    public void stopMetricsScheduler() {
        if (bootstrapper != null) {
            bootstrapper.stopMetricsScheduler();
        }
    }
}
