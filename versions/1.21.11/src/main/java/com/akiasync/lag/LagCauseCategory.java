package com.akiasync.lag;

public enum LagCauseCategory {
    MOB("生物"),
    ENTITY("实体"),
    BLOCK("方块"),
    CHUNK("区块"),
    PLUGIN("插件"),
    WORLD_SAVE("世界保存"),
    NETWORK("网络"),
    GC("GC"),
    CPU("CPU"),
    MEMORY("内存"),
    DISK_IO("磁盘IO"),
    WORLD_GENERATION("地图生成"),
    PLAYER_ACTION("玩家行为"),
    COMMAND("命令"),
    DATA_PACK("数据包"),
    CONFIGURATION("配置问题"),
    UNKNOWN("未归因");

    private final String displayName;

    LagCauseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
