package org.virgil.akiasync.compat;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.State;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

public class ViaVersionCompat {
    
    private static final Logger LOGGER = Logger.getLogger("AkiAsync-ViaCompat");
    private static boolean viaVersionAvailable = false;
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        try {
            Class.forName("com.viaversion.viaversion.api.Via");
            viaVersionAvailable = true;
            LOGGER.info("[ViaCompat] ViaVersion detected, compatibility layer enabled");
        } catch (ClassNotFoundException e) {
            viaVersionAvailable = false;
            LOGGER.info("[ViaCompat] ViaVersion not found, compatibility layer disabled");
        }
    }
    
    public static boolean isViaVersionAvailable() {
        return viaVersionAvailable;
    }
    
    public static int getPlayerProtocolVersion(Player player) {
        if (!viaVersionAvailable) {
            return -1;
        }
        
        try {
            UserConnection connection = Via.getAPI().getConnection(player.getUniqueId());
            if (connection != null) {
                return connection.getProtocolInfo().protocolVersion().getVersion();
            }
        } catch (Exception e) {
            LOGGER.warning("[ViaCompat] Failed to get protocol version for player " + player.getName() + ": " + e.getMessage());
        }
        
        return -1;
    }
    
    public static boolean isPlayerUsingVia(UUID playerId) {
        if (!viaVersionAvailable) {
            return false;
        }
        
        try {
            UserConnection connection = Via.getAPI().getConnection(playerId);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isPlayerUsingVia(Player player) {
        return isPlayerUsingVia(player.getUniqueId());
    }
    
    public static boolean isConnectionInPlayState(UUID playerId) {
        if (!viaVersionAvailable) {
            return true;
        }
        
        try {
            UserConnection connection = Via.getAPI().getConnection(playerId);
            if (connection != null) {
                State state = connection.getProtocolInfo().getState(Direction.CLIENTBOUND);
                return state == State.PLAY;
            }
        } catch (Exception e) {
            LOGGER.warning("[ViaCompat] Failed to check connection state for player " + playerId + ": " + e.getMessage());
        }
        
        return true;
    }
    
    public static boolean isConnectionInPlayState(Player player) {
        return isConnectionInPlayState(player.getUniqueId());
    }
    
    public static int getServerProtocolVersion() {
        if (!viaVersionAvailable) {
            return -1;
        }
        
        try {
            return Via.getAPI().getServerVersion().lowestSupportedProtocolVersion().getVersion();
        } catch (Exception e) {
            LOGGER.warning("[ViaCompat] Failed to get server protocol version: " + e.getMessage());
            return -1;
        }
    }
    
    public static boolean isProtocolVersionDifferent(Player player) {
        if (!viaVersionAvailable) {
            return false;
        }
        
        int playerVersion = getPlayerProtocolVersion(player);
        int serverVersion = getServerProtocolVersion();
        
        if (playerVersion == -1 || serverVersion == -1) {
            return false;
        }
        
        return playerVersion != serverVersion;
    }
    
    public static String getProtocolVersionName(int protocolVersion) {
        return switch (protocolVersion) {
            case 769 -> "1.21.4";
            case 770 -> "1.21.5";
            case 771 -> "1.21.6";
            case 767 -> "1.21.1";
            case 766 -> "1.21";
            default -> "Unknown (" + protocolVersion + ")";
        };
    }
    
    public static void logPlayerProtocolInfo(Player player) {
        if (!viaVersionAvailable) {
            return;
        }
        
        int playerVersion = getPlayerProtocolVersion(player);
        int serverVersion = getServerProtocolVersion();
        boolean usingVia = isPlayerUsingVia(player);
        boolean inPlayState = isConnectionInPlayState(player);
        
        LOGGER.info(String.format(
            "[ViaCompat] Player: %s, Protocol: %s, Server: %s, UsingVia: %s, PlayState: %s",
            player.getName(),
            getProtocolVersionName(playerVersion),
            getProtocolVersionName(serverVersion),
            usingVia,
            inPlayState
        ));
    }
}
