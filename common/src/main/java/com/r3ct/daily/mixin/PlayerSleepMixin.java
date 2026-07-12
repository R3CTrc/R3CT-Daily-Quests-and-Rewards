package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerSleepMixin {

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void r3ct_daily$onStopSleepInBed(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
        if (!forcefulWakeUp) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            QuestManager.handleAction(player, "SLEEP_IN_BED", "any", 1);
        }
    }
}