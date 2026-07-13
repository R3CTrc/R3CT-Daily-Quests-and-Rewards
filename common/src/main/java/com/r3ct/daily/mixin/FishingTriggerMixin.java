package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.advancements.triggers.FishingRodHookedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(FishingRodHookedTrigger.class)
public abstract class FishingTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void r3ct_daily$onFishCaught(ServerPlayer player, ItemStack rod, FishingHook entity, Collection<ItemStack> loot, CallbackInfo ci) {
        if (player != null && player.connection != null) {
            for (ItemStack stack : loot) {
                if (!stack.isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    QuestManager.handleAction(player, "FISHING", itemId, stack.getCount());
                }
            }
        }
    }
}