package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class AsyncPath extends Path {

    private volatile PathProcessState processState = PathProcessState.WAITING;

    private final List<Runnable> postProcessing = new ArrayList<>(0);

    private final Set<BlockPos> positions;

    private final Supplier<Path> pathSupplier;

    private final List<Node> nodes;
    
    private BlockPos target;
    
    private float distToTarget = 0;
    
    private boolean canReach = true;

    public AsyncPath(List<Node> emptyNodeList, Set<BlockPos> positions, Supplier<Path> pathSupplier) {
        super(emptyNodeList, null, false);

        this.nodes = emptyNodeList;
        this.positions = positions;
        this.pathSupplier = pathSupplier;

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
                this.nodes.addAll(bestPath.nodes);
                this.target = bestPath.getTarget();
                this.distToTarget = bestPath.getDistToTarget();
                this.canReach = bestPath.canReach();
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

    @Override
    public BlockPos getTarget() {
        checkProcessed();
        return target != null ? target : BlockPos.ZERO;
    }

    @Override
    public float getDistToTarget() {
        checkProcessed();
        return distToTarget;
    }

    @Override
    public boolean canReach() {
        checkProcessed();
        return canReach;
    }

    @Override
    public Node getEndNode() {
        checkProcessed();
        return super.getEndNode();
    }

    @Override
    public Node getNode(int index) {
        checkProcessed();
        return super.getNode(index);
    }

    @Override
    public int getNodeCount() {
        checkProcessed();
        return super.getNodeCount();
    }

    @Override
    public int getNextNodeIndex() {
        checkProcessed();
        return super.getNextNodeIndex();
    }

    @Override
    public Vec3 getEntityPosAtNode(net.minecraft.world.entity.Entity entity, int index) {
        checkProcessed();
        return super.getEntityPosAtNode(entity, index);
    }

    @Override
    public boolean isDone() {
        checkProcessed();
        return super.isDone();
    }

    @Override
    public void truncateNodes(int length) {
        checkProcessed();
        super.truncateNodes(length);
    }

    @Override
    public void replaceNode(int index, Node node) {
        checkProcessed();
        super.replaceNode(index, node);
    }

    @Override
    public void setNextNodeIndex(int index) {
        checkProcessed();
        super.setNextNodeIndex(index);
    }

    @Override
    public void advance() {
        checkProcessed();
        super.advance();
    }

    @Override
    public boolean notStarted() {
        checkProcessed();
        return super.notStarted();
    }

    @Override
    public boolean sameAs(Path other) {
        checkProcessed();
        return super.sameAs(other);
    }
}
