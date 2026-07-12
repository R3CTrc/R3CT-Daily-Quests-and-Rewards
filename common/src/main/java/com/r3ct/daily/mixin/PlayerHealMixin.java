package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerHealMixin {

    @Unique
    private float r3ct_daily$dailyHealBuffer = 0.0f;

    @Inject(method = "heal", at = @At("HEAD"))
    private void r3ct_daily$onHeal(float healAmount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {

            float missingHealth = player.getMaxHealth() - player.getHealth();
            float actualHeal = Math.min(healAmount, missingHealth);

            if (actualHeal > 0) {
                this.r3ct_daily$dailyHealBuffer += actualHeal;

                if (this.r3ct_daily$dailyHealBuffer >= 1.0f) {
                    int pointsToGive = (int) this.r3ct_daily$dailyHealBuffer;
                    QuestManager.handleAction(player, "HEAL", "any", pointsToGive);
                    this.r3ct_daily$dailyHealBuffer -= pointsToGive;
                }
            }
        }
    }
}