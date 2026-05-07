package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestEventHandlers;
import net.minecraft.advancements.criterion.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantedItemTrigger.class)
public abstract class EnchantItemMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void onEnchant(ServerPlayer player, ItemStack item, int levels, CallbackInfo ci) {
        QuestEventHandlers.onItemEnchanted(player, item);
    }
}