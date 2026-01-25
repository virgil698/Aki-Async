package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Delegate class handling network-related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class NetworkBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;

    public NetworkBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public long getConnectionPendingBytes(Object connection) {
        try {
            if (connection instanceof net.minecraft.network.Connection conn) {
                if (conn.channel != null && conn.channel.isActive() &&
                    conn.channel.unsafe() != null &&
                    conn.channel.unsafe().outboundBuffer() != null) {
                    return conn.channel.unsafe().outboundBuffer().totalPendingWriteBytes();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    public boolean addFlushConsolidationHandler(Object channel, int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress) {
        try {
            if (channel instanceof io.netty.channel.Channel nettyChannel) {
                io.netty.channel.ChannelPipeline pipeline = nettyChannel.pipeline();
                if (pipeline != null && pipeline.get("flush_consolidation") == null) {
                    pipeline.addFirst("flush_consolidation",
                        new io.netty.handler.flush.FlushConsolidationHandler(explicitFlushAfterFlushes, consolidateWhenNoReadInProgress));
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public void sendPacketWithoutFlush(Object connection, Object packet) {
        if (!(connection instanceof net.minecraft.network.Connection conn) ||
            !(packet instanceof net.minecraft.network.protocol.Packet<?> pkt)) {
            return;
        }
        try {
            conn.send((net.minecraft.network.protocol.Packet<?>) pkt, null, false);
        } catch (Exception e) {
            conn.send((net.minecraft.network.protocol.Packet<?>) pkt);
        }
    }

    public void flushConnection(Object connection) {
        try {
            if (connection instanceof net.minecraft.network.Connection conn) {
                conn.flushChannel();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public Object getConnectionFromListener(Object listener) {
        try {
            if (listener instanceof net.minecraft.server.network.ServerCommonPacketListenerImpl impl) {
                return impl.connection;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public void handleConnectionProtocolChange(Object connection, int protocolOrdinal) {
        if (config == null || !config.isMultiNettyEventLoopEnabled()) {
            return;
        }

        try {
            if (connection instanceof net.minecraft.network.Connection conn) {
                org.virgil.akiasync.network.MultiNettyEventLoopManager.handleProtocolChange(conn, protocolOrdinal);
            }
        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[MultiNettyEventLoop] Error handling protocol change: " + e.getMessage());
        }
    }

    public long getNetworkTrafficInRate() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getCurrentInRate();
    }

    public long getNetworkTrafficOutRate() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getCurrentOutRate();
    }

    public long getNetworkTrafficTotalIn() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getTotalBytesIn();
    }

    public long getNetworkTrafficTotalOut() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getTotalBytesOut();
    }

    public void calculateNetworkTrafficRates() {
        org.virgil.akiasync.mixin.network.NetworkTrafficTracker.calculateRates();
    }

    public void tickChunkVisibilityFilter() {
        if (config == null || !config.isChunkVisibilityFilterEnabled()) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.network.ChunkVisibilityFilter.tick();
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "NetworkBridgeDelegate", "tickChunkVisibilityFilter", e);
        }
    }

    // PacketStatistics methods
    public boolean isPacketStatisticsEnabled() {
        return org.virgil.akiasync.mixin.network.PacketStatistics.isEnabled();
    }

    public void resetPacketStatistics() {
        org.virgil.akiasync.mixin.network.PacketStatistics.reset();
    }

    public long getPacketStatisticsElapsedSeconds() {
        return org.virgil.akiasync.mixin.network.PacketStatistics.getElapsedSeconds();
    }

    public java.util.List<Object[]> getTopOutgoingPackets(int limit) {
        return org.virgil.akiasync.mixin.network.PacketStatistics.getTopOutgoing(limit).stream()
            .map(stat -> new Object[]{stat.name(), stat.count(), stat.totalBytes(), stat.countPerSecond(), stat.bytesPerSecond()})
            .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<Object[]> getTopIncomingPackets(int limit) {
        return org.virgil.akiasync.mixin.network.PacketStatistics.getTopIncoming(limit).stream()
            .map(stat -> new Object[]{stat.name(), stat.count(), stat.totalBytes(), stat.countPerSecond(), stat.bytesPerSecond()})
            .collect(java.util.stream.Collectors.toList());
    }

    public long getTotalOutgoingPacketCount() {
        return org.virgil.akiasync.mixin.network.PacketStatistics.getTotalOutgoingCount();
    }

    public long getTotalIncomingPacketCount() {
        return org.virgil.akiasync.mixin.network.PacketStatistics.getTotalIncomingCount();
    }
}
