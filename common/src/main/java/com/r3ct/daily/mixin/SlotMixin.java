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

@Mixin(net.minecraft.world.inventory.Slot.class)
public abstract class SlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeItemFromSlot(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {

            net.minecraft.world.inventory.AbstractContainerMenu menu = serverPlayer.containerMenu;

            net.minecraft.world.inventory.Slot currentSlot = (net.minecraft.world.inventory.Slot) (Object) this;

            int amount = stack.getCount();
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (menu instanceof net.minecraft.world.inventory.LoomMenu loomMenu) {
                if (loomMenu.getSlot(3) == currentSlot) {
                    QuestManager.handleAction(serverPlayer, "USE_LOOM", itemId, 1);
                }
            }

            else if (menu instanceof net.minecraft.world.inventory.GrindstoneMenu grindstoneMenu) {
                if (grindstoneMenu.getSlot(2) == currentSlot) {
                    QuestManager.handleAction(serverPlayer, "GRINDSTONE_ITEM", itemId, 1);
                }
            }

            else if (menu instanceof net.minecraft.world.inventory.SmithingMenu smithingMenu) {
                if (smithingMenu.getSlot(3) == currentSlot) {
                    QuestManager.handleAction(serverPlayer, "TRIM_ARMOR", itemId, 1);
                }
            }

            else if (menu instanceof net.minecraft.world.inventory.FurnaceMenu furnaceMenu) {
                if (furnaceMenu.getSlot(2) == currentSlot) {
                    handleSmelting(serverPlayer, itemId, amount, false);
                }
            }
            else if (menu instanceof net.minecraft.world.inventory.BlastFurnaceMenu blastMenu) {
                if (blastMenu.getSlot(2) == currentSlot) {
                    handleSmelting(serverPlayer, itemId, amount, true);
                }
            }
            else if (menu instanceof net.minecraft.world.inventory.SmokerMenu smokerMenu) {
                if (smokerMenu.getSlot(2) == currentSlot) {
                    handleSmelting(serverPlayer, itemId, amount, false);
                }
            }
        }
    }

    private void handleSmelting(ServerPlayer player, String itemId, int amount, boolean isBlastFurnace) {

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