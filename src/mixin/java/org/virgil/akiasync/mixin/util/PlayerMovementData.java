package org.virgil.akiasync.mixin.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class PlayerMovementData {
    private double lastX, lastY, lastZ;
    private double velocityX, velocityZ;
    private long lastUpdateTime;
    private final Deque<double[]> positionHistory;

    public PlayerMovementData() {
        this.positionHistory = new ArrayDeque<>(10);
        this.lastUpdateTime = 0;
    }

    public void updatePosition(double x, double y, double z, long gameTime) {
        if (lastUpdateTime > 0) {
            long deltaTime = gameTime - lastUpdateTime;

            if (deltaTime > 0) {

                double deltaX = x - lastX;
                double deltaZ = z - lastZ;

                velocityX = deltaX / deltaTime;
                velocityZ = deltaZ / deltaTime;

                positionHistory.offer(new double[]{x, y, z, velocityX, velocityZ});
                if (positionHistory.size() > 10) {
                    positionHistory.poll();
                }
            }
        }

        lastX = x;
        lastY = y;
        lastZ = z;
        lastUpdateTime = gameTime;
    }

    public double getSpeed() {

        if (positionHistory.isEmpty()) {
            return Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        }

        int count = 0;
        double sumVx = 0;
        double sumVz = 0;

        for (double[] data : positionHistory) {
            if (count >= 3) break;
            sumVx += data[3];
            sumVz += data[4];
            count++;
        }

        if (count > 0) {
            double avgVx = sumVx / count;
            double avgVz = sumVz / count;
            return Math.sqrt(avgVx * avgVx + avgVz * avgVz);
        }

        return Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
    }

    public double[] predictPosition(int ticks) {

        double predictedX = lastX + velocityX * ticks;
        double predictedZ = lastZ + velocityZ * ticks;

        return new double[]{predictedX, lastY, predictedZ};
    }
    
    public double[] getVelocity() {
        return new double[]{velocityX, 0, velocityZ};
    }
}
