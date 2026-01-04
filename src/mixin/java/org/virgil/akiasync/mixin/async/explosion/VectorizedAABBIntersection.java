package org.virgil.akiasync.mixin.async.explosion;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;
import net.minecraft.world.phys.AABB;
import org.virgil.akiasync.mixin.util.ExceptionHandler;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.function.BiConsumer;


public class VectorizedAABBIntersection {
    
    private static final Unsafe UNSAFE;
    private static final VectorSpecies<Float> SPECIES;
    private static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();
    private static volatile boolean vectorSupported = false;
    
    static {
        Unsafe tmpUnsafe = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            tmpUnsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            ExceptionHandler.handleExpected("VectorizedAABBIntersection", "initUnsafe", e);
        }
        UNSAFE = tmpUnsafe;
        
        VectorSpecies<Float> tmpSpecies = null;
        try {
            tmpSpecies = FloatVector.SPECIES_PREFERRED;
            vectorSupported = true;
        } catch (Exception e) {
            ExceptionHandler.handleExpected("VectorizedAABBIntersection", "initVectorAPI", e);
            vectorSupported = false;
        }
        SPECIES = tmpSpecies;
    }
    
    
    public static <T> void intersectsWithVector(
            float xmin, float ymin, float zmin,
            float xmax, float ymax, float zmax,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects,
            BiConsumer<AABB, T> consumer) {
        
        if (!vectorSupported || SPECIES == null) {
            
            intersectsWithVectorFallback(xmin, ymin, zmin, xmax, ymax, zmax,
                xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr,
                boxes, objects, consumer);
            return;
        }
        
        try {
            int length = boxes.length;
            int step = SPECIES.length();
            
            
            FloatVector qMinX = FloatVector.broadcast(SPECIES, xmin);
            FloatVector qMinY = FloatVector.broadcast(SPECIES, ymin);
            FloatVector qMinZ = FloatVector.broadcast(SPECIES, zmin);
            FloatVector qMaxX = FloatVector.broadcast(SPECIES, xmax);
            FloatVector qMaxY = FloatVector.broadcast(SPECIES, ymax);
            FloatVector qMaxZ = FloatVector.broadcast(SPECIES, zmax);
            
            int i = 0;
            int limit = length - (length % step);
            
            
            for (; i < limit; i += step) {
                
                FloatVector minX = FloatVector.fromArray(SPECIES, xminArr, i);
                FloatVector minY = FloatVector.fromArray(SPECIES, yminArr, i);
                FloatVector minZ = FloatVector.fromArray(SPECIES, zminArr, i);
                FloatVector maxX = FloatVector.fromArray(SPECIES, xmaxArr, i);
                FloatVector maxY = FloatVector.fromArray(SPECIES, ymaxArr, i);
                FloatVector maxZ = FloatVector.fromArray(SPECIES, zmaxArr, i);
                
                
                VectorMask<Float> xCond = minX.compare(VectorOperators.LE, qMaxX)
                    .and(maxX.compare(VectorOperators.GE, qMinX));
                VectorMask<Float> yCond = minY.compare(VectorOperators.LE, qMaxY)
                    .and(maxY.compare(VectorOperators.GE, qMinY));
                VectorMask<Float> zCond = minZ.compare(VectorOperators.LE, qMaxZ)
                    .and(maxZ.compare(VectorOperators.GE, qMinZ));
                
                VectorMask<Float> intersects = xCond.and(yCond).and(zCond);
                
                
                for (int j = 0; j < step && (i + j) < length; j++) {
                    if (intersects.laneIsSet(j) && boxes[i + j] != null && objects[i + j] != null) {
                        consumer.accept(boxes[i + j], objects[i + j]);
                    }
                }
            }
            
            
            for (; i < length; i++) {
                if (xminArr[i] <= xmax && xmaxArr[i] >= xmin &&
                    yminArr[i] <= ymax && ymaxArr[i] >= ymin &&
                    zminArr[i] <= zmax && zmaxArr[i] >= zmin &&
                    boxes[i] != null && objects[i] != null) {
                    consumer.accept(boxes[i], objects[i]);
                }
            }
            
        } catch (Exception e) {
            ExceptionHandler.handleExpected("VectorizedAABBIntersection", "intersectsWithVector", e);
            
            intersectsWithVectorFallback(xmin, ymin, zmin, xmax, ymax, zmax,
                xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr,
                boxes, objects, consumer);
        }
    }
    
    
    public static <T> void pointIntersectsVector(
            float x, float y, float z,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects,
            BiConsumer<AABB, T> consumer) {
        
        if (!vectorSupported || SPECIES == null) {
            pointIntersectsVectorFallback(x, y, z,
                xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr,
                boxes, objects, consumer);
            return;
        }
        
        try {
            int length = boxes.length;
            int step = SPECIES.length();
            
            FloatVector px = FloatVector.broadcast(SPECIES, x);
            FloatVector py = FloatVector.broadcast(SPECIES, y);
            FloatVector pz = FloatVector.broadcast(SPECIES, z);
            
            int i = 0;
            int limit = length - (length % step);
            
            for (; i < limit; i += step) {
                FloatVector minX = FloatVector.fromArray(SPECIES, xminArr, i);
                FloatVector minY = FloatVector.fromArray(SPECIES, yminArr, i);
                FloatVector minZ = FloatVector.fromArray(SPECIES, zminArr, i);
                FloatVector maxX = FloatVector.fromArray(SPECIES, xmaxArr, i);
                FloatVector maxY = FloatVector.fromArray(SPECIES, ymaxArr, i);
                FloatVector maxZ = FloatVector.fromArray(SPECIES, zmaxArr, i);
                
                VectorMask<Float> xCond = minX.compare(VectorOperators.LE, px)
                    .and(px.compare(VectorOperators.LE, maxX));
                VectorMask<Float> yCond = minY.compare(VectorOperators.LE, py)
                    .and(py.compare(VectorOperators.LE, maxY));
                VectorMask<Float> zCond = minZ.compare(VectorOperators.LE, pz)
                    .and(pz.compare(VectorOperators.LE, maxZ));
                
                VectorMask<Float> contains = xCond.and(yCond).and(zCond);
                
                for (int j = 0; j < step && (i + j) < length; j++) {
                    if (contains.laneIsSet(j) && boxes[i + j] != null && objects[i + j] != null) {
                        consumer.accept(boxes[i + j], objects[i + j]);
                    }
                }
            }
            
            for (; i < length; i++) {
                if (xminArr[i] <= x && x <= xmaxArr[i] &&
                    yminArr[i] <= y && y <= ymaxArr[i] &&
                    zminArr[i] <= z && z <= zmaxArr[i] &&
                    boxes[i] != null && objects[i] != null) {
                    consumer.accept(boxes[i], objects[i]);
                }
            }
            
        } catch (Exception e) {
            ExceptionHandler.handleExpected("VectorizedAABBIntersection", "pointIntersectsVector", e);
            pointIntersectsVectorFallback(x, y, z,
                xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr,
                boxes, objects, consumer);
        }
    }
    
    
    private static final int UNROLL_FACTOR = 4;
    
    private static <T> void intersectsWithVectorFallback(
            float xmin, float ymin, float zmin,
            float xmax, float ymax, float zmax,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects,
            BiConsumer<AABB, T> consumer) {
        
        int length = boxes.length;
        int i = 0;
        int unrolledLength = length - (length % UNROLL_FACTOR);
        
        for (; i < unrolledLength; i += UNROLL_FACTOR) {
            if (xminArr[i] <= xmax && xmaxArr[i] >= xmin &&
                yminArr[i] <= ymax && ymaxArr[i] >= ymin &&
                zminArr[i] <= zmax && zmaxArr[i] >= zmin &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }
            
            if (xminArr[i+1] <= xmax && xmaxArr[i+1] >= xmin &&
                yminArr[i+1] <= ymax && ymaxArr[i+1] >= ymin &&
                zminArr[i+1] <= zmax && zmaxArr[i+1] >= zmin &&
                boxes[i+1] != null && objects[i+1] != null) {
                consumer.accept(boxes[i+1], objects[i+1]);
            }
            
            if (xminArr[i+2] <= xmax && xmaxArr[i+2] >= xmin &&
                yminArr[i+2] <= ymax && ymaxArr[i+2] >= ymin &&
                zminArr[i+2] <= zmax && zmaxArr[i+2] >= zmin &&
                boxes[i+2] != null && objects[i+2] != null) {
                consumer.accept(boxes[i+2], objects[i+2]);
            }
            
            if (xminArr[i+3] <= xmax && xmaxArr[i+3] >= xmin &&
                yminArr[i+3] <= ymax && ymaxArr[i+3] >= ymin &&
                zminArr[i+3] <= zmax && zmaxArr[i+3] >= zmin &&
                boxes[i+3] != null && objects[i+3] != null) {
                consumer.accept(boxes[i+3], objects[i+3]);
            }
        }
        
        for (; i < length; i++) {
            if (xminArr[i] <= xmax && xmaxArr[i] >= xmin &&
                yminArr[i] <= ymax && ymaxArr[i] >= ymin &&
                zminArr[i] <= zmax && zmaxArr[i] >= zmin &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }
        }
    }
    
    private static <T> void pointIntersectsVectorFallback(
            float x, float y, float z,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects,
            BiConsumer<AABB, T> consumer) {
        
        int length = boxes.length;
        int i = 0;
        int unrolledLength = length - (length % UNROLL_FACTOR);
        
        for (; i < unrolledLength; i += UNROLL_FACTOR) {
            if (xminArr[i] <= x && x <= xmaxArr[i] &&
                yminArr[i] <= y && y <= ymaxArr[i] &&
                zminArr[i] <= z && z <= zmaxArr[i] &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }
            
            if (xminArr[i+1] <= x && x <= xmaxArr[i+1] &&
                yminArr[i+1] <= y && y <= ymaxArr[i+1] &&
                zminArr[i+1] <= z && z <= zmaxArr[i+1] &&
                boxes[i+1] != null && objects[i+1] != null) {
                consumer.accept(boxes[i+1], objects[i+1]);
            }
            
            if (xminArr[i+2] <= x && x <= xmaxArr[i+2] &&
                yminArr[i+2] <= y && y <= ymaxArr[i+2] &&
                zminArr[i+2] <= z && z <= zmaxArr[i+2] &&
                boxes[i+2] != null && objects[i+2] != null) {
                consumer.accept(boxes[i+2], objects[i+2]);
            }
            
            if (xminArr[i+3] <= x && x <= xmaxArr[i+3] &&
                yminArr[i+3] <= y && y <= ymaxArr[i+3] &&
                zminArr[i+3] <= z && z <= zmaxArr[i+3] &&
                boxes[i+3] != null && objects[i+3] != null) {
                consumer.accept(boxes[i+3], objects[i+3]);
            }
        }
        
        for (; i < length; i++) {
            if (xminArr[i] <= x && x <= xmaxArr[i] &&
                yminArr[i] <= y && y <= ymaxArr[i] &&
                zminArr[i] <= z && z <= zmaxArr[i] &&
                boxes[i] != null && objects[i] != null) {
                consumer.accept(boxes[i], objects[i]);
            }
        }
    }
    
    public static boolean isVectorSupported() {
        return vectorSupported;
    }
    
    public static String getOptimizationInfo() {
        return String.format("Loop unrolling optimization (factor=%d)", UNROLL_FACTOR);
    }
}

