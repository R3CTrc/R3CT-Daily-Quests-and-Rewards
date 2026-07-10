package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class EnderPearlMixin {

    @Inject(method = "onHit", at = @At("HEAD"))
    private void r3ct_daily$onHit(HitResult result, CallbackInfo ci) {
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        Entity owner = pearl.getOwner();

        if (owner instanceof ServerPlayer player) {
            if (player.level() == pearl.level()) {
                double distance = player.position().distanceTo(pearl.position());
                QuestManager.handleAction(player, "PEARL_DISTANCE", "any", (int) distance);
            }
        }
    }
}