package org.virgil.akiasync.mixin.util;

import java.lang.reflect.Field;

public final class REFLECTIONS {
    public static boolean setField(Object obj, String fieldName, Object value) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = null;
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
            ExceptionHandler.handleExpected("REFLECTIONS", "setField", 
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
            return false;
        }
    }
}
