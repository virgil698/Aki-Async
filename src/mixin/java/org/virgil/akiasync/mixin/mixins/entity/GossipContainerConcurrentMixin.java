package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ai.gossip.GossipContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Mixin(value = GossipContainer.class, priority = 900)
public class GossipContainerConcurrentMixin {

    @Shadow
    private final Map<UUID, ?> gossips = new ConcurrentHashMap<>();
}
