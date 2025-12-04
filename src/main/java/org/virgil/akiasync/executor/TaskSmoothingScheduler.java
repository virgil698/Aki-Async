package org.virgil.akiasync.executor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class TaskSmoothingScheduler {
    

    public enum Priority {
        CRITICAL(0),
        HIGH(1),
        NORMAL(2),
        LOW(3);
        
        private final int level;
        Priority(int level) { this.level = level; }
        public int getLevel() { return level; }
    }
    

    private static class SmoothTask implements Comparable<SmoothTask> {
        final Runnable task;
        final Priority priority;
        final long submitTime;
        final String category;
        
        SmoothTask(Runnable task, Priority priority, String category) {
            this.task = task;
            this.priority = priority;
            this.submitTime = System.nanoTime();
            this.category = category;
        }
        
        @Override
        public int compareTo(SmoothTask other) {

            int priorityCompare = Integer.compare(this.priority.level, other.priority.level);
            if (priorityCompare != 0) return priorityCompare;

            return Long.compare(this.submitTime, other.submitTime);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SmoothTask other = (SmoothTask) obj;
            return submitTime == other.submitTime && 
                   priority == other.priority &&
                   task == other.task;
        }
        
        @Override
        public int hashCode() {
            int result = priority.hashCode();
            result = 31 * result + Long.hashCode(submitTime);
            result = 31 * result + (task != null ? System.identityHashCode(task) : 0);
            return result;
        }
    }
    

    private final int maxQueueSize;
    private final int maxTasksPerTick;
    private final int smoothingWindowTicks;
    private final ExecutorService executor;
    

    private final PriorityBlockingQueue<SmoothTask> taskQueue;
    

    private final AtomicLong totalSubmitted = new AtomicLong(0);
    private final AtomicLong totalExecuted = new AtomicLong(0);
    private final AtomicLong totalDropped = new AtomicLong(0);
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);
    

    private final AtomicInteger tasksThisTick = new AtomicInteger(0);
    

    private volatile double currentTPS = 20.0;
    private volatile double currentMSPT = 50.0;
    

    private final ConcurrentHashMap<String, AtomicLong> categoryStats = new ConcurrentHashMap<>();
    
    public TaskSmoothingScheduler(ExecutorService executor, int maxQueueSize, 
                                  int maxTasksPerTick, int smoothingWindowTicks) {
        this.executor = executor;
        this.maxQueueSize = maxQueueSize;
        this.maxTasksPerTick = maxTasksPerTick;
        this.smoothingWindowTicks = smoothingWindowTicks;
        this.taskQueue = new PriorityBlockingQueue<>(maxQueueSize);
        

        startSchedulerThread();
    }
    
    
    public boolean submit(Runnable task, Priority priority, String category) {
        if (task == null) return false;
        
        totalSubmitted.incrementAndGet();
        

        if (priority == Priority.LOW && AdaptiveLoadBalancer.shouldSkipLowPriority()) {
            totalDropped.incrementAndGet();
            return false;
        }
        

        if (priority != Priority.CRITICAL && !AdaptiveLoadBalancer.shouldSubmitTask()) {
            totalDropped.incrementAndGet();
            return false;
        }
        

        if (currentQueueSize.get() >= maxQueueSize) {
            if (priority == Priority.CRITICAL) {

                SmoothTask removed = removeLowestPriorityTask();
                if (removed != null) {
                    totalDropped.incrementAndGet();
                }
            } else {

                totalDropped.incrementAndGet();
                return false;
            }
        }
        
        SmoothTask smoothTask = new SmoothTask(task, priority, category);
        boolean added = taskQueue.offer(smoothTask);
        
        if (added) {
            currentQueueSize.incrementAndGet();
            categoryStats.computeIfAbsent(category, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            totalDropped.incrementAndGet();
        }
        
        return added;
    }
    
    
    public boolean submit(Runnable task, String category) {
        return submit(task, Priority.NORMAL, category);
    }
    
    
    public void updatePerformanceMetrics(double tps, double mspt) {
        this.currentTPS = tps;
        this.currentMSPT = mspt;
        

        AdaptiveLoadBalancer.updateMspt(mspt);
    }
    
    
    public void onTick() {
        tasksThisTick.set(0);
    }
    
    
    private void startSchedulerThread() {
        Thread schedulerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    if (canSubmitMoreTasks()) {
                        SmoothTask task = taskQueue.poll(10, TimeUnit.MILLISECONDS);
                        
                        if (task != null) {
                            currentQueueSize.decrementAndGet();
                            

                            executor.execute(() -> {
                                try {
                                    task.task.run();
                                    totalExecuted.incrementAndGet();
                                } catch (Throwable t) {

                                }
                            });
                            
                            tasksThisTick.incrementAndGet();
                        }
                    } else {

                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {

                }
            }
        }, "AkiAsync-TaskSmoothing");
        
        schedulerThread.setDaemon(true);
        schedulerThread.setPriority(Thread.NORM_PRIORITY - 1);
        schedulerThread.start();
    }
    
    
    private boolean canSubmitMoreTasks() {

        if (tasksThisTick.get() >= getAdaptiveMaxTasksPerTick()) {
            return false;
        }
        

        if (taskQueue.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    
    private int getAdaptiveMaxTasksPerTick() {

        if (currentTPS >= 19.5) {

            return (int) (maxTasksPerTick * 1.5);
        } else if (currentTPS >= 18.0) {

            return maxTasksPerTick;
        } else if (currentTPS >= 15.0) {

            return (int) (maxTasksPerTick * 0.7);
        } else {

            return (int) (maxTasksPerTick * 0.5);
        }
    }
    
    
    private SmoothTask removeLowestPriorityTask() {

        SmoothTask lowest = null;
        for (SmoothTask task : taskQueue) {
            if (task.priority == Priority.LOW) {
                if (taskQueue.remove(task)) {
                    currentQueueSize.decrementAndGet();
                    return task;
                }
            }
        }
        return null;
    }
    
    
    public String getStatistics() {
        return String.format(
            "TaskSmoothing: Queue=%d/%d | Submitted=%d | Executed=%d | Dropped=%d | Rate=%d/%d/tick | TPS=%.1f | MSPT=%.1f",
            currentQueueSize.get(),
            maxQueueSize,
            totalSubmitted.get(),
            totalExecuted.get(),
            totalDropped.get(),
            tasksThisTick.get(),
            getAdaptiveMaxTasksPerTick(),
            currentTPS,
            currentMSPT
        );
    }
    
    
    public String getCategoryStatistics() {
        StringBuilder sb = new StringBuilder("Category Stats:\n");
        categoryStats.forEach((category, count) -> {
            sb.append(String.format("  - %s: %d\n", category, count.get()));
        });
        return sb.toString();
    }
    
    
    public void resetStatistics() {
        totalSubmitted.set(0);
        totalExecuted.set(0);
        totalDropped.set(0);
        categoryStats.clear();
    }
    
    
    public int getQueueSize() {
        return currentQueueSize.get();
    }
    
    
    public double getQueueUsagePercent() {
        return (currentQueueSize.get() * 100.0) / maxQueueSize;
    }
    
    
    public void clearQueue() {
        taskQueue.clear();
        currentQueueSize.set(0);
    }
}
