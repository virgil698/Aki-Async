package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
@SuppressWarnings("unused")
@Mixin(value = Villager.class, priority = 1200)
public class VillagerBreedAsyncMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void aki$throttleBreedCheck(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (!(villager.level() instanceof ServerLevel sl)) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge == null || !bridge.isAsyncVillagerBreedEnabled()) return;
        BlockPos pos = villager.blockPosition();
        long currentTick = sl.getGameTime();
        if (bridge.isVillagerAgeThrottleEnabled()) {
            if (org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor
                    .isIdle(villager.getUUID(), pos, currentTick)) {
                ci.cancel();
                return;
            }
        }
        int interval = bridge.getVillagerBreedCheckInterval();
        if (currentTick % interval != 0) {
            ci.cancel();
        }
    }
}