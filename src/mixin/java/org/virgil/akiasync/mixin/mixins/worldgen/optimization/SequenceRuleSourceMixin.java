package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SurfaceRules.SequenceRuleSource.class)
public class SequenceRuleSourceMixin {

    @Unique
    private static final SurfaceRules.SurfaceRule EMPTY =
        new SurfaceRules.SequenceRule(List.of());

    @Shadow @Final
    private List<SurfaceRules.RuleSource> sequence;

    @Unique
    private SurfaceRules.RuleSource[] akiasync$sequenceArray;

    @Unique
    private boolean akiasync$isSingleOrNoElement;

    @Unique
    private SurfaceRules.RuleSource akiasync$firstElement;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.akiasync$sequenceArray = this.sequence.toArray(SurfaceRules.RuleSource[]::new);
        this.akiasync$isSingleOrNoElement = this.akiasync$sequenceArray.length <= 1;
        this.akiasync$firstElement = this.akiasync$sequenceArray.length == 0 ? null : this.akiasync$sequenceArray[0];
    }

    @Overwrite
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        if (this.akiasync$isSingleOrNoElement) {
            return this.akiasync$firstElement != null ? this.akiasync$firstElement.apply(context) : EMPTY;
        } else {
            @SuppressWarnings("UnstableApiUsage")
            ImmutableList.Builder<SurfaceRules.SurfaceRule> builder =
                ImmutableList.builderWithExpectedSize(this.akiasync$sequenceArray.length);

            for (SurfaceRules.RuleSource ruleSource : this.akiasync$sequenceArray) {
                builder.add(ruleSource.apply(context));
            }

            return new SurfaceRules.SequenceRule(builder.build());
        }
    }
}
