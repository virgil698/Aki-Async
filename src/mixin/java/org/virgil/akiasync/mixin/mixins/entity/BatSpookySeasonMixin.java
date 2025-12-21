package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

@Mixin(Bat.class)
public class BatSpookySeasonMixin {
    
    @Unique
    private static boolean akiasync$isSpookySeason = false;
    
    @Unique
    private static final int akiasync$ONE_HOUR = 20 * 60 * 60;
    
    @Unique
    private static int akiasync$lastSpookyCheck = -akiasync$ONE_HOUR;
    
    @Inject(method = "isHalloween", at = @At("HEAD"), cancellable = true)
    private static void onIsHalloween(CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftServer.currentTick - akiasync$lastSpookyCheck > akiasync$ONE_HOUR) {
            LocalDate localDate = LocalDate.now();
            int day = localDate.get(ChronoField.DAY_OF_MONTH);
            int month = localDate.get(ChronoField.MONTH_OF_YEAR);
            akiasync$isSpookySeason = month == 10 && day >= 20 || month == 11 && day <= 3;
            akiasync$lastSpookyCheck = MinecraftServer.currentTick;
        }
        cir.setReturnValue(akiasync$isSpookySeason);
    }
}
