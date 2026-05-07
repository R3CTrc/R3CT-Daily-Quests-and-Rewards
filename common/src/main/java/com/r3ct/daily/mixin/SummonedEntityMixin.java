package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.advancements.criterion.SummonedEntityTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SummonedEntityTrigger.class)
public abstract class SummonedEntityMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void onEntitySummoned(ServerPlayer player, Entity entity, CallbackInfo ci) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        if (mobId.equals("minecraft:snow_golem")) {
            QuestManager.handleAction(player, "BUILD_GOLEM", "snow", 1);
        }
        else if (mobId.equals("minecraft:iron_golem")) {
            QuestManager.handleAction(player, "BUILD_GOLEM", "iron", 1);
        }
        else if (mobId.equals("minecraft:copper_golem")) {
            QuestManager.handleAction(player, "BUILD_GOLEM", "copper", 1);
        }
        else if (mobId.equals("minecraft:wither")) {
            QuestManager.handleAction(player, "BUILD_GOLEM", "wither", 1);
        }
    }
}