package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(SurfaceRules.SequenceRule.class)
public class SequenceRuleMixin {

    @Shadow @Final
    private List<SurfaceRules.SurfaceRule> rules;

    @Unique
    private SurfaceRules.SurfaceRule[] akiasync$rulesArray;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.akiasync$rulesArray = this.rules.toArray(SurfaceRules.SurfaceRule[]::new);
    }

    @Overwrite
    public @Nullable BlockState tryApply(int x, int y, int z) {
        for (SurfaceRules.SurfaceRule rule : this.akiasync$rulesArray) {
            BlockState state = rule.tryApply(x, y, z);
            if (state != null) {
                return state;
            }
        }
        return null;
    }
}
