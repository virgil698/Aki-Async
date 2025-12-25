package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.FoliaUtils;


@Mixin(Projectile.class)
public abstract class ProjectileChunkLoadingMixin extends Entity implements TraceableEntity {
    
    @Unique
    private static int akiasync$loadedThisTick = 0;
    
    @Unique
    private static int akiasync$loadedTick;
    
    @Unique
    private int akiasync$loadedLifetime = 0;
    
    public ProjectileChunkLoadingMixin(EntityType<?> type, Level level) {
        super(type, level);
    }
    
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null || !bridge.isProjectileOptimizationEnabled()) {
            return;
        }
        
        
        int currentTick = (int) FoliaUtils.getCurrentTick();
        if (akiasync$loadedTick != currentTick) {
            akiasync$loadedTick = currentTick;
            akiasync$loadedThisTick = 0;
        }
        
        
        int chunkX = Mth.floor(this.getX()) >> 4;
        int chunkZ = Mth.floor(this.getZ()) >> 4;
        
        boolean isLoaded = ((ServerChunkCache) this.level().getChunkSource())
            .getChunkAtIfLoadedImmediately(chunkX, chunkZ) != null;
        
        if (!isLoaded) {
            
            if (akiasync$loadedThisTick > bridge.getMaxProjectileLoadsPerTick()) {
                
                if (++this.akiasync$loadedLifetime > bridge.getMaxProjectileLoadsPerProjectile()) {
                    if (bridge.isDebugLoggingEnabled()) {
                        bridge.debugLog("[AkiAsync-Projectile] Removed projectile after loading " + 
                            this.akiasync$loadedLifetime + " chunks (limit: " + 
                            bridge.getMaxProjectileLoadsPerProjectile() + ")");
                    }
                    this.discard();
                }
                return;
            }
            akiasync$loadedThisTick++;
        }
    }
}
