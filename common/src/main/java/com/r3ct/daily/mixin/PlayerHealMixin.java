package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerHealMixin {

    @Inject(method = "heal", at = @At("HEAD"))
    private void onHeal(float healAmount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            float missingHealth = player.getMaxHealth() - player.getHealth();
            float actualHeal = Math.min(healAmount, missingHealth);
            if (actualHeal > 0) {
                QuestManager.handleAction(player, "HEAL", "any", (int) Math.ceil(actualHeal));
            }
        }
    }
}