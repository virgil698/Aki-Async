package org.virgil.akiasync.mixin.bridge.sub;

public interface SeedBridge {

    boolean isSeedProtectionEnabled();
    boolean shouldReturnFakeSeed();
    long getFakeSeedValue();

    boolean isQuantumSeedEnabled();
    byte[] getQuantumServerKey();
    long getEncryptedSeed(long originalSeed, int chunkX, int chunkZ, String dimension, String generationType, long gameTime);

    boolean isSecureSeedEnabled();
    long[] getSecureSeedWorldSeed();
    void initializeSecureSeed(long originalSeed);
    int getSecureSeedBits();

    boolean isSeedEncryptionProtectStructures();
    boolean isSeedEncryptionProtectOres();
    boolean isSeedEncryptionProtectSlimes();
    boolean isSeedEncryptionProtectBiomes();
}
