package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public abstract class PiglinBarterMixin {

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private static void r3ct_daily$onPickUpGold(ServerLevel level, Piglin piglin, ItemEntity itemEntity, CallbackInfo ci) {
        if (itemEntity.getItem().is(Items.GOLD_INGOT)) {
            java.util.UUID ownerUUID = itemEntity.getOwner() != null ? itemEntity.getOwner().getUUID() : null;
            if (ownerUUID != null) {
                ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(ownerUUID);
                if (player != null) {
                    QuestManager.handleAction(player, "PIGLIN_BARTER", "any", 1);
                }
            }
        }
    }
}