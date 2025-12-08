package org.virgil.akiasync.mixin.mixins.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.async.explosion.merge.MergeableTNT;
import org.virgil.akiasync.mixin.async.explosion.merge.TNTMergeHandler;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.List;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMergeMixin implements MergeableTNT {
    
    @Unique
    private int aki$mergeCount = 1;

    @Override
    public int aki$getMergeCount() {
        return aki$mergeCount;
    }

    @Override
    public void aki$setMergeCount(int count) {
        this.aki$mergeCount = count;
    }

    @Override
    public void aki$mergeWith(PrimedTnt other) {
        if (other instanceof MergeableTNT mergeable) {
            this.aki$mergeCount += mergeable.aki$getMergeCount();
        }
    }

    @Override
    public boolean aki$canMergeWith(PrimedTnt other) {
        return TNTMergeHandler.canMerge((PrimedTnt)(Object)this, other);
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void aki$mergeBeforeExplode(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt)(Object)this;
        
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge == null || !bridge.isTNTMergeEnabled()) {
            return;
        }

        List<PrimedTnt> merged = TNTMergeHandler.mergeNearbyTNT(serverLevel, self);
        
        for (PrimedTnt tnt : merged) {
            tnt.discard();
        }

        if (!merged.isEmpty() && bridge.isTNTDebugEnabled()) {
            BridgeConfigCache.debugLog("[AkiAsync-TNT] Merged " + merged.size() + 
                " TNT entities. Total merge count: " + aki$mergeCount);
        }
    }

    @Unique
    public float aki$getMergedPower() {
        return TNTMergeHandler.calculateMergedPower(aki$mergeCount);
    }
}
