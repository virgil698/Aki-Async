package org.virgil.akiasync.mixin.mixins.entitytracker;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;


@Mixin(ChunkMap.class)
public class EntityTrackerLinkedHashMapMixin {

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean fieldExists = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }

        if (enabled && fieldExists) {
            akiasync$replaceEntityMap();
        }
    }

    @Unique
    private void akiasync$replaceEntityMap() {
        try {
            
            Field entityMapField = null;
            try {
                entityMapField = ChunkMap.class.getDeclaredField("entityMap");
            } catch (NoSuchFieldException e) {
                
                return;
            }
            
            entityMapField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Int2ObjectMap<ChunkMap.TrackedEntity> oldMap = 
                (Int2ObjectMap<ChunkMap.TrackedEntity>) entityMapField.get(this);
            
            
            Int2ObjectMap<ChunkMap.TrackedEntity> newMap = new Int2ObjectLinkedOpenHashMap<>();
            
            
            if (oldMap != null && !oldMap.isEmpty()) {
                newMap.putAll(oldMap);
            }
            
            
            entityMapField.set(this, newMap);
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityTrackerLinkedHashMap", "replaceEntityMap", e);
        }
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            
            try {
                Field field = ChunkMap.class.getDeclaredField("entityMap");
                fieldExists = (field != null);
            } catch (NoSuchFieldException e) {
                fieldExists = false;
            }
            
            if (!fieldExists) {
                enabled = false;
                
                try {
                    org.virgil.akiasync.mixin.bridge.Bridge bridge =
                        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null) {
                        bridge.debugLog("[EntityTrackerLinkedHashMap] Disabled: entityMap field not found (Folia/Luminol environment)");
                    }
                } catch (Exception e) {
                    
                }
            } else {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

                if (bridge != null) {
                    enabled = bridge.isEntityTrackerLinkedHashMapEnabled();
                    bridge.debugLog("[EntityTrackerLinkedHashMap] Initialized: enabled=%s", enabled);
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityTrackerLinkedHashMap", "initConfig", e);
        }

        initialized = true;
    }
}
