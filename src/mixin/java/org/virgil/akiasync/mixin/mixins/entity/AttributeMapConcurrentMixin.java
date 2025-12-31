package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Mixin(value = AttributeMap.class, priority = 900)
public class AttributeMapConcurrentMixin {

    @Shadow
    private final Set<AttributeInstance> attributesToSync = ConcurrentHashMap.newKeySet();

    @Shadow
    private final Map<Holder<Attribute>, AttributeInstance> attributes = new ConcurrentHashMap<>();

    @Shadow
    private final Set<AttributeInstance> attributesToUpdate = ConcurrentHashMap.newKeySet();
}
