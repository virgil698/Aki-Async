package org.virgil.akiasync.network;

public enum PacketPriority {

    CRITICAL(0, "关键数据包", "Critical Packets"),

    HIGH(1, "高优先级数据包", "High Priority Packets"),

    NORMAL(2, "普通数据包", "Normal Packets"),

    LOW(3, "低优先级数据包", "Low Priority Packets");

    private final int level;
    private final String nameCn;
    private final String nameEn;

    PacketPriority(int level, String nameCn, String nameEn) {
        this.level = level;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
    }

    public int getLevel() {
        return level;
    }

    public String getNameCn() {
        return nameCn;
    }

    public String getNameEn() {
        return nameEn;
    }

    public boolean isHigherThan(PacketPriority other) {
        return this.level < other.level;
    }

    public boolean isLowerThan(PacketPriority other) {
        return this.level > other.level;
    }
}
