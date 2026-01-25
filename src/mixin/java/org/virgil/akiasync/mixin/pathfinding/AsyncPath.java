package org.virgil.akiasync.mixin.pathfinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class AsyncPath {

  private volatile PathState processState = PathState.WAITING;
  private final List<Runnable> postProcessing = new ArrayList<>(0);
  private final Set<BlockPos> positions;
  private final Supplier<Path> pathSupplier;
  private volatile Path delegatedPath;
  private final List<Node> emptyNodeList;

  public AsyncPath(List<Node> emptyNodeList, Set<BlockPos> positions, Supplier<Path> pathSupplier) {
    this.emptyNodeList = emptyNodeList;
    this.positions = positions;
    this.pathSupplier = pathSupplier;
    this.delegatedPath = new Path(emptyNodeList, null, false);

    AsyncPathProcessor.queue(this);
  }

  public Set<BlockPos> getPositions() {
    return positions;
  }

  public Supplier<Path> getPathSupplier() {
    return pathSupplier;
  }

  public boolean isProcessed() {
    return this.processState == PathState.COMPLETED;
  }

  public synchronized void postProcessing(Runnable runnable) {
    if (isProcessed()) {
      runnable.run();
    } else {
      this.postProcessing.add(runnable);
    }
  }

  public boolean hasSameProcessingPositions(final Set<BlockPos> positions) {
    if (this.positions.size() != positions.size()) {
      return false;
    }
    return this.positions.containsAll(positions);
  }

  public synchronized void process() {
    if (this.processState == PathState.COMPLETED || this.processState == PathState.PROCESSING) {
      return;
    }

    processState = PathState.PROCESSING;

    try {

      Path cachedPath = tryGetCachedPath();

      final Path bestPath;
      if (cachedPath != null) {
        bestPath = cachedPath;
      } else {

        bestPath = this.pathSupplier.get();

        if (bestPath != null && bestPath.canReach()) {
          cacheComputedPath(bestPath);
        }
      }

      if (bestPath != null) {
        this.delegatedPath = bestPath;
      }

      processState = PathState.COMPLETED;

      for (Runnable runnable : this.postProcessing) {
        try {
          runnable.run();
        } catch (Exception e) {
          org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
              "AsyncPath", "postProcessing", e);
        }
      }
    } catch (Exception e) {
      org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
          "AsyncPath", "process", e);
      processState = PathState.COMPLETED;
    }
  }

  private Path tryGetCachedPath() {
    if (positions == null || positions.isEmpty()) {
      return null;
    }

    BlockPos start = positions.iterator().next();
    BlockPos target = null;
    for (BlockPos pos : positions) {
      target = pos;
    }

    if (start != null && target != null && !start.equals(target)) {
      Path cached = SharedPathCache.getCachedPath(start, target);
      if (cached != null) {
        AsyncPathProcessor.recordCacheHit();
      }
      return cached;
    }

    return null;
  }

  private void cacheComputedPath(Path path) {
    if (positions == null || positions.isEmpty()) {
      return;
    }

    BlockPos start = positions.iterator().next();
    BlockPos target = null;
    for (BlockPos pos : positions) {
      target = pos;
    }

    if (start != null && target != null && !start.equals(target)) {
      SharedPathCache.cachePath(start, target, path);
    }
  }

  private void checkProcessed() {
    if (this.processState == PathState.WAITING) {
      long startTime = System.nanoTime();
      long timeoutNanos = getTimeoutNanos();

      while (this.processState == PathState.WAITING &&
             (System.nanoTime() - startTime) < timeoutNanos) {

        java.util.concurrent.locks.LockSupport.parkNanos(100_000);
      }

      if (this.processState == PathState.WAITING && shouldFallbackToSync()) {
        process();
      }
    }
  }

  private long getTimeoutNanos() {

    org.virgil.akiasync.mixin.bridge.Bridge bridge =
        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
    if (bridge != null) {
      return bridge.getAsyncPathfindingTimeoutMs() * 1_000_000L;
    }
    return 5_000_000L;
  }

  private boolean shouldFallbackToSync() {

    org.virgil.akiasync.mixin.bridge.Bridge bridge =
        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
    return bridge != null && bridge.isAsyncPathfindingSyncFallbackEnabled();
  }

  public Path getPath() {
    checkProcessed();
    return delegatedPath;
  }

  public BlockPos getTarget() {
    checkProcessed();
    return delegatedPath.getTarget();
  }

  public float getDistToTarget() {
    checkProcessed();
    return delegatedPath.getDistToTarget();
  }

  public boolean canReach() {
    checkProcessed();
    return delegatedPath.canReach();
  }

  public Node getEndNode() {
    checkProcessed();
    return delegatedPath.getEndNode();
  }

  public Node getNode(int index) {
    checkProcessed();
    return delegatedPath.getNode(index);
  }

  public int getNodeCount() {
    checkProcessed();
    return delegatedPath.getNodeCount();
  }

  public int getNextNodeIndex() {
    checkProcessed();
    return delegatedPath.getNextNodeIndex();
  }

  public Vec3 getEntityPosAtNode(net.minecraft.world.entity.Entity entity, int index) {
    checkProcessed();
    return delegatedPath.getEntityPosAtNode(entity, index);
  }

  public boolean isDone() {
    checkProcessed();
    return delegatedPath.isDone();
  }

  public void truncateNodes(int length) {
    checkProcessed();
    delegatedPath.truncateNodes(length);
  }

  public void replaceNode(int index, Node node) {
    checkProcessed();
    delegatedPath.replaceNode(index, node);
  }

  public void setNextNodeIndex(int index) {
    checkProcessed();
    delegatedPath.setNextNodeIndex(index);
  }

  public void advance() {
    checkProcessed();
    delegatedPath.advance();
  }

  public boolean notStarted() {
    checkProcessed();
    return delegatedPath.notStarted();
  }

  public boolean sameAs(Path other) {
    checkProcessed();
    return delegatedPath.sameAs(other);
  }
}
