package org.virgil.akiasync.mixin.mixins.entity.parallel;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.stream.Stream;

@Mixin(EntitySection.class)
public class EntitySectionConcurrentMixin<T extends EntityAccess> {

    @Shadow
    @Final
    private ClassInstanceMultiMap<T> storage;

    @Shadow
    private Visibility chunkStatus;

    @Unique
    private static final Object aki$lock = new Object();

    @WrapMethod(method = "add")
    private void aki$syncAdd(T entity, Operation<Void> original) {
        synchronized (aki$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "remove")
    private boolean aki$syncRemove(T entity, Operation<Boolean> original) {
        synchronized (aki$lock) {
            return original.call(entity);
        }
    }

    @WrapMethod(method = "getStatus")
    private Visibility aki$safeGetStatus(Operation<Visibility> original) {
        return this.chunkStatus != null ? this.chunkStatus : Visibility.HIDDEN;
    }

    @WrapMethod(method = "updateChunkStatus")
    private Visibility aki$syncUpdateChunkStatus(Visibility status, Operation<Visibility> original) {
        synchronized (aki$lock) {
            if (this.chunkStatus == null) {
                this.chunkStatus = Visibility.HIDDEN;
            }
            Visibility safeStatus = (status != null ? status : Visibility.HIDDEN);
            return original.call(safeStatus);
        }
    }

    @WrapMethod(method = "getEntities()Ljava/util/stream/Stream;")
    private Stream<T> aki$safeGetEntities(Operation<Stream<T>> original) {
        return storage.stream()
                .filter(Objects::nonNull)
                .toList()
                .stream();
    }
}
