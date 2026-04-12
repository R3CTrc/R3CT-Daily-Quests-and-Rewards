package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Inject(method = "tameWithName", at = @At("HEAD"))
    private void onTameHorse(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            AbstractHorse horse = (AbstractHorse) (Object) this;
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(horse.getType()).toString();
            QuestManager.handleAction(serverPlayer, "TAME_MOB", mobId, 1);
        }
    }
}