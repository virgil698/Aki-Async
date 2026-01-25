package org.virgil.akiasync.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FastThreadLocalThread;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MultiNettyEventLoopManager {

    private static final Logger LOGGER = Logger.getLogger("AkiAsync-MultiNetty");

    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;

    private static NioEventLoopGroup nioLoginGroup;
    private static NioEventLoopGroup nioPlayGroup;
    private static EpollEventLoopGroup epollLoginGroup;
    private static EpollEventLoopGroup epollPlayGroup;

    private static final java.util.concurrent.ConcurrentHashMap<Channel, ReregistrationState> reregistrationStates =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static class ReregistrationState {
        volatile boolean isReregistering = false;
        volatile EventLoopGroup pendingReregistration = null;
    }

    public static synchronized void initialize(boolean enable) {
        if (initialized) {
            return;
        }

        enabled = enable;

        if (enabled) {
            try {

                nioLoginGroup = new NioEventLoopGroup(2, createThreadFactory("AkiAsync-Netty-Login-IO"));
                nioPlayGroup = new NioEventLoopGroup(0, createThreadFactory("AkiAsync-Netty-Play-IO"));

                try {
                    epollLoginGroup = new EpollEventLoopGroup(2, createThreadFactory("AkiAsync-Netty-Epoll-Login-IO"));
                    epollPlayGroup = new EpollEventLoopGroup(0, createThreadFactory("AkiAsync-Netty-Epoll-Play-IO"));
                } catch (Throwable t) {

                    epollLoginGroup = null;
                    epollPlayGroup = null;
                }

                LOGGER.info("[MultiNettyEventLoop] Initialized: nioLogin=2 threads, nioPlay=" +
                    Runtime.getRuntime().availableProcessors() + " threads");
            } catch (Exception e) {
                enabled = false;
                LOGGER.warning("[MultiNettyEventLoop] Failed to initialize: " + e.getMessage());
            }
        }

        initialized = true;
    }

    public static void shutdown() {
        if (nioLoginGroup != null) {
            nioLoginGroup.shutdownGracefully();
        }
        if (nioPlayGroup != null) {
            nioPlayGroup.shutdownGracefully();
        }
        if (epollLoginGroup != null) {
            epollLoginGroup.shutdownGracefully();
        }
        if (epollPlayGroup != null) {
            epollPlayGroup.shutdownGracefully();
        }
        reregistrationStates.clear();
        initialized = false;
    }

    public static void handleProtocolChange(Connection connection, int protocolOrdinal) {
        if (!enabled || connection == null) {
            return;
        }

        try {
            Channel channel = connection.channel;
            if (channel == null || !channel.isActive()) {
                return;
            }

            ConnectionProtocol protocol = ConnectionProtocol.values()[protocolOrdinal];
            EventLoopGroup targetGroup = getEventLoopGroup(channel, protocol);

            if (targetGroup != null && channel.eventLoop().parent() != targetGroup) {
                reregister(channel, targetGroup);
            }
        } catch (Exception e) {
            LOGGER.warning("[MultiNettyEventLoop] Error handling protocol change: " + e.getMessage());
        }
    }

    private static EventLoopGroup getEventLoopGroup(Channel channel, ConnectionProtocol protocol) {
        if (channel instanceof NioSocketChannel) {
            if (protocol == ConnectionProtocol.LOGIN || protocol == ConnectionProtocol.CONFIGURATION) {
                return nioLoginGroup;
            } else if (protocol == ConnectionProtocol.PLAY) {
                return nioPlayGroup;
            }
        } else if (channel instanceof EpollSocketChannel) {
            if (protocol == ConnectionProtocol.LOGIN || protocol == ConnectionProtocol.CONFIGURATION) {
                return epollLoginGroup;
            } else if (protocol == ConnectionProtocol.PLAY) {
                return epollPlayGroup;
            }
        }
        return null;
    }

    private static void reregister(Channel channel, EventLoopGroup group) {
        ReregistrationState state = reregistrationStates.computeIfAbsent(channel, k -> new ReregistrationState());

        synchronized (state) {
            if (state.isReregistering) {
                state.pendingReregistration = group;
                return;
            }

            if (!channel.isActive()) {
                return;
            }

            try {
                ChannelPromise promise = channel.newPromise();
                channel.config().setAutoRead(false);
                state.isReregistering = true;

                channel.deregister().addListener(future -> {
                    if (future.isSuccess()) {
                        group.register(promise);
                    } else {
                        promise.setFailure(new RuntimeException("Failed to deregister channel", future.cause()));
                    }
                });

                promise.addListener(future -> {
                    synchronized (state) {
                        state.isReregistering = false;
                        if (future.isSuccess()) {
                            channel.config().setAutoRead(true);
                        } else {
                            if (channel.pipeline() != null) {
                                channel.pipeline().fireExceptionCaught(future.cause());
                            }
                        }
                        if (state.pendingReregistration != null) {
                            EventLoopGroup pending = state.pendingReregistration;
                            state.pendingReregistration = null;
                            reregister(channel, pending);
                        }
                    }
                });

                if (!promise.channel().eventLoop().inEventLoop()) {
                    promise.syncUninterruptibly();
                }
            } catch (Exception e) {
                state.isReregistering = false;
                LOGGER.warning("[MultiNettyEventLoop] Error reregistering channel: " + e.getMessage());
            }
        }
    }

    private static ThreadFactory createThreadFactory(String namePrefix) {
        AtomicInteger counter = new AtomicInteger(0);
        return r -> {
            Thread thread = new FastThreadLocalThread(r, namePrefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
