package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;


@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageOptimizationMixin<T extends EntityAccess> {
    
    @Unique
    private static boolean akiasync$initialized = false;
    
    @Unique
    private static boolean akiasync$enabled = true;

    @Shadow
    public abstract EntitySection<T> getSection(long sectionPos);
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isEntitySectionStorageOptimizationEnabled();
            }
        } catch (Exception e) {
            
            akiasync$enabled = true;
        }
        
        akiasync$initialized = true;
    }

    
    @Inject(
            method = "forEachAccessibleNonEmptySection",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/core/SectionPos;posToSectionCoord(D)I",
                    ordinal = 5
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    public void optimizeSmallAreaIteration(AABB box, AbortableIterationConsumer<EntitySection<T>> action, CallbackInfo ci,
                                           int i, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        
        if (!akiasync$enabled) {
            return;
        }
        
        
        if (maxX >= minX + 4 || maxZ >= minZ + 4) {
            return;
        }
        
        ci.cancel();

        
        for (int x = minX; x <= maxX; x++) {
            
            for (int z = Math.max(minZ, 0); z <= maxZ; z++) {
                if (this.forEachInColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }

            
            int bound = Math.min(-1, maxZ);
            for (int z = minZ; z <= bound; z++) {
                if (this.forEachInColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }
        }
    }

    
    private AbortableIterationConsumer.Continuation forEachInColumn(int x, int minY, int maxY, int z,
                                                                     AbortableIterationConsumer<EntitySection<T>> action) {
        AbortableIterationConsumer.Continuation ret = AbortableIterationConsumer.Continuation.CONTINUE;
        
        
        for (int y = Math.max(minY, 0); y <= maxY; y++) {
            ret = this.consumeSection(SectionPos.asLong(x, y, z), action);
            if (ret.shouldAbort()) {
                return ret;
            }
        }
        
        
        int bound = Math.min(-1, maxY);
        for (int y = minY; y <= bound; y++) {
            ret = this.consumeSection(SectionPos.asLong(x, y, z), action);
            if (ret.shouldAbort()) {
                return ret;
            }
        }
        
        return ret;
    }

    
    private AbortableIterationConsumer.Continuation consumeSection(long pos, AbortableIterationConsumer<EntitySection<T>> action) {
        EntitySection<T> section = this.getSection(pos);
        
        
        if (section != null && section.size() != 0 && section.getStatus().isAccessible()) {
            return action.accept(section);
        }
        
        return AbortableIterationConsumer.Continuation.CONTINUE;
    }
}
