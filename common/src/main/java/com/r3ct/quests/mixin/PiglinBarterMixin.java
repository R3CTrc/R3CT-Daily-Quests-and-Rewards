package com.r3ct.quests.mixin;

import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public abstract class PiglinBarterMixin {

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private static void onPickUpGold(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.monster.piglin.Piglin piglin, net.minecraft.world.entity.item.ItemEntity itemEntity, CallbackInfo ci) {
        if (itemEntity.getItem().is(net.minecraft.world.item.Items.GOLD_INGOT)) {
            java.util.UUID ownerUUID = itemEntity.getOwner() != null ? itemEntity.getOwner().getUUID() : null;
            if (ownerUUID != null) {
                net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) level.getPlayerByUUID(ownerUUID);
                if (player != null) {
                    com.r3ct.quests.QuestManager.handleAction(player, "TRADE", "minecraft:piglin", 1);
                }
            }
        }
    }
}