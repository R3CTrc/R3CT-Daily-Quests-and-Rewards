package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class TradeMixin {

    @Shadow @Final private Merchant merchant;
    @Shadow @Final private Player player;

    @Inject(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void onTrade(ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int amount = stack.getCount();

            QuestManager.handleAction(serverPlayer, "TRADE", itemId, amount);

            if (stack.is(net.minecraft.world.item.Items.EMERALD)) {
                QuestManager.handleAction(serverPlayer, "TRADE_SELL_SPECIFIC", "any", amount);
            }

            if (this.merchant instanceof net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader) {
                QuestManager.handleAction(serverPlayer, "TRADE_WANDERING", "any", amount);
            } else if (this.merchant instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
                if (villager.getVillagerData().level() >= 5) {
                    QuestManager.handleAction(serverPlayer, "TRADE_MASTER", "any", amount);
                }
            }
        }
    }
}