package com.example.plugin;

import com.example.plugin.mixin.BridgeManager;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class TemplatePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // TODO: remove or change this. this is only for example
        BridgeManager.INSTANCE.setBridge(new MyBridge());
        getLogger().info("Already inject mixin bridge.");
        // TODO: your plugin startup logic
    }

    @Override
    public void onDisable() {
        // TODO: your plugin shutdown logic
    }
}
