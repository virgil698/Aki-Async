package org.virgil.akiasync.mixin.mixins.entity.combat;

import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = CombatTracker.class, priority = 900)
public class CombatTrackerConcurrentMixin {

    @Shadow
    @Final
    @Mutable
    private List<CombatEntry> entries = new CopyOnWriteArrayList<>();
}
