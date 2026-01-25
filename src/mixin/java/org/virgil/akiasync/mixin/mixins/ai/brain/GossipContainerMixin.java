package org.virgil.akiasync.mixin.mixins.ai.brain;

import org.virgil.akiasync.mixin.util.concurrent.ConcurrentCollections;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;

@Mixin(GossipContainer.class)
public class GossipContainerMixin {

    @Shadow
    private final Map<UUID, ?> gossips = ConcurrentCollections.newHashMap();
}
