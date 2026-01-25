package org.virgil.akiasync.mixin.util;

public final class ManhattanSearchOffsets {

    public static final int[] OFFSETS_8_4_8;
    public static final int OFFSET_COUNT_8_4_8;

    public static final int[] OFFSETS_5_1_5;
    public static final int OFFSET_COUNT_5_1_5;

    private ManhattanSearchOffsets() {}

    static {

        int[] result = computeOffsets(8, 4, 8);
        OFFSETS_8_4_8 = result;
        OFFSET_COUNT_8_4_8 = result.length / 3;

        result = computeOffsets(5, 1, 5);
        OFFSETS_5_1_5 = result;
        OFFSET_COUNT_5_1_5 = result.length / 3;
    }

    public static int[] computeOffsets(int maxX, int maxY, int maxZ) {
        final int maxDist = maxX + maxY + maxZ;

        int count = 0;
        for (int dist = 0; dist <= maxDist; dist++) {
            for (int dx = -Math.min(dist, maxX); dx <= Math.min(dist, maxX); dx++) {
                int remaining = dist - Math.abs(dx);
                for (int dy = -Math.min(remaining, maxY); dy <= Math.min(remaining, maxY); dy++) {
                    int dzAbs = remaining - Math.abs(dy);
                    if (dzAbs <= maxZ) {
                        count++;
                        if (dzAbs > 0) count++;
                    }
                }
            }
        }

        int[] offsets = new int[count * 3];
        int idx = 0;

        for (int dist = 0; dist <= maxDist; dist++) {
            for (int dx = -Math.min(dist, maxX); dx <= Math.min(dist, maxX); dx++) {
                int remaining = dist - Math.abs(dx);
                for (int dy = -Math.min(remaining, maxY); dy <= Math.min(remaining, maxY); dy++) {
                    int dzAbs = remaining - Math.abs(dy);
                    if (dzAbs <= maxZ) {
                        if (dzAbs == 0) {
                            offsets[idx++] = dx;
                            offsets[idx++] = dy;
                            offsets[idx++] = 0;
                        } else {

                            offsets[idx++] = dx;
                            offsets[idx++] = dy;
                            offsets[idx++] = dzAbs;

                            offsets[idx++] = dx;
                            offsets[idx++] = dy;
                            offsets[idx++] = -dzAbs;
                        }
                    }
                }
            }
        }

        return offsets;
    }
}
