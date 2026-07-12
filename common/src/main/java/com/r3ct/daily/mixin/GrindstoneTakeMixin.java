package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneTakeMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void r3ct_daily$onGrindstoneTake(Player player, ItemStack carried, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !carried.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(carried.getItem()).toString();
            QuestManager.handleAction(serverPlayer, "GRINDSTONE_ITEM", itemId, carried.getCount());
        }
    }
}