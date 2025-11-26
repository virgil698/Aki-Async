package org.virgil.akiasync.constants;

public final class AkiAsyncConstants {

    private AkiAsyncConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final class Threading {
        public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;
        public static final long THREAD_KEEP_ALIVE_TIME = 60L;
        public static final int EXECUTOR_TEST_TIMEOUT_MS = 100;
        public static final int EXECUTOR_SHUTDOWN_TIMEOUT_MS = 1000;
        public static final int BATCH_DELAY_MS = 100;
    }

    public static final class Logging {
        public static final String LOG_PREFIX = "[AkiAsync]";
        public static final String DEBUG_PREFIX = "[AkiAsync-Debug]";
        public static final String TNT_PREFIX = "[AkiAsync-TNT]";
        public static final String ERROR_PREFIX = "[AkiAsync-Error]";
    }

    public static final class Folia {
        public static final String REGIONIZED_SERVER_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";
        public static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
        public static final String IS_OWNED_METHOD = "isOwnedByCurrentRegion";
        public static final String QUEUE_METHOD = "queueTickTaskQueue";
    }
}
