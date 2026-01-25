package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class CompatibilityConfig {

    private boolean virtualEntityCompatibilityEnabled;
    private boolean virtualEntityBypassPacketQueue;
    private boolean virtualEntityExcludeFromThrottling;

    private boolean fancynpcsCompatEnabled;
    private boolean fancynpcsUseAPI;
    private int fancynpcsPriority;

    private boolean znpcsplusCompatEnabled;
    private boolean znpcsplusUseAPI;
    private int znpcsplusPriority;

    private List<String> virtualEntityDetectionOrder;

    public void load(FileConfiguration config) {
        virtualEntityCompatibilityEnabled = config.getBoolean("virtual-entity-compatibility.enabled", true);
        virtualEntityBypassPacketQueue = config.getBoolean("virtual-entity-compatibility.bypass-packet-queue", true);
        virtualEntityExcludeFromThrottling = config.getBoolean("virtual-entity-compatibility.exclude-from-throttling", true);

        fancynpcsCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.enabled", true);
        fancynpcsUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.use-api", true);
        fancynpcsPriority = config.getInt("virtual-entity-compatibility.plugins.fancynpcs.priority", 90);

        znpcsplusCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.enabled", true);
        znpcsplusUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.use-api", false);
        znpcsplusPriority = config.getInt("virtual-entity-compatibility.plugins.znpcsplus.priority", 90);

        virtualEntityDetectionOrder = config.getStringList("virtual-entity-compatibility.detection-order");
    }

    public boolean isVirtualEntityCompatibilityEnabled() { return virtualEntityCompatibilityEnabled; }
    public boolean isVirtualEntityBypassPacketQueue() { return virtualEntityBypassPacketQueue; }
    public boolean isVirtualEntityExcludeFromThrottling() { return virtualEntityExcludeFromThrottling; }

    public boolean isFancynpcsCompatEnabled() { return fancynpcsCompatEnabled; }
    public boolean isFancynpcsUseAPI() { return fancynpcsUseAPI; }
    public int getFancynpcsPriority() { return fancynpcsPriority; }

    public boolean isZnpcsplusCompatEnabled() { return znpcsplusCompatEnabled; }
    public boolean isZnpcsplusUseAPI() { return znpcsplusUseAPI; }
    public int getZnpcsplusPriority() { return znpcsplusPriority; }

    public List<String> getVirtualEntityDetectionOrder() { return virtualEntityDetectionOrder; }
}
