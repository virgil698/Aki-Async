package org.virgil.akiasync.mixin.secureseed.duck;

public interface IWorldOptionsFeatureSeed {
    long[] secureSeed$featureSeed();
    
    void secureSeed$setFeatureSeed(long[] seed);
    
    String secureSeed$featureSeedSerialize();
}
