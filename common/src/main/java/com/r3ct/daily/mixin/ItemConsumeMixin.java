package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ItemConsumeMixin {

    @Shadow public abstract ItemStack getActiveItem();

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void r3ct_daily$onCompleteUsingItem(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ItemStack stack = this.getActiveItem();
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                QuestManager.handleAction(player, "CONSUME_ITEM", itemId, 1);
            }
        }
    }
}