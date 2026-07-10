package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.advancements.criterion.CuredZombieVillagerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CuredZombieVillagerTrigger.class)
public abstract class CuredZombieVillagerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void r3ct_daily$onZombieCured(ServerPlayer player, Zombie zombie, Villager villager, CallbackInfo ci) {
        if (player != null) {
            QuestManager.handleAction(player, "CURE_ZOMBIE_VILLAGER", "any", 1);
        }
    }
}