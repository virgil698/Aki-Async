package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

/**
 * Fix for MC-17876: Entities' health is capped at their base max health upon reload.
 * 
 * The bug occurs because in LivingEntity.readAdditionalSaveData():
 * 1. Line 878: Attributes are read
 * 2. Line 893: setHealth() is called - but equipment modifiers aren't applied yet
 * 3. Line 921: Equipment is read
 * 
 * When setHealth() is called, getMaxHealth() returns the base value (20) because
 * equipment attribute modifiers haven't been applied yet. This causes health to
 * be clamped to 20, losing any extra health from equipment.
 * 
 * This fix applies equipment attribute modifiers immediately after reading save data,
 * then restores the correct health value.
 */
@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthCapFixMixin {

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    @Shadow
    public abstract AttributeMap getAttributes();

    @Unique
    private static volatile boolean akiAsync$enabled = true;

    @Unique
    private static volatile boolean akiAsync$initialized = false;

    @Unique
    private static void akiAsync$init() {
        if (akiAsync$initialized) return;
        synchronized (LivingEntityHealthCapFixMixin.class) {
            if (akiAsync$initialized) return;
            try {
                var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                akiAsync$enabled = bridge.isEquipmentHealthCapFixEnabled();
                BridgeConfigCache.debugLog("[AkiAsync-HealthCapFix] Initialized: enabled=" + akiAsync$enabled);
            } catch (Exception e) {
                BridgeConfigCache.debugLog("[AkiAsync-HealthCapFix] Init failed, using default: " + e.getMessage());
            }
            akiAsync$initialized = true;
        }
    }

    @Unique
    private float akiAsync$savedHealth = -1.0F;

    /**
     * Capture the health value from NBT before it gets clamped by setHealth().
     */
    @Inject(
        method = "readAdditionalSaveData",
        at = @At("HEAD")
    )
    private void akiAsync$captureHealthBeforeRead(ValueInput input, CallbackInfo ci) {
        if (!akiAsync$initialized) {
            akiAsync$init();
        }
        if (!akiAsync$enabled) {
            return;
        }
        
        akiAsync$savedHealth = input.getFloatOr("Health", -1.0F);
    }

    /**
     * After readAdditionalSaveData completes, equipment has been loaded.
     * Apply equipment modifiers and restore the correct health value.
     */
    @Inject(
        method = "readAdditionalSaveData",
        at = @At("RETURN")
    )
    private void akiAsync$restoreHealthAfterRead(ValueInput input, CallbackInfo ci) {
        if (!akiAsync$enabled || akiAsync$savedHealth < 0) {
            akiAsync$savedHealth = -1.0F;
            return;
        }

        try {
            LivingEntity self = (LivingEntity) (Object) this;
            
            akiAsync$applyEquipmentModifiers(self);
            
            float newMaxHealth = self.getMaxHealth();
            float currentHealth = self.getHealth();
            
            if (akiAsync$savedHealth > currentHealth && akiAsync$savedHealth <= newMaxHealth) {
                self.setHealth(akiAsync$savedHealth);
                
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-HealthCapFix] Restored health for %s: %.1f -> %.1f (max: %.1f)",
                    self.getClass().getSimpleName(),
                    currentHealth,
                    akiAsync$savedHealth,
                    newMaxHealth
                ));
            }
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-HealthCapFix] Error restoring health: " + e.getMessage());
        } finally {
            akiAsync$savedHealth = -1.0F;
        }
    }

    /**
     * Apply attribute modifiers from all equipment slots.
     * This ensures max_health modifiers from equipment are applied before we restore health.
     */
    @Unique
    private void akiAsync$applyEquipmentModifiers(LivingEntity entity) {
        AttributeMap attributes = entity.getAttributes();
        
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = entity.getItemBySlot(slot);
            if (!itemStack.isEmpty() && !itemStack.isBroken()) {
                itemStack.forEachModifier(slot, (holder, modifier) -> {
                    AttributeInstance instance = attributes.getInstance(holder);
                    if (instance != null && !instance.hasModifier(modifier.id())) {
                        instance.addTransientModifier(modifier);
                    }
                });
            }
        }
    }
}
