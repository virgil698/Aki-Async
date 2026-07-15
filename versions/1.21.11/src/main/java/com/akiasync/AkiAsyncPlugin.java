package com.akiasync;

import com.akiasync.datapack.DataPackService;
import com.akiasync.lag.LagCommand;
import com.akiasync.lag.LagProfilerService;
import com.akiasync.mixin.BridgeManager;
import com.akiasync.scheduler.AkiScheduler;
import com.akiasync.scheduler.SchedulerConfig;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class AkiAsyncPlugin extends JavaPlugin {
    private final LagProfilerService lagProfiler = new LagProfilerService();
    private final DataPackService dataPackService = new DataPackService();
    private final AkiAsyncBridge bridge = new AkiAsyncBridge(lagProfiler, dataPackService);
    private AkiScheduler scheduler;

    @Override
    public void onEnable() {
        scheduler = new AkiScheduler(this, SchedulerConfig.defaults());
        scheduler.start();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "akiasync",
                "Aki-Async performance toolkit",
                new LagCommand(lagProfiler, dataPackService, bridge, scheduler)
        ));
        BridgeManager.INSTANCE.install(bridge);
        getLogger().info("Aki-Async enabled. Use /akiasync lag status or /akiasync scheduler status.");
    }

    @Override
    public void onDisable() {
        BridgeManager.INSTANCE.uninstall(bridge);
        if (scheduler != null) {
            scheduler.close();
            scheduler = null;
        }
        lagProfiler.clear();
        dataPackService.clear();
    }
}
