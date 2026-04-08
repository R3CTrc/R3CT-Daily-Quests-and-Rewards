package com.r3ct.quests.mixin;

import com.r3ct.quests.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerQuestsMixin {

    @Inject(method = "setVillagerData", at = @At("RETURN"))
    private void onProfession(VillagerData data, CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        String profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.profession().value()).toString();

        if (!profId.equals("minecraft:none")) {
            Player closest = villager.level().getNearestPlayer(villager, 10.0D);
            if (closest instanceof ServerPlayer sp) {
                QuestManager.handleAction(sp, "VILLAGER_PROFESSION", "any", 1);
            }
        }
    }

    @Inject(method = "getBreedOffspring", at = @At("RETURN"))
    private void onBreed(ServerLevel world, net.minecraft.world.entity.AgeableMob ageableMob, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Villager> cir) {
        Villager villager = (Villager)(Object)this;

        if (cir.getReturnValue() != null) {
            Player player = world.getNearestPlayer(villager, 10.0D);
            if (player instanceof ServerPlayer sp) {
                QuestManager.handleAction(sp, "VILLAGER_BREED", "any", 1);
            }
        }
    }
}