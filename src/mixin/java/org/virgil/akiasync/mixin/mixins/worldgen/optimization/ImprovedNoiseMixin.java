package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ImprovedNoise.class)
public abstract class ImprovedNoiseMixin {
    @Shadow @Final
    public double xo;

    @Shadow @Final
    public double yo;

    @Shadow @Final
    public double zo;

    @Shadow @Final
    private byte[] p;

    @Shadow
    protected abstract double sampleAndLerp(int sectionX, int sectionY, int sectionZ, double localX, double localY, double localZ, double fadeLocalX);

    @Unique
    private static final double[] akiasync$OPTIMIZED_GRADIENTS = akiasync$createOptimizedGradients();

    @Unique
    private int[] akiasync$optimizedPermutation;

    @Unique
    private static double[] akiasync$createOptimizedGradients() {
        int[][] simplexGradients = {
                {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
                {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
                {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, -1, 1}, {0, -1, -1}
        };

        double[] gradients = new double[48];
        for (int i = 0; i < 16; i++) {
            gradients[i * 3] = simplexGradients[i][0];
            gradients[i * 3 + 1] = simplexGradients[i][1];
            gradients[i * 3 + 2] = simplexGradients[i][2];
        }

        return gradients;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void akiasync$onInit(RandomSource random, CallbackInfo ci) {

        this.akiasync$optimizedPermutation = new int[512];
        for (int i = 0; i < 256; i++) {
            this.akiasync$optimizedPermutation[i] = this.p[i] & 0xFF;
            this.akiasync$optimizedPermutation[i + 256] = this.p[i] & 0xFF;
        }
    }

    @Overwrite
    public double noise(double x, double y, double z, double yScale, double yMax) {
        if (!akiasync$isOptimizationEnabled()) {
            return akiasync$vanillaNoise(x, y, z, yScale, yMax);
        }

        double offsetX = x + this.xo;
        double offsetY = y + this.yo;
        double offsetZ = z + this.zo;

        int gridX = Mth.floor(offsetX);
        int gridY = Mth.floor(offsetY);
        int gridZ = Mth.floor(offsetZ);

        double deltaX = offsetX - gridX;
        double deltaY = offsetY - gridY;
        double deltaZ = offsetZ - gridZ;

        double yShift;
        if (yScale != 0.0) {
            double clampedY = yMax >= 0.0 && yMax < deltaY ? yMax : deltaY;
            yShift = Mth.floor(clampedY / yScale + 1.0E-7F) * yScale;
        } else {
            yShift = 0.0;
        }

        return akiasync$optimizedSampleAndLerp(
                gridX, gridY, gridZ,
                deltaX, deltaY - yShift, deltaZ, deltaY
        );
    }

    @Unique
    private double akiasync$optimizedSampleAndLerp(int gridX, int gridY, int gridZ,
                                                   double deltaX, double weirdDeltaY,
                                                   double deltaZ, double deltaY) {
        int idxX0 = this.akiasync$optimizedPermutation[gridX & 0xFF];
        int idxX1 = this.akiasync$optimizedPermutation[(gridX + 1) & 0xFF];

        int idxXY00 = this.akiasync$optimizedPermutation[(idxX0 + gridY) & 0xFF];
        int idxXY10 = this.akiasync$optimizedPermutation[(idxX1 + gridY) & 0xFF];
        int idxXY01 = this.akiasync$optimizedPermutation[(idxX0 + gridY + 1) & 0xFF];
        int idxXY11 = this.akiasync$optimizedPermutation[(idxX1 + gridY + 1) & 0xFF];

        int gradIdx000 = this.akiasync$optimizedPermutation[(idxXY00 + gridZ) & 0xFF] & 15;
        int gradIdx100 = this.akiasync$optimizedPermutation[(idxXY10 + gridZ) & 0xFF] & 15;
        int gradIdx010 = this.akiasync$optimizedPermutation[(idxXY01 + gridZ) & 0xFF] & 15;
        int gradIdx110 = this.akiasync$optimizedPermutation[(idxXY11 + gridZ) & 0xFF] & 15;
        int gradIdx001 = this.akiasync$optimizedPermutation[(idxXY00 + gridZ + 1) & 0xFF] & 15;
        int gradIdx101 = this.akiasync$optimizedPermutation[(idxXY10 + gridZ + 1) & 0xFF] & 15;
        int gradIdx011 = this.akiasync$optimizedPermutation[(idxXY01 + gridZ + 1) & 0xFF] & 15;
        int gradIdx111 = this.akiasync$optimizedPermutation[(idxXY11 + gridZ + 1) & 0xFF] & 15;

        double d0 = akiasync$optimizedGradDot(gradIdx000, deltaX, weirdDeltaY, deltaZ);
        double d1 = akiasync$optimizedGradDot(gradIdx100, deltaX - 1.0, weirdDeltaY, deltaZ);
        double d2 = akiasync$optimizedGradDot(gradIdx010, deltaX, weirdDeltaY - 1.0, deltaZ);
        double d3 = akiasync$optimizedGradDot(gradIdx110, deltaX - 1.0, weirdDeltaY - 1.0, deltaZ);
        double d4 = akiasync$optimizedGradDot(gradIdx001, deltaX, weirdDeltaY, deltaZ - 1.0);
        double d5 = akiasync$optimizedGradDot(gradIdx101, deltaX - 1.0, weirdDeltaY, deltaZ - 1.0);
        double d6 = akiasync$optimizedGradDot(gradIdx011, deltaX, weirdDeltaY - 1.0, deltaZ - 1.0);
        double d7 = akiasync$optimizedGradDot(gradIdx111, deltaX - 1.0, weirdDeltaY - 1.0, deltaZ - 1.0);

        double smoothX = akiasync$optimizedSmoothstep(deltaX);
        double smoothY = akiasync$optimizedSmoothstep(deltaY);
        double smoothZ = akiasync$optimizedSmoothstep(deltaZ);

        return Mth.lerp3(smoothX, smoothY, smoothZ, d0, d1, d2, d3, d4, d5, d6, d7);
    }

    @Unique
    private double akiasync$optimizedGradDot(int gradIdx, double x, double y, double z) {
        int baseIdx = gradIdx * 3;
        double gradX = akiasync$OPTIMIZED_GRADIENTS[baseIdx];
        double gradY = akiasync$OPTIMIZED_GRADIENTS[baseIdx + 1];
        double gradZ = akiasync$OPTIMIZED_GRADIENTS[baseIdx + 2];

        return gradX * x + gradY * y + gradZ * z;
    }

    @Unique
    private double akiasync$optimizedSmoothstep(double t) {
        double t3 = t * t * t;
        return t3 * (t * (t * 6.0 - 15.0) + 10.0);
    }

    @Unique
    private boolean akiasync$isOptimizationEnabled() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            return bridge != null && bridge.isNoiseOptimizationEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    @Unique
    private double akiasync$vanillaNoise(double x, double y, double z, double yScale, double yMax) {
        double offsetX = x + this.xo;
        double offsetY = y + this.yo;
        double offsetZ = z + this.zo;

        int gridX = Mth.floor(offsetX);
        int gridY = Mth.floor(offsetY);
        int gridZ = Mth.floor(offsetZ);

        double deltaX = offsetX - gridX;
        double deltaY = offsetY - gridY;
        double deltaZ = offsetZ - gridZ;

        double yShift;
        if (yScale != 0.0) {
            double clampedY = yMax >= 0.0 && yMax < deltaY ? yMax : deltaY;
            yShift = Mth.floor(clampedY / yScale + 1.0E-7F) * yScale;
        } else {
            yShift = 0.0;
        }

        return this.sampleAndLerp(gridX, gridY, gridZ, deltaX, deltaY - yShift, deltaZ, deltaY);
    }
}
