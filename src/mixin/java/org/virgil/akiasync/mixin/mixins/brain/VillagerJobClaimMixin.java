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
@SuppressWarnings("unused")
@Mixin(value = Villager.class, priority = 1200)
public abstract class VillagerJobClaimMixin {
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void aki$atomicClaim(CallbackInfo ci) {
        if (!initialized) { aki$initAtomicClaim(); }
        if (!cached_enabled) return;

        Villager villager = (Villager) (Object) this;

        if (!villager.getVillagerData().profession().is(
                net.minecraft.world.entity.npc.VillagerProfession.NONE)) {
            return;
        }

        Brain<?> brain = villager.getBrain();
        Optional<GlobalPos> wanted = brain.getMemory(MemoryModuleType.JOB_SITE);
        if (wanted.isEmpty()) return;

        GlobalPos globalPos = wanted.get();
        BlockPos pos = globalPos.pos();
        ServerLevel level = (ServerLevel) villager.level();

        Optional<BlockPos> result = level.getPoiManager().take(
            holder -> true,
            (holder, blockPos) -> blockPos.equals(pos),
            pos,
            1
        );

        if (result.isPresent() && result.get().equals(pos)) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-JobClaim] Villager successfully claimed job site at " + pos);
            }
            return;
        }

        brain.eraseMemory(MemoryModuleType.JOB_SITE);
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-JobClaim] Villager failed to claim job site at " + pos + ", memory erased");
        }
    }
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
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] VillagerJobClaimMixin initialized (atomic claim): enabled=" + cached_enabled);
        }
    }

    @Unique
    private static synchronized void aki$resetInitialization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] Resetting VillagerJobClaimMixin initialization");
        }
        initialized = false;
        cached_enabled = false;
    }
}