package org.virgil.akiasync.mixin.util;

import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ThreadPoolExecutor;

public class AsyncBlockTickExecutor extends ThreadPoolExecutor {
    public AsyncBlockTickExecutor(ServerLevel level) {
        super(
                2,
                4,
                30L, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(1024),
                new java.util.concurrent.ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "AkiAsync-BlockTick");
                        t.setDaemon(true);
                        return t;
                    }
                },
                (r, executor) -> {
                    if (level != null) level.getServer().execute(r);
                }
        );
    }
}
