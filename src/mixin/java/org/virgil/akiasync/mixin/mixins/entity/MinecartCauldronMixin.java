package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(AbstractMinecart.class)
public abstract class MinecartCauldronMixin {

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean enabled = true;

    @Unique
    private boolean aki$hasBeenInCauldronLava = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$checkCauldronLava(CallbackInfo ci) {
        if (!initialized) {
            aki$initMinecartCauldron();
        }

        if (!enabled) {
            return;
        }

        AbstractMinecart self = (AbstractMinecart) (Object) this;

        if (self.level().isClientSide) {
            return;
        }

        BlockPos pos = self.blockPosition();
        BlockState state = self.level().getBlockState(pos);

        if (state.getBlock() instanceof LavaCauldronBlock) {
            if (!aki$hasBeenInCauldronLava) {
                aki$hasBeenInCauldronLava = true;

                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Minecart] Minecart entered lava cauldron at " + pos +
                        ", type: " + self.getClass().getSimpleName());
                }
            }

            aki$destroyMinecartAndDropContents(self, pos);

        } else {
            aki$hasBeenInCauldronLava = false;
        }
    }

    @Unique
    private void aki$destroyMinecartAndDropContents(AbstractMinecart minecart, BlockPos pos) {
        Bridge bridge = BridgeManager.getBridge();

        try {
            if (minecart instanceof MinecartHopper hopperCart) {
                int itemCount = 0;
                for (int i = 0; i < hopperCart.getContainerSize(); i++) {
                    ItemStack stack = hopperCart.getItem(i);
                    if (!stack.isEmpty()) {
                        itemCount++;
                        ItemEntity itemEntity = new ItemEntity(
                            minecart.level(),
                            minecart.getX(),
                            minecart.getY(),
                            minecart.getZ(),
                            stack.copy()
                        );
                        minecart.level().addFreshEntity(itemEntity);
                        hopperCart.setItem(i, ItemStack.EMPTY);
                    }
                }

                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Minecart] MinecartHopper destroyed in lava cauldron, dropped " +
                        itemCount + " item stacks");
                }
            }
            else if (minecart instanceof MinecartChest chestCart) {
                int itemCount = 0;
                for (int i = 0; i < chestCart.getContainerSize(); i++) {
                    ItemStack stack = chestCart.getItem(i);
                    if (!stack.isEmpty()) {
                        itemCount++;
                        ItemEntity itemEntity = new ItemEntity(
                            minecart.level(),
                            minecart.getX(),
                            minecart.getY(),
                            minecart.getZ(),
                            stack.copy()
                        );
                        minecart.level().addFreshEntity(itemEntity);
                        chestCart.setItem(i, ItemStack.EMPTY);
                    }
                }

                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Minecart] MinecartChest destroyed in lava cauldron, dropped " +
                        itemCount + " item stacks");
                }
            }

            minecart.discard();

            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Minecart] Minecart destroyed at " + pos);
            }

        } catch (Exception e) {
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Minecart] Error destroying minecart: " + e.getMessage());
            }
        }
    }

    @Unique
    private static synchronized void aki$initMinecartCauldron() {
        if (initialized) return;

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isMinecartCauldronDestructionEnabled();
            bridge.debugLog("[AkiAsync] MinecartCauldronMixin initialized: enabled=" + enabled);
        }

        initialized = true;
    }
}
