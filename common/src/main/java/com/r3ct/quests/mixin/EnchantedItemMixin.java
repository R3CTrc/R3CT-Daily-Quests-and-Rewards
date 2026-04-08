package com.r3ct.quests.mixin;

import com.r3ct.quests.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantedItemMixin {

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void onEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && player instanceof ServerPlayer serverPlayer) {
            QuestManager.handleAction(serverPlayer, "ENCHANT_ITEM", "any", 1);
            if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu enchMenu) {
                if (enchMenu.costs[id] >= 30) {
                    QuestManager.handleAction(serverPlayer, "ENCHANT_LEVEL_30", "any", 1);
                }
            }
        }
    }
}