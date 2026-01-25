package org.virgil.akiasync.mixin.mixins.network.chunk;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Mixin(ServerEntity.class)
public class EntityTrackerPassengersMixin {

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Collections;emptyList()Ljava/util/List;",
            remap = false
        ),
        require = 0
    )
    private List<Entity> akiasync$useGuavaImmutableList() {
        return ImmutableList.of();
    }
}
