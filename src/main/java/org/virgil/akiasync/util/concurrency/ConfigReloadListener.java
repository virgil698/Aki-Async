package org.virgil.akiasync.util.concurrency;

import org.virgil.akiasync.config.ConfigManager;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface ConfigReloadListener {

    void onConfigReload(@Nonnull ConfigManager newConfig);
}
