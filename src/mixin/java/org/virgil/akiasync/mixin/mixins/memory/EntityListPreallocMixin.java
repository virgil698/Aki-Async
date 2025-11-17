package org.virgil.akiasync.mixin.mixins.memory;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@SuppressWarnings("unused")
@Mixin(net.minecraft.world.level.Level.class)
public abstract class EntityListPreallocMixin {
    private static volatile boolean enabled;
    private static volatile int defaultCapacity;
    private static volatile boolean initialized = false;
    @Redirect(
        method = {
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            "getEntitiesOfClass"
        },
        at = @At(
            value = "NEW",
            target = "()Ljava/util/ArrayList;"
        ),
        require = 0
    )
    private <T> ArrayList<T> preallocateList() {
        if (!initialized) { akiasync$initListPrealloc(); }
        if (!enabled) return new ArrayList<>();
        return new ArrayList<>(defaultCapacity);
    }
    @Redirect(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At(
            value = "NEW",
            target = "(I)Ljava/util/ArrayList;"
        ),
        require = 0
    )
    private <T> ArrayList<T> preallocateListWithSize(int size) {
        if (!enabled) return new ArrayList<>(size);
        return new ArrayList<>(Math.max(size, defaultCapacity));
    }
    private static synchronized void akiasync$initListPrealloc() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isListPreallocEnabled();
            defaultCapacity = bridge.getListPreallocCapacity();
        } else {
            enabled = true;
            defaultCapacity = 32;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] EntityListPreallocMixin initialized: enabled=" + enabled + ", capacity=" + defaultCapacity);
        }
    }
}