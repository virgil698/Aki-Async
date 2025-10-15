package org.virgil.akiasync.mixin.brain.witch;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Witch differential (v2.1: printStackTrace + no rethrow + 全栈)
 */
public final class WitchDiff {
    private static final Field TARGET_FIELD;
    
    static {
        Field temp = null;
        try {
            // 方案1：直接getDeclaredField("target")
            temp = Mob.class.getDeclaredField("target");
            temp.setAccessible(true);
        } catch (Throwable t) {
            // ① 打印完整堆栈
            t.printStackTrace();
            // ② 不 rethrow，异常被吞掉
            // ③ 后续NPE时会重新打印，但static块不崩溃
        }
        TARGET_FIELD = temp;
    }
    
    private UUID witchTarget;
    private int changeCount;
    
    public WitchDiff() {}
    public void setWitchTarget(UUID id) { this.witchTarget = id; changeCount++; }
    
    public void applyTo(net.minecraft.world.entity.monster.Witch witch, ServerLevel level) {
        if (TARGET_FIELD == null) return;  // 防御性：字段未找到时跳过
        
        if (witchTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(witchTarget);
            if (player != null && !player.isRemoved()) {
                try {
                    TARGET_FIELD.set(witch, player);
                } catch (Throwable t) {
                    // v2.1热回退：只跑CPU
                    t.printStackTrace();
                }
            }
        }
    }
    
    public boolean hasChanges() { return changeCount > 0; }
}

