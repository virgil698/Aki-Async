package org.virgil.akiasync.mixin.util.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.virgil.akiasync.mixin.async.explosion.VectorizedAABBIntersection;

import java.util.ArrayList;
import java.util.List;

public class VectorizedCollisionDetector {
    
    private static final int VECTORIZATION_THRESHOLD = 64;
    
    public static List<Entity> filterCollisions(AABB searchBox, List<Entity> entities) {
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (entities.size() < VECTORIZATION_THRESHOLD) {
            return filterTraditional(searchBox, entities);
        }
        
        return filterVectorized(searchBox, entities);
    }
    
    private static List<Entity> filterTraditional(AABB searchBox, List<Entity> entities) {
        List<Entity> result = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (entity != null && !entity.isRemoved()) {
                AABB entityBox = entity.getBoundingBox();
                if (entityBox.intersects(searchBox)) {
                    result.add(entity);
                }
            }
        }
        
        return result;
    }
    
    private static List<Entity> filterVectorized(AABB searchBox, List<Entity> entities) {
        int size = entities.size();
        
        float[] xminArr = new float[size];
        float[] yminArr = new float[size];
        float[] zminArr = new float[size];
        float[] xmaxArr = new float[size];
        float[] ymaxArr = new float[size];
        float[] zmaxArr = new float[size];
        
        AABB[] boxes = new AABB[size];
        Entity[] entityArray = new Entity[size];
        
        for (int i = 0; i < size; i++) {
            Entity entity = entities.get(i);
            if (entity != null && !entity.isRemoved()) {
                AABB box = entity.getBoundingBox();
                
                xminArr[i] = (float) box.minX;
                yminArr[i] = (float) box.minY;
                zminArr[i] = (float) box.minZ;
                xmaxArr[i] = (float) box.maxX;
                ymaxArr[i] = (float) box.maxY;
                zmaxArr[i] = (float) box.maxZ;
                
                boxes[i] = box;
                entityArray[i] = entity;
            } else {
                xminArr[i] = Float.MAX_VALUE;
                yminArr[i] = Float.MAX_VALUE;
                zminArr[i] = Float.MAX_VALUE;
                xmaxArr[i] = Float.MIN_VALUE;
                ymaxArr[i] = Float.MIN_VALUE;
                zmaxArr[i] = Float.MIN_VALUE;
                
                boxes[i] = null;
                entityArray[i] = null;
            }
        }
        
        List<Entity> result = new ArrayList<>();
        
        VectorizedAABBIntersection.intersectsWithVector(
            (float) searchBox.minX, (float) searchBox.minY, (float) searchBox.minZ,
            (float) searchBox.maxX, (float) searchBox.maxY, (float) searchBox.maxZ,
            xminArr, yminArr, zminArr,
            xmaxArr, ymaxArr, zmaxArr,
            boxes, entityArray,
            (box, entity) -> {
                if (entity != null) {
                    result.add(entity);
                }
            }
        );
        
        return result;
    }
    
    public static List<Entity> findEntitiesAtPoint(double x, double y, double z, List<Entity> entities) {
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (entities.size() < VECTORIZATION_THRESHOLD) {
            return findAtPointTraditional(x, y, z, entities);
        }
        
        return findAtPointVectorized(x, y, z, entities);
    }
    
    private static List<Entity> findAtPointTraditional(double x, double y, double z, List<Entity> entities) {
        List<Entity> result = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (entity != null && !entity.isRemoved()) {
                AABB box = entity.getBoundingBox();
                if (box.contains(x, y, z)) {
                    result.add(entity);
                }
            }
        }
        
        return result;
    }
    
    private static List<Entity> findAtPointVectorized(double x, double y, double z, List<Entity> entities) {
        int size = entities.size();
        
        float[] xminArr = new float[size];
        float[] yminArr = new float[size];
        float[] zminArr = new float[size];
        float[] xmaxArr = new float[size];
        float[] ymaxArr = new float[size];
        float[] zmaxArr = new float[size];
        
        AABB[] boxes = new AABB[size];
        Entity[] entityArray = new Entity[size];
        
        for (int i = 0; i < size; i++) {
            Entity entity = entities.get(i);
            if (entity != null && !entity.isRemoved()) {
                AABB box = entity.getBoundingBox();
                
                xminArr[i] = (float) box.minX;
                yminArr[i] = (float) box.minY;
                zminArr[i] = (float) box.minZ;
                xmaxArr[i] = (float) box.maxX;
                ymaxArr[i] = (float) box.maxY;
                zmaxArr[i] = (float) box.maxZ;
                
                boxes[i] = box;
                entityArray[i] = entity;
            } else {
                xminArr[i] = Float.MAX_VALUE;
                yminArr[i] = Float.MAX_VALUE;
                zminArr[i] = Float.MAX_VALUE;
                xmaxArr[i] = Float.MIN_VALUE;
                ymaxArr[i] = Float.MIN_VALUE;
                zmaxArr[i] = Float.MIN_VALUE;
                
                boxes[i] = null;
                entityArray[i] = null;
            }
        }
        
        List<Entity> result = new ArrayList<>();
        
        VectorizedAABBIntersection.pointIntersectsVector(
            (float) x, (float) y, (float) z,
            xminArr, yminArr, zminArr,
            xmaxArr, ymaxArr, zmaxArr,
            boxes, entityArray,
            (box, entity) -> {
                if (entity != null) {
                    result.add(entity);
                }
            }
        );
        
        return result;
    }
    
    public static int getVectorizationThreshold() {
        return VECTORIZATION_THRESHOLD;
    }
    
    public static String getOptimizationInfo() {
        return String.format(
            "VectorizedCollisionDetector: threshold=%d, using %s",
            VECTORIZATION_THRESHOLD,
            VectorizedAABBIntersection.getOptimizationInfo()
        );
    }
}
