package org.virgil.akiasync.mixin.util.math;

import net.minecraft.util.Mth;

public class CompactSineLUT {
    
    private static final int[] SINE_TABLE_INT = new int[16384 + 1];
    
    private static final float SINE_TABLE_MIDPOINT;
    
    private static boolean initialized = false;
    
    static {
        try {
            
            for (int i = 0; i < SINE_TABLE_INT.length; i++) {
                SINE_TABLE_INT[i] = Float.floatToRawIntBits(Mth.SIN[i]);
            }
            
            SINE_TABLE_MIDPOINT = Mth.SIN[Mth.SIN.length / 2];
            
            for (int i = 0; i < Mth.SIN.length; i++) {
                float expected = Mth.SIN[i];
                float value = lookup(i);
                
                if (expected != value) {
                    throw new IllegalArgumentException(
                        String.format("LUT error at index %d (expected: %s, found: %s)", 
                            i, expected, value));
                }
            }
            
            initialized = true;
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.BridgeConfigCache.errorLog("[AkiAsync] Failed to initialize CompactSineLUT: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("CompactSineLUT initialization failed", e);
        }
    }
    
    public static void init() {
        
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static float sin(float f) {
        return lookup((int) (f * 10430.378f) & 0xFFFF);
    }
    
    public static float cos(float f) {
        return lookup((int) (f * 10430.378f + 16384.0f) & 0xFFFF);
    }
    
    private static float lookup(int index) {
        
        if (index == 32768) {
            return SINE_TABLE_MIDPOINT;
        }
        
        int neg = (index & 0x8000) << 16;
        
        int mask = (index << 17) >> 31;
        
        int pos = (0x8001 & mask) + (index ^ mask);
        
        pos &= 0x7fff;
        
        return Float.intBitsToFloat(SINE_TABLE_INT[pos] ^ neg);
    }
    
    public static String getStats() {
        int originalSize = 65536; 
        int compactSize = SINE_TABLE_INT.length;
        int savedBytes = (originalSize - compactSize) * 4; 
        double reduction = (1.0 - (double) compactSize / originalSize) * 100;
        
        return String.format(
            "CompactSineLUT: %d entries (%.1f%% reduction, saved %d KB)",
            compactSize, reduction, savedBytes / 1024
        );
    }
}
