package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {

    @Inject(method = "tame", at = @At("HEAD"))
    private void onTame(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            TamableAnimal animal = (TamableAnimal) (Object) this;
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
            QuestManager.handleAction(serverPlayer, "TAME_MOB", mobId, 1);
        }
    }
}