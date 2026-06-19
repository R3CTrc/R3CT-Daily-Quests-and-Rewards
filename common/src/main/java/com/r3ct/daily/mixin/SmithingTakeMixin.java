package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingTakeMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onSmithingTake(Player player, ItemStack carried, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !carried.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(carried.getItem()).toString();
            QuestManager.handleAction(serverPlayer, "TRIM_ARMOR", itemId, carried.getCount());
        }
    }
}