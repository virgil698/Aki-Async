package org.virgil.akiasync.mixin.util;

import java.lang.reflect.Field;

/**
 * Safe reflection utility (避免static块崩溃)
 * 
 * Key difference from VarHandle approach:
 * - Lazy initialization (no static block)
 * - Try-catch on every setField call
 * - Fallback to no-op if field not found
 * 
 * @author Virgil
 */
public final class REFLECTIONS {
    
    /**
     * Safe set field (no exception thrown)
     * 
     * @param obj Target object
     * @param fieldName Field name
     * @param value Value to set
     * @return true if success, false if failed
     */
    public static boolean setField(Object obj, String fieldName, Object value) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = null;
            
            // Search in class and all parent classes
            while (clazz != null && field == null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (field != null) {
                field.setAccessible(true);
                field.set(obj, value);
                return true;
            }
            
            return false;
        } catch (Throwable t) {
            // Silent fail: keep vanilla behavior
            return false;
        }
    }
}

