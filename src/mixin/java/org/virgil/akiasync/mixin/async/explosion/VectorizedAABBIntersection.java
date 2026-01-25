package org.virgil.akiasync.mixin.async.explosion;

import net.minecraft.world.phys.AABB;
import java.util.function.BiConsumer;

public class VectorizedAABBIntersection {

    private static final int UNROLL_FACTOR = 4;

    public static <T> void intersectsWithVector(
            float xmin, float ymin, float zmin,
            float xmax, float ymax, float zmax,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects,
            BiConsumer<AABB, T> consumer) {

        int length = boxes.length;
        int i = 0;
        int unrolledLength = length - (length % UNROLL_FACTOR);

        for (; i < unrolledLength; i += UNROLL_FACTOR) {
            if (xminArr[i] <= xmax && xmaxArr[i] >= xmin &&
                yminArr[i] <= ymax && ymaxArr[i] >= ymin &&
                zminArr[i] <= zmax && zmaxArr[i] >= zmin &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }

            if (xminArr[i+1] <= xmax && xmaxArr[i+1] >= xmin &&
                yminArr[i+1] <= ymax && ymaxArr[i+1] >= ymin &&
                zminArr[i+1] <= zmax && zmaxArr[i+1] >= zmin &&
                boxes[i+1] != null && objects[i+1] != null) {
                consumer.accept(boxes[i+1], objects[i+1]);
            }

            if (xminArr[i+2] <= xmax && xmaxArr[i+2] >= xmin &&
                yminArr[i+2] <= ymax && ymaxArr[i+2] >= ymin &&
                zminArr[i+2] <= zmax && zmaxArr[i+2] >= zmin &&
                boxes[i+2] != null && objects[i+2] != null) {
                consumer.accept(boxes[i+2], objects[i+2]);
            }

            if (xminArr[i+3] <= xmax && xmaxArr[i+3] >= xmin &&
                yminArr[i+3] <= ymax && ymaxArr[i+3] >= ymin &&
                zminArr[i+3] <= zmax && zmaxArr[i+3] >= zmin &&
                boxes[i+3] != null && objects[i+3] != null) {
                consumer.accept(boxes[i+3], objects[i+3]);
            }
        }

        for (; i < length; i++) {
            if (xminArr[i] <= xmax && xmaxArr[i] >= xmin &&
                yminArr[i] <= ymax && ymaxArr[i] >= ymin &&
                zminArr[i] <= zmax && zmaxArr[i] >= zmin &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }
        }
    }

    public static String getOptimizationInfo() {
        return String.format("Scalar with loop unrolling (factor=%d) - Legacy fallback", UNROLL_FACTOR);
    }
}
