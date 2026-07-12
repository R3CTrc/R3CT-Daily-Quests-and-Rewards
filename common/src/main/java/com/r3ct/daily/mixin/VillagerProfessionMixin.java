package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {

    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void r3ct_daily$onSetVillagerData(VillagerData newData, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        if (villager.level().isClientSide()) return;

        if (villager.tickCount < 20) return;

        VillagerData oldData = villager.getVillagerData();
        VillagerProfession oldProfession = oldData.profession().value();
        VillagerProfession newProfession = newData.profession().value();

        String oldProfId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(oldProfession).toString();
        String newProfId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(newProfession).toString();

        if (oldProfId.equals("minecraft:none") && !newProfId.equals("minecraft:none") && !newProfId.equals("minecraft:nitwit")) {

            Player nearestPlayer = villager.level().getNearestPlayer(villager, 16.0D);

            if (nearestPlayer instanceof ServerPlayer serverPlayer) {
                QuestManager.handleAction(serverPlayer, "VILLAGER_PROFESSION", newProfId, 1);
            }
        }
    }
}