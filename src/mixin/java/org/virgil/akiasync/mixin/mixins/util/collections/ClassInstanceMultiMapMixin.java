package org.virgil.akiasync.mixin.mixins.util.collections;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.AbstractCollection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;

@Mixin(value = ClassInstanceMultiMap.class)
public abstract class ClassInstanceMultiMapMixin<T> extends AbstractCollection<T> {

    @Unique
    private static final Object akiasync$lock = new Object();

    @Shadow
    private final Map<Class<?>, List<T>> byClass = new ConcurrentHashMap<>();

    @Shadow
    private final List<T> allInstances = new CopyOnWriteArrayList<>();

    @ModifyArg(method = {"lambda$find$0", "m_13537_"}, at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;"))
    private Collector<T, ?, List<T>> akiasync$overwriteCollectToList(Collector<T, ?, List<T>> collector) {
        return java.util.stream.Collectors.toList();
    }

    @WrapMethod(method = "add")
    private boolean akiasync$add(Object e, Operation<Boolean> original) {
        synchronized (akiasync$lock) {
            return original.call(e);
        }
    }

    @WrapMethod(method = "remove")
    private boolean akiasync$remove(Object o, Operation<Boolean> original) {
        synchronized (akiasync$lock) {
            return original.call(o);
        }
    }
}
