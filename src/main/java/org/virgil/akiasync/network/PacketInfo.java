package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;

public class PacketInfo implements Comparable<PacketInfo> {

    private final Packet<?> packet;
    private final PacketPriority priority;
    private final long timestamp;
    private final String playerName;

    public PacketInfo(Packet<?> packet, PacketPriority priority, String playerName) {
        this.packet = packet;
        this.priority = priority;
        this.timestamp = System.nanoTime();
        this.playerName = playerName;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public PacketPriority getPriority() {
        return priority;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getWaitingTime() {
        return System.nanoTime() - timestamp;
    }

    public long getWaitingTimeMs() {
        return getWaitingTime() / 1_000_000;
    }

    @Override
    public int compareTo(PacketInfo other) {

        int priorityCompare = Integer.compare(this.priority.getLevel(), other.priority.getLevel());
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PacketInfo other = (PacketInfo) obj;
        return timestamp == other.timestamp && 
               priority == other.priority &&
               packet == other.packet;
    }

    @Override
    public int hashCode() {
        int result = priority.hashCode();
        result = 31 * result + Long.hashCode(timestamp);
        result = 31 * result + (packet != null ? System.identityHashCode(packet) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("PacketInfo{priority=%s, packet=%s, player=%s, waitingMs=%d}",
            priority.name(),
            packet.getClass().getSimpleName(),
            playerName,
            getWaitingTimeMs()
        );
    }
}
