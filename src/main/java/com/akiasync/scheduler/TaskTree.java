package com.akiasync.scheduler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TaskTree<T> {
    private final Seal seal;
    private final TaskNode<T> root;

    private TaskTree(String name, SchedulerPriority priority, RootAction<T> action) {
        seal = new Seal();
        root = new TaskNode<>(seal, name, priority, action::run);
    }

    public static <T> TaskTree<T> root(String name, SchedulerPriority priority, RootAction<T> action) {
        return new TaskTree<>(name, priority, Objects.requireNonNull(action, "action"));
    }

    public TaskNode<T> root() {
        return root;
    }

    PreparedTree<T> prepare() {
        synchronized (seal) {
            seal.close();
            List<TaskNode<?>> nodes = new ArrayList<>();
            ArrayDeque<TaskNode<?>> pending = new ArrayDeque<>();
            pending.push(root);
            while (!pending.isEmpty()) {
                TaskNode<?> node = pending.pop();
                nodes.add(node);
                List<TaskNode<?>> children = node.children();
                for (int index = children.size() - 1; index >= 0; index--) {
                    pending.push(children.get(index));
                }
            }
            return new PreparedTree<>(root, List.copyOf(nodes));
        }
    }

    @FunctionalInterface
    public interface RootAction<T> {
        T run(TaskExecutionContext context) throws Exception;
    }

    record PreparedTree<T>(TaskNode<T> root, List<TaskNode<?>> nodes) {
    }

    static final class Seal {
        private boolean closed;

        void requireOpen() {
            if (closed) {
                throw new IllegalStateException("Task tree is already submitted");
            }
        }

        void close() {
            requireOpen();
            closed = true;
        }
    }
}
