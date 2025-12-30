package org.virgil.akiasync.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public class ByteBufOptimizer {
    
    private static boolean usePooledAllocator = true;
    private static boolean preferDirectBuffer = true;
    private static ByteBufAllocator allocator;
    
    public static void initialize(boolean pooled, boolean direct) {
        usePooledAllocator = pooled;
        preferDirectBuffer = direct;
        
        if (usePooledAllocator) {
            allocator = PooledByteBufAllocator.DEFAULT;
        } else {
            allocator = UnpooledByteBufAllocator.DEFAULT;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[ByteBufOptimizer] Initialized - pooled=%s, direct=%s", 
                pooled, direct);
        }
    }
    
    public static ByteBufAllocator getAllocator() {
        if (allocator == null) {
            initialize(true, true);
        }
        return allocator;
    }
    
    public static ByteBuf allocate(int initialCapacity) {
        ByteBufAllocator alloc = getAllocator();
        
        if (preferDirectBuffer) {
            return alloc.directBuffer(initialCapacity);
        } else {
            return alloc.heapBuffer(initialCapacity);
        }
    }
    
    public static ByteBuf allocate(int initialCapacity, int maxCapacity) {
        ByteBufAllocator alloc = getAllocator();
        
        if (preferDirectBuffer) {
            return alloc.directBuffer(initialCapacity, maxCapacity);
        } else {
            return alloc.heapBuffer(initialCapacity, maxCapacity);
        }
    }
    
    public static ByteBuf allocateForCompression(int uncompressedSize) {
        int estimatedSize = Math.max(256, uncompressedSize / 2);
        
        return getAllocator().directBuffer(estimatedSize, uncompressedSize + 512);
    }
    
    public static ByteBuf allocateForDecompression(int expectedUncompressedSize) {
        return getAllocator().directBuffer(expectedUncompressedSize);
    }
    
    public static ByteBuf allocateForVarInt(int valueCount) {
        int estimatedSize = valueCount * 2;
        int maxSize = valueCount * 5;
        
        return allocate(estimatedSize, maxSize);
    }
    
    public static ByteBuf allocateForString(String string) {
        int estimatedSize = string.length() * 3 + 5;
        
        return allocate(estimatedSize);
    }
    
    public static ByteBuf allocateForNBT(int estimatedSize) {
        int initialSize = Math.max(1024, estimatedSize);
        int maxSize = Math.max(8192, estimatedSize * 2);
        
        return allocate(initialSize, maxSize);
    }
    
    public static void ensureWritable(ByteBuf buf, int additionalBytes) {
        if (buf.writableBytes() >= additionalBytes) {
            return;
        }
        
        int currentCapacity = buf.capacity();
        int requiredCapacity = buf.writerIndex() + additionalBytes;
        
        int newCapacity;
        if (currentCapacity < 4096) {
            newCapacity = Math.max(requiredCapacity, currentCapacity * 2);
        } else {
            newCapacity = Math.max(requiredCapacity, currentCapacity + currentCapacity / 2);
        }
        
        newCapacity = Math.min(newCapacity, buf.maxCapacity());
        
        buf.capacity(newCapacity);
    }
    
    public static void safeRelease(ByteBuf buf) {
        if (buf != null && buf.refCnt() > 0) {
            try {
                buf.release();
            } catch (Exception e) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog("[ByteBufOptimizer] Failed to release buffer: %s", 
                        e.getMessage());
                }
            }
        }
    }
    
    public static ByteBuf copy(ByteBuf source) {
        ByteBuf copy = allocate(source.readableBytes());
        copy.writeBytes(source, source.readerIndex(), source.readableBytes());
        return copy;
    }
    
    public static boolean isDirect(ByteBuf buf) {
        return buf != null && buf.isDirect();
    }
    
    public static String getStats() {
        if (allocator instanceof PooledByteBufAllocator) {
            PooledByteBufAllocator pooled = (PooledByteBufAllocator) allocator;
            return String.format("Pooled allocator stats - Used: %d MB, Pooled: %s, Direct: %s",
                pooled.metric().usedDirectMemory() / 1024 / 1024,
                usePooledAllocator,
                preferDirectBuffer);
        } else {
            return String.format("Unpooled allocator - Pooled: %s, Direct: %s",
                usePooledAllocator,
                preferDirectBuffer);
        }
    }
}
