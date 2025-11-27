package org.virgil.akiasync.network;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerJoinRampUpController {

    private final Logger logger;
    private final boolean debugEnabled;
    
    
    private final Map<UUID, PlayerJoinState> playerJoinStates = new ConcurrentHashMap<>();
    
    
    private int initialSendRate;        
    private int targetSendRate;         
    private int rampUpDurationSeconds;  
    private int rampUpSteps;            
    
    public PlayerJoinRampUpController(Logger logger, boolean debugEnabled) {
        this.logger = logger;
        this.debugEnabled = debugEnabled;
        
        
        this.initialSendRate = 3;  
        this.targetSendRate = 50;
        this.rampUpDurationSeconds = 15;  
        this.rampUpSteps = 10;  
    }
    
    
    private static class PlayerJoinState {
        private final long joinTime;
        private int currentSendRate;
        private int currentStep;
        
        public PlayerJoinState(int initialRate) {
            this.joinTime = System.currentTimeMillis();
            this.currentSendRate = initialRate;
            this.currentStep = 0;
        }
        
        public long getJoinTime() {
            return joinTime;
        }
        
        public int getCurrentSendRate() {
            return currentSendRate;
        }
        
        public void setCurrentSendRate(int rate) {
            this.currentSendRate = rate;
        }
        
        public int getCurrentStep() {
            return currentStep;
        }
        
        public void incrementStep() {
            this.currentStep++;
        }
        
        public long getElapsedSeconds() {
            return (System.currentTimeMillis() - joinTime) / 1000;
        }
    }
    
    
    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();
        playerJoinStates.put(playerId, new PlayerJoinState(initialSendRate));
        
        if (debugEnabled) {
            logger.info(String.format(
                "[JoinRampUp] Player %s joined, initial send rate: %d packets/tick",
                player.getName(),
                initialSendRate
            ));
        }
    }
    
    
    public void onPlayerQuit(UUID playerId) {
        playerJoinStates.remove(playerId);
    }
    
    
    public int getPlayerSendRate(UUID playerId) {
        PlayerJoinState state = playerJoinStates.get(playerId);
        if (state == null) {
            return targetSendRate; 
        }
        
        updatePlayerSendRate(playerId, state);
        return state.getCurrentSendRate();
    }
    
    
    private void updatePlayerSendRate(UUID playerId, PlayerJoinState state) {
        long elapsedSeconds = state.getElapsedSeconds();
        
        
        if (elapsedSeconds >= rampUpDurationSeconds) {
            state.setCurrentSendRate(targetSendRate);
            playerJoinStates.remove(playerId); 
            
            if (debugEnabled) {
                logger.info(String.format(
                    "[JoinRampUp] Player %s ramp-up completed, final rate: %d packets/tick",
                    playerId,
                    targetSendRate
                ));
            }
            return;
        }
        
        
        int targetStep = (int) (elapsedSeconds * rampUpSteps / rampUpDurationSeconds);
        
        
        if (targetStep > state.getCurrentStep()) {
            state.incrementStep();
            
            
            double progress = (double) state.getCurrentStep() / rampUpSteps;
            int newRate = (int) (initialSendRate + (targetSendRate - initialSendRate) * progress);
            
            state.setCurrentSendRate(newRate);
            
            if (debugEnabled) {
                logger.info(String.format(
                    "[JoinRampUp] Player %s step %d/%d, send rate: %d -> %d packets/tick (%.1f%% complete)",
                    playerId,
                    state.getCurrentStep(),
                    rampUpSteps,
                    state.getCurrentSendRate(),
                    newRate,
                    progress * 100
                ));
            }
        }
    }
    
    
    public boolean isPlayerInRampUp(UUID playerId) {
        return playerJoinStates.containsKey(playerId);
    }
    
    
    public double getPlayerRampUpProgress(UUID playerId) {
        PlayerJoinState state = playerJoinStates.get(playerId);
        if (state == null) {
            return 1.0; 
        }
        
        long elapsedSeconds = state.getElapsedSeconds();
        return Math.min(1.0, (double) elapsedSeconds / rampUpDurationSeconds);
    }
    
    
    public void configure(int initialRate, int targetRate, int durationSeconds, int steps) {
        this.initialSendRate = Math.max(1, initialRate);
        this.targetSendRate = Math.max(initialRate, targetRate);
        this.rampUpDurationSeconds = Math.max(1, durationSeconds);
        this.rampUpSteps = Math.max(1, steps);
        
        if (debugEnabled) {
            logger.info(String.format(
                "[JoinRampUp] Configured: %d -> %d packets/tick over %d seconds (%d steps)",
                initialSendRate,
                targetSendRate,
                rampUpDurationSeconds,
                rampUpSteps
            ));
        }
    }
    
    
    public String getStatistics() {
        int activeRampUps = playerJoinStates.size();
        if (activeRampUps == 0) {
            return "No active ramp-ups";
        }
        
        return String.format(
            "Active ramp-ups: %d, Config: %d->%d over %ds",
            activeRampUps,
            initialSendRate,
            targetSendRate,
            rampUpDurationSeconds
        );
    }
    
    
    public void clear() {
        playerJoinStates.clear();
    }
}
