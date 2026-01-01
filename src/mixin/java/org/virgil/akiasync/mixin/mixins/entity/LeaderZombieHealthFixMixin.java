package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(Zombie.class)
public class LeaderZombieHealthFixMixin {
    
    private static final ResourceLocation LEADER_ZOMBIE_BONUS_ID = 
        ResourceLocation.withDefaultNamespace("leader_zombie_bonus");
    
    private static volatile boolean enabled = true;
    private static volatile boolean initialized = false;
    
    private static void init() {
        if (initialized) return;
        synchronized (LeaderZombieHealthFixMixin.class) {
            if (initialized) return;
            try {
                var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                enabled = bridge.isLeaderZombieHealthFixEnabled();
                BridgeConfigCache.debugLog("[AkiAsync-LeaderZombieFix] Initialized: enabled=" + enabled);
            } catch (Exception e) {
                BridgeConfigCache.debugLog("[AkiAsync-LeaderZombieFix] Init failed, using default: " + e.getMessage());
            }
        }
    }
    
    @Inject(
        method = "finalizeSpawn",
        at = @At("RETURN")
    )
    private void onFinalizeSpawnReturn(CallbackInfoReturnable<?> cir) {
        if (!initialized) {
            init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            Zombie zombie = (Zombie) (Object) this;
            
            AttributeInstance maxHealthAttribute = zombie.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttribute == null) {
                return;
            }
            
            AttributeModifier leaderBonus = maxHealthAttribute.getModifier(LEADER_ZOMBIE_BONUS_ID);
            if (leaderBonus == null) {
                return;
            }
            
            float currentHealth = zombie.getHealth();
            float maxHealth = zombie.getMaxHealth();
            float defaultHealth = 20.0F;
            
            if (Math.abs(currentHealth - defaultHealth) < 0.1F && maxHealth > defaultHealth) {
                zombie.setHealth(maxHealth);
                
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-LeaderZombieFix] Fixed leader zombie health: %s (%.1f -> %.1f, bonus=%.1f)",
                    EntityType.getKey(zombie.getType()),
                    defaultHealth,
                    maxHealth,
                    leaderBonus.amount()
                ));
            }
            
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-LeaderZombieFix] Error fixing leader zombie health: " + e.getMessage());
        }
    }
}
