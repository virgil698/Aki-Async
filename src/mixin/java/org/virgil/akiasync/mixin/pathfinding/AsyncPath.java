package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * AsyncPath wrapper for Path to support async pathfinding.
 * Changed from inheritance to delegation to support Leaves 1.21.10+ where Path is final.
 */
public class AsyncPath {

    private volatile PathProcessState processState = PathProcessState.WAITING;

    private final List<Runnable> postProcessing = new ArrayList<>(0);

    private final Set<BlockPos> positions;

    private final Supplier<Path> pathSupplier;

    // Delegated path object
    private volatile Path delegatedPath;
    
    // Temporary storage before processing
    private final List<Node> emptyNodeList;

    public AsyncPath(List<Node> emptyNodeList, Set<BlockPos> positions, Supplier<Path> pathSupplier) {
        this.emptyNodeList = emptyNodeList;
        this.positions = positions;
        this.pathSupplier = pathSupplier;
        
        // Create a dummy path initially
        this.delegatedPath = new Path(emptyNodeList, null, false);

        AsyncPathProcessor.queue(this);
    }

    public boolean isProcessed() {
        return this.processState == PathProcessState.COMPLETED;
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
        if (this.processState == PathProcessState.COMPLETED ||
                this.processState == PathProcessState.PROCESSING) {
            return;
        }

        processState = PathProcessState.PROCESSING;

        try {
            final Path bestPath = this.pathSupplier.get();

            if (bestPath != null) {
                // Replace the delegated path with the computed one
                this.delegatedPath = bestPath;
            }

            processState = PathProcessState.COMPLETED;

            for (Runnable runnable : this.postProcessing) {
                try {
                    runnable.run();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            processState = PathProcessState.COMPLETED;
        }
    }

    private void checkProcessed() {
        if (this.processState == PathProcessState.WAITING ||
                this.processState == PathProcessState.PROCESSING) {
            this.process();
        }
    }

    // Delegation methods - forward all calls to delegatedPath after processing
    
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
