package org.virgil.akiasync.util.worldgen;

public class OctreeHolder {
    private static final ThreadLocal<BoxOctree> OCTREE = new ThreadLocal<>();
    
    public static BoxOctree get() {
        return OCTREE.get();
    }
    
    public static void set(BoxOctree octree) {
        OCTREE.set(octree);
    }
    
    public static void clear() {
        BoxOctree octree = OCTREE.get();
        if (octree != null) {
            octree.clear();
        }
        OCTREE.remove();
    }
    
    public static boolean isSet() {
        return OCTREE.get() != null;
    }
}
