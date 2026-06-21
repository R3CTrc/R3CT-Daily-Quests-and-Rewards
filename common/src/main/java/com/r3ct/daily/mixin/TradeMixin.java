package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
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

    @Shadow @Final private Player player;
    @Shadow @Final private Merchant merchant;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void onTradeResult(ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            QuestManager.handleAction(serverPlayer, "TRADE", "any", 1);

            QuestManager.handleAction(serverPlayer, "TRADE_ITEM", itemId, stack.getCount());

            if (this.merchant instanceof WanderingTrader) {
                QuestManager.handleAction(serverPlayer, "TRADE_WANDERING", "any", 1);
            } else if (this.merchant instanceof Villager villager) {
                if (villager.getVillagerData().level() >= 5) {
                    QuestManager.handleAction(serverPlayer, "TRADE_MASTER", "any", 1);
                }
            }
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            Container container = ((Slot)(Object)this).container;

            if (container instanceof MerchantContainer merchantContainer) {
                ItemStack itemPaid1 = merchantContainer.getItem(0);
                ItemStack itemPaid2 = merchantContainer.getItem(1);

                checkSoldItem(serverPlayer, itemPaid1);
                checkSoldItem(serverPlayer, itemPaid2);
            }
        }
    }

    private void checkSoldItem(ServerPlayer player, ItemStack paid) {
        if (!paid.isEmpty()) {
            String paidId = BuiltInRegistries.ITEM.getKey(paid.getItem()).toString();
            QuestManager.handleAction(player, "TRADE_SELL", paidId, paid.getCount());
        }
    }
}