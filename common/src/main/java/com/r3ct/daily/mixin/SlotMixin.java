package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void r3ct_daily$onTakeItemFromSlot(Player player, ItemStack stack, CallbackInfo ci) {

        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {

            AbstractContainerMenu menu = serverPlayer.containerMenu;
            Slot currentSlot = (Slot) (Object) this;

            int amount = stack.getCount();
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (menu instanceof LoomMenu loomMenu) {
                if (loomMenu.getSlot(3) == currentSlot) {
                    QuestManager.handleAction(serverPlayer, "USE_LOOM", itemId, amount);
                }
            }
            else if (menu instanceof FurnaceMenu furnaceMenu) {
                if (furnaceMenu.getSlot(2) == currentSlot) {
                    r3ct_daily$handleSmelting(serverPlayer, itemId, amount, false);
                }
            }
            else if (menu instanceof BlastFurnaceMenu blastMenu) {
                if (blastMenu.getSlot(2) == currentSlot) {
                    r3ct_daily$handleSmelting(serverPlayer, itemId, amount, true);
                }
            }
            else if (menu instanceof SmokerMenu smokerMenu) {
                if (smokerMenu.getSlot(2) == currentSlot) {
                    r3ct_daily$handleSmelting(serverPlayer, itemId, amount, false);
                }
            }
        }
    }

    @Unique
    private void r3ct_daily$handleSmelting(ServerPlayer player, String itemId, int amount, boolean isBlastFurnace) {
        QuestManager.handleAction(player, "SMELT_ITEM", itemId, amount);

        if (itemId.endsWith("_glazed_terracotta")) {
            QuestManager.handleAction(player, "SMELT_ITEM", "r3ct_daily:glazed_terracotta", amount);
        }

        if (isBlastFurnace) {
            QuestManager.handleAction(player, "BLAST_SMELT", itemId, amount);
        }

        if (itemId.equals("minecraft:iron_nugget") || itemId.equals("minecraft:gold_nugget")) {
            QuestManager.handleAction(player, "SMELT_NUGGETS", itemId, amount);
        }
    }
}