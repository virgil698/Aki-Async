package com.akiasync;

import com.akiasync.lag.LagCommand;
import com.akiasync.lag.LagProfilerService;
import com.akiasync.mixin.BridgeManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class AkiAsyncPlugin extends JavaPlugin {
    private final LagProfilerService lagProfiler = new LagProfilerService();
    private final AkiAsyncBridge bridge = new AkiAsyncBridge(lagProfiler);

    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "akiasync",
                "Aki-Async lag source visualizer",
                new LagCommand(lagProfiler, bridge)
        ));
        BridgeManager.INSTANCE.install(bridge);
        getLogger().info("Lag source profiler enabled. Use /akiasync lag status.");
    }

    @Override
    public void onDisable() {
        BridgeManager.INSTANCE.uninstall(bridge);
        lagProfiler.clear();
    }
}
