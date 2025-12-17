package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import javax.annotation.Nullable;

@Mixin(EnderMan.class)
public abstract class EndermanBlockCarryLimiterMixin extends Monster {

    @Shadow
    @Nullable
    public abstract BlockState getCarriedBlock();

    @Shadow
    public abstract void setCarriedBlock(@Nullable BlockState state);

    @Unique
    private static volatile boolean aki$initialized = false;
    @Unique
    private static volatile boolean aki$enabled = true;
    @Unique
    private static volatile int aki$maxCarryingEndermen = 50;
    @Unique
    private static volatile boolean aki$countTowardsMobCap = true;
    @Unique
    private static volatile boolean aki$preventPickup = true;

    protected EndermanBlockCarryLimiterMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
    private void aki$fixMobCapBug(CallbackInfoReturnable<Boolean> cir) {
        if (!aki$initialized) {
            aki$initConfig();
        }

        if (!aki$enabled || !aki$countTowardsMobCap) {
            return;
        }

        if (this.getCarriedBlock() != null) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static synchronized void aki$initConfig() {
        if (aki$initialized) {
            return;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            aki$enabled = bridge.isEndermanBlockCarryLimiterEnabled();
            aki$maxCarryingEndermen = bridge.getEndermanMaxCarrying();
            aki$countTowardsMobCap = bridge.isEndermanCountTowardsMobCap();
            aki$preventPickup = bridge.isEndermanPreventPickup();

            BridgeConfigCache.debugLog("[EndermanBlockCarryLimiter] Initialized:");
            BridgeConfigCache.debugLog("  - Enabled: " + aki$enabled);
            BridgeConfigCache.debugLog("  - Max carrying endermen: " + aki$maxCarryingEndermen);
            BridgeConfigCache.debugLog("  - Count towards mob cap: " + aki$countTowardsMobCap);
            BridgeConfigCache.debugLog("  - Prevent pickup when limit reached: " + aki$preventPickup);
        }

        aki$initialized = true;
    }

    @Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
    public static abstract class EndermanTakeBlockGoalMixin extends Goal {

        @Shadow
        private EnderMan enderman;

        @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
        private void aki$limitBlockPickup(CallbackInfoReturnable<Boolean> cir) {
            if (!aki$initialized) {
                aki$initConfig();
            }

            if (!aki$enabled || !aki$preventPickup) {
                return;
            }

            if (this.enderman.getCarriedBlock() != null) {
                return;
            }

            Level level = this.enderman.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            int carryingCount = aki$countCarryingEndermen(serverLevel);

            if (carryingCount >= aki$maxCarryingEndermen) {
                cir.setReturnValue(false);

                if (carryingCount == aki$maxCarryingEndermen && level.getGameTime() % 1200 == 0) {
                    BridgeConfigCache.debugLog(
                        "[EndermanBlockCarryLimiter] Prevented enderman from picking up block. " +
                        "Current carrying: " + carryingCount + "/" + aki$maxCarryingEndermen
                    );
                }
            }
        }

        @Unique
        private static int aki$countCarryingEndermen(ServerLevel level) {
            int count = 0;
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof EnderMan enderman && enderman.getCarriedBlock() != null) {
                    count++;
                }
            }
            return count;
        }
    }
}
