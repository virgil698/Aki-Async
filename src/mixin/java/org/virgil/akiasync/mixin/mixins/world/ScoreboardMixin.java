package org.virgil.akiasync.mixin.mixins.world;

import org.virgil.akiasync.mixin.util.concurrent.ConcurrentCollections;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {

    @Shadow
    private final Map<String, Map<Objective, Score>> playerScores = ConcurrentCollections.newHashMap();
}
