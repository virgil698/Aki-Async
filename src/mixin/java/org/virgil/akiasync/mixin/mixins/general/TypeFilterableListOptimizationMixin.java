package org.virgil.akiasync.mixin.mixins.general;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Mixin(value = ClassInstanceMultiMap.class, priority = 1005)
public abstract class TypeFilterableListOptimizationMixin<T> extends AbstractCollection<T> {

    @Mutable
    @Shadow @Final private Map<Class<?>, List<T>> byClass;

    @Mutable
    @Shadow @Final private List<T> allInstances;

    @Shadow @Final private Class<T> baseClass;

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Class<T> baseClass, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }

        if (enabled) {
            
            this.byClass = new Object2ObjectLinkedOpenHashMap<>();
            this.allInstances = new ObjectArrayList<>();
        }
    }

    
    @Overwrite
    public <S> Collection<S> find(Class<S> type) {
        if (!enabled) {
            
            return akiasync$findOriginal(type);
        }

        List<T> cached = this.byClass.get(type);
        if (cached != null) {
            return (Collection<S>) cached;
        }

        if (!this.baseClass.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Don't know how to search for " + type);
        }

        List<T> result = this.byClass.computeIfAbsent(type, typeClass -> {
            ObjectArrayList<T> filtered = new ObjectArrayList<>(this.allInstances.size());
            
            if (this.allInstances instanceof ObjectArrayList) {
                Object[] elements = ((ObjectArrayList<T>) this.allInstances).elements();
                int size = this.allInstances.size();
                for (int i = 0; i < size; i++) {
                    Object element = elements[i];
                    if (element != null && typeClass.isInstance(element)) {
                        filtered.add((T) element);
                    }
                }
            } else {
                for (T element : this.allInstances) {
                    if (typeClass.isInstance(element)) {
                        filtered.add(element);
                    }
                }
            }
            
            return filtered;
        });

        return (Collection<S>) result;
    }

    @Unique
    private <S> Collection<S> akiasync$findOriginal(Class<S> type) {
        if (!this.baseClass.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Don't know how to search for " + type);
        }

        List<T> list = this.byClass.computeIfAbsent(type, typeClass -> {
            List<T> filtered = new java.util.ArrayList<>();
            for (T element : this.allInstances) {
                if (typeClass.isInstance(element)) {
                    filtered.add(element);
                }
            }
            return filtered;
        });

        return (Collection<S>) list;
    }

    @Override
    public Iterator<T> iterator() {
        if (enabled && this.allInstances instanceof ObjectArrayList) {
            return ((ObjectArrayList<T>) this.allInstances).iterator();
        }
        return this.allInstances.iterator();
    }

    @Override
    public int size() {
        return this.allInstances.size();
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isTypeFilterableListOptimizationEnabled();
                bridge.debugLog("[TypeFilterableListOptimization] Initialized: enabled=%s", enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "TypeFilterableListOptimization", "initConfig", e);
        }

        initialized = true;
    }
}
