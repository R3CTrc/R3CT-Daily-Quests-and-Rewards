package com.r3ct.quests.mixin;

import com.r3ct.quests.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HealMixin {
    @Inject(method = "heal", at = @At("HEAD"))
    private void onHeal(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof ServerPlayer player && amount > 0) {
            float actualHeal = Math.min(entity.getMaxHealth() - entity.getHealth(), amount);
            if (actualHeal > 0) {
                QuestManager.handleAction(player, "HEAL", "any", (int) Math.ceil(actualHeal));
            }
        }
    }
}