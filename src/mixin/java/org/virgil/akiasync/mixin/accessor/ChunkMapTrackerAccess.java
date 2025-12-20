package org.virgil.akiasync.mixin.accessor;

/**
 * ChunkMap 访问接口
 * 
 * 用于在 ServerEntity 中访问 ChunkMap 的 runOnTrackerMainThread 方法
 * 
 * @author AkiAsync
 */
public interface ChunkMapTrackerAccess {
    
    void aki$runOnTrackerMainThread(Runnable runnable);
}
