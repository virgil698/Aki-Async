package org.virgil.akiasync.mixin.mixins.chunk.loading;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public class LevelChunkLightningMixin {

    @Shadow
    @Final
    public ServerLevel level;

    @Unique
    private int akiasync$lightningTick = -1;

    @Unique
    public boolean akiasync$shouldDoLightning(RandomSource random) {

        if (this.akiasync$lightningTick == -1) {
            this.akiasync$lightningTick = random.nextInt(100000) << 1;
        }

        if (this.akiasync$lightningTick-- <= 0) {

            this.akiasync$lightningTick = random.nextInt(this.level.spigotConfig.thunderChance) << 1;
            return true;
        }
        return false;
    }
}
