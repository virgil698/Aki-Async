package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.lang.reflect.Method;

public final class SafeTargetSetter {

    private static Method setTargetWithReasonMethod;
    private static Class<?> targetReasonClass;
    private static boolean initialized = false;
    private static boolean available = false;

    static {
        tryInitialize();
    }

    private static void tryInitialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {

            targetReasonClass = Class.forName("org.bukkit.event.entity.EntityTargetEvent$TargetReason");
            setTargetWithReasonMethod = Mob.class.getMethod("setTarget",
                LivingEntity.class, targetReasonClass);
            available = true;
        } catch (Exception e) {

            available = false;
        }
    }

    public static void setTarget(Mob mob, LivingEntity target, String reasonName) {
        if (!available) {

            mob.setTarget(target);
            return;
        }

        try {
            Object reason = Enum.valueOf((Class<Enum>) targetReasonClass, reasonName);
            setTargetWithReasonMethod.invoke(mob, target, reason);
        } catch (Exception e) {

            mob.setTarget(target);
        }
    }

    public static void clearTarget(Mob mob) {
        setTarget(mob, null, "FORGOT_TARGET");
    }

    public static void setCustomTarget(Mob mob, LivingEntity target) {
        setTarget(mob, target, "CUSTOM");
    }

    public static void setClosestPlayer(Mob mob, LivingEntity target) {
        setTarget(mob, target, "CLOSEST_PLAYER");
    }

    private SafeTargetSetter() {
        throw new UnsupportedOperationException("Utility class");
    }
}
