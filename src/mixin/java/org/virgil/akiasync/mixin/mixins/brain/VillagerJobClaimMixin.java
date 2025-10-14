package org.virgil.akiasync.mixin.mixins.brain;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

/**
 * Villager job acquisition atomicity fix (1.21.8 API)
 * 
 * Problem: Async Brain writes JOB_SITE → Main thread POI already taken → Skin doesn't change
 * Solution: Job acquisition must return to main thread for atomic verification
 * 
 * Core logic (1.21.8 API):
 * 1. Atomic claim: PoiManager.take() verifies ownership
 * 2. Claim fails → Erase memory, avoid infinite retry loop
 * 3. Claim succeeds → Vanilla logic updates profession/skin automatically  
 * 4. Entire process < 0.1ms → No MSPT impact
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = Villager.class, priority = 1200)
public abstract class VillagerJobClaimMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    
    /**
     * Atomically handle job acquisition after customServerAiStep
     * 
     * @At("RETURN") - Execute before method returns
     */
    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void aki$atomicClaim(CallbackInfo ci) {
        // Initialization check
        if (!initialized) { aki$initAtomicClaim(); }
        if (!cached_enabled) return;
        
        Villager villager = (Villager) (Object) this;
        
        // Key fix: Only execute atomic claim for unemployed villagers
        if (!villager.getVillagerData().profession().is(
                net.minecraft.world.entity.npc.VillagerProfession.NONE)) {
            return;  // Already has profession → skip, prevent repeated claiming
        }
        
        Brain<?> brain = villager.getBrain();
        
        // 1.21.8: JOB_SITE serves as both WANTED and claimed (no WANTED_ prefix)
        Optional<GlobalPos> wanted = brain.getMemory(MemoryModuleType.JOB_SITE);
        if (wanted.isEmpty()) return;
        
        GlobalPos globalPos = wanted.get();
        BlockPos pos = globalPos.pos();
        ServerLevel level = (ServerLevel) villager.level();
        
        // 1. Atomic claim (1.21.8 correct API: 4 parameters)
        Optional<BlockPos> result = level.getPoiManager().take(
            holder -> true,  // Accept all POI types (simplified)
            (holder, blockPos) -> blockPos.equals(pos),
            pos,
            1
        );
        
        // Return value is Optional<BlockPos>, check if successful and position matches
        if (result.isPresent() && result.get().equals(pos)) {
            // Claim succeeded → Do nothing, vanilla logic will update profession/skin automatically
            return;
        }
        
        // Claim failed → Erase memory to avoid infinite retry loop
        brain.eraseMemory(MemoryModuleType.JOB_SITE);
    }
    
    /**
     * Initialize configuration
     */
    @Unique
    private static synchronized void aki$initAtomicClaim() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isVillagerOptimizationEnabled();
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
        System.out.println("[AkiAsync] VillagerJobClaimMixin initialized (atomic claim): enabled=" + cached_enabled);
    }
}

