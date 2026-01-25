package org.virgil.akiasync.mixin.optimization.thread;

import java.util.concurrent.ThreadFactory;

public abstract class VirtualThreadService {

    public abstract ThreadFactory createFactory();

    public abstract Thread start(Runnable task);

    protected void runTest() throws Throwable {
        try {
            this.start(() -> {}).join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            var thread = this.createFactory().newThread(() -> {});
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean initialized = false;
    private static VirtualThreadService implementation;

    public static boolean isSupported() {
        return get() != null;
    }

    public static VirtualThreadService get() {
        if (!initialized) {
            initialized = true;
            try {
                implementation = DirectVirtualThreadService.create();
            } catch (Throwable e) {

                try {
                    implementation = ReflectionVirtualThreadService.create();
                } catch (Throwable e2) {

                }
            }
        }
        return implementation;
    }

    public static int getJavaMajorVersion() {
        var version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return version.charAt(2) - '0';
        }
        int dotIndex = version.indexOf(".");
        return Integer.parseInt(dotIndex == -1 ? version : version.substring(0, dotIndex));
    }

    public static class DirectVirtualThreadService extends VirtualThreadService {

        public static DirectVirtualThreadService create() throws Throwable {
            var factory = Thread.ofVirtual().factory();
            var thread = factory.newThread(() -> {});
            thread.start();
            thread.join();

            return new DirectVirtualThreadService();
        }

        @Override
        public ThreadFactory createFactory() {
            return Thread.ofVirtual().factory();
        }

        @Override
        public Thread start(Runnable task) {
            return Thread.ofVirtual().start(task);
        }
    }

    public static class ReflectionVirtualThreadService extends VirtualThreadService {

        private final java.lang.reflect.Method ofVirtualMethod;
        private final java.lang.reflect.Method factoryMethod;
        private final java.lang.reflect.Method startMethod;

        private ReflectionVirtualThreadService() throws Throwable {
            Class<?> threadClass = Thread.class;
            this.ofVirtualMethod = threadClass.getMethod("ofVirtual");

            Object builder = ofVirtualMethod.invoke(null);
            Class<?> builderClass = builder.getClass();

            this.factoryMethod = builderClass.getMethod("factory");
            this.startMethod = builderClass.getMethod("start", Runnable.class);

            runTest();
        }

        public static ReflectionVirtualThreadService create() throws Throwable {
            return new ReflectionVirtualThreadService();
        }

        @Override
        public ThreadFactory createFactory() {
            try {
                Object builder = ofVirtualMethod.invoke(null);
                return (ThreadFactory) factoryMethod.invoke(builder);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create virtual thread factory", e);
            }
        }

        @Override
        public Thread start(Runnable task) {
            try {
                Object builder = ofVirtualMethod.invoke(null);
                return (Thread) startMethod.invoke(builder, task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start virtual thread", e);
            }
        }
    }

    public static class VirtualThreadExecutor implements java.util.concurrent.Executor {

        private final VirtualThreadService service;

        public VirtualThreadExecutor(VirtualThreadService service) {
            this.service = service;
        }

        @Override
        public void execute(Runnable command) {
            service.start(command);
        }

        public static VirtualThreadExecutor create() {
            VirtualThreadService service = VirtualThreadService.get();
            if (service != null) {
                return new VirtualThreadExecutor(service);
            }
            return null;
        }
    }
}
