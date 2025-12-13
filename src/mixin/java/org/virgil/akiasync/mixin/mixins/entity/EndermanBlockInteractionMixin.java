package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;


@Mixin(EnderMan.class)
public class EndermanBlockInteractionMixin {
    
    @Unique
    private static volatile boolean cached_initialized = false;
    
    @Unique
    private static volatile boolean cached_allowPickupBlocks = true;
    
    @Unique
    private static volatile boolean cached_allowPlaceBlocks = true;
    
    
    @Inject(method = "registerGoals", at = @At("RETURN"))
    @SuppressWarnings("unused") 
    private void aki$controlBlockInteraction(CallbackInfo ci) {
        if (!cached_initialized) {
            aki$initConfig();
        }
        
        
        if (cached_allowPickupBlocks && cached_allowPlaceBlocks) {
            return;
        }
        
        EnderMan enderman = (EnderMan) (Object) this;
        GoalSelector goalSelector = enderman.goalSelector;
        
        try {
            
            java.lang.reflect.Field availableGoalsField = GoalSelector.class.getDeclaredField("availableGoals");
            availableGoalsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Set<net.minecraft.world.entity.ai.goal.WrappedGoal> availableGoals = 
                (java.util.Set<net.minecraft.world.entity.ai.goal.WrappedGoal>) availableGoalsField.get(goalSelector);
            
            
            availableGoals.removeIf(wrappedGoal -> {
                Goal goal = wrappedGoal.getGoal();
                String goalClassName = goal.getClass().getSimpleName();
                
                
                if (!cached_allowPickupBlocks && goalClassName.equals("EndermanTakeBlockGoal")) {
                    Bridge bridge = BridgeManager.getBridge();
                    if (bridge != null && bridge.isDebugLoggingEnabled()) {
                        bridge.debugLog("[AkiAsync-Enderman] Removed EndermanTakeBlockGoal (pickup disabled)");
                    }
                    return true;
                }
                
                
                if (!cached_allowPlaceBlocks && goalClassName.equals("EndermanLeaveBlockGoal")) {
                    Bridge bridge = BridgeManager.getBridge();
                    if (bridge != null && bridge.isDebugLoggingEnabled()) {
                        bridge.debugLog("[AkiAsync-Enderman] Removed EndermanLeaveBlockGoal (place disabled)");
                    }
                    return true;
                }
                
                return false;
            });
            
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-Enderman] Failed to control block interaction: %s", e.getMessage());
            }
        }
    }
    
    @Unique
    private static synchronized void aki$initConfig() {
        if (cached_initialized) {
            return;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_allowPickupBlocks = bridge.isEndermanAllowPickupBlocks();
            cached_allowPlaceBlocks = bridge.isEndermanAllowPlaceBlocks();
            
            bridge.debugLog(
                "[AkiAsync] EndermanBlockInteractionMixin initialized: pickup=%s, place=%s",
                cached_allowPickupBlocks, cached_allowPlaceBlocks);
        } else {
            
            cached_allowPickupBlocks = true;
            cached_allowPlaceBlocks = true;
        }
        
        cached_initialized = true;
    }
}
