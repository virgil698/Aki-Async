package org.virgil.akiasync.mixin.bridge;

import org.virgil.akiasync.mixin.bridge.sub.*;

/**
 * Main Bridge interface that aggregates all sub-bridges.
 * This interface follows the Interface Segregation Principle (ISP) by
 * extending specialized sub-interfaces, allowing clients to depend only
 * on the interfaces they actually need.
 *
 * Sub-interfaces (in sub package):
 * - CoreBridge: Core functionality, executors, debugging
 * - EntityBridge: Entity-related optimizations
 * - NetworkBridge: Network optimizations
 * - LightingBridge: Lighting system
 * - TNTBridge: TNT/explosion optimizations
 * - ChunkBridge: Chunk-related optimizations
 * - PathfindingBridge: Pathfinding system
 * - StructureBridge: Structure location
 * - SeedBridge: Seed encryption
 * - CollisionBridge: Collision optimizations
 * - BlockEntityBridge: Block entity optimizations
 * - PerformanceBridge: Performance optimizations
 * - DataPackBridge: DataPack optimizations
 */
public interface Bridge extends
        CoreBridge,
        EntityBridge,
        NetworkBridge,
        LightingBridge,
        TNTBridge,
        ChunkBridge,
        PathfindingBridge,
        StructureBridge,
        SeedBridge,
        CollisionBridge,
        BlockEntityBridge,
        PerformanceBridge,
        DataPackBridge {

}
