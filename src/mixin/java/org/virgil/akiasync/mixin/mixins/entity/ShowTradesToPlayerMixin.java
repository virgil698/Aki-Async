package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(ShowTradesToPlayer.class)
public class ShowTradesToPlayerMixin {

    @Shadow
    @Final
    @Mutable
    private List<ItemStack> displayItems = new CopyOnWriteArrayList<>();
}
