package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        net.minecraft.world.inventory.LoomMenu.class,
        net.minecraft.world.inventory.GrindstoneMenu.class,
        net.minecraft.world.inventory.ItemCombinerMenu.class,
        net.minecraft.world.inventory.AbstractFurnaceMenu.class
})
public abstract class MenuQuickMoveMixin {

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void onShiftClickResultSlot(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack originalStackCopy = cir.getReturnValue();
        if (originalStackCopy == null || originalStackCopy.isEmpty()) return;

        net.minecraft.world.inventory.AbstractContainerMenu menu = (net.minecraft.world.inventory.AbstractContainerMenu) (Object) this;

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) return;

        int amountMoved = originalStackCopy.getCount() - menu.getSlot(slotIndex).getItem().getCount();
        if (amountMoved <= 0) return;

        String itemId = BuiltInRegistries.ITEM.getKey(originalStackCopy.getItem()).toString();

        if (menu instanceof net.minecraft.world.inventory.LoomMenu && slotIndex == 3) {
            QuestManager.handleAction(serverPlayer, "USE_LOOM", itemId, amountMoved);
        }

        else if (menu instanceof net.minecraft.world.inventory.GrindstoneMenu && slotIndex == 2) {
            QuestManager.handleAction(serverPlayer, "GRINDSTONE_ITEM", itemId, amountMoved);
        }

        else if (menu instanceof net.minecraft.world.inventory.SmithingMenu && slotIndex == 3) {
            QuestManager.handleAction(serverPlayer, "TRIM_ARMOR", itemId, amountMoved);
        }

        else if (menu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu && slotIndex == 2) {
            boolean isBlastFurnace = menu instanceof net.minecraft.world.inventory.BlastFurnaceMenu;

            QuestManager.handleAction(serverPlayer, "SMELT_ITEM", itemId, amountMoved);

            if (itemId.endsWith("_glazed_terracotta")) {
                QuestManager.handleAction(serverPlayer, "SMELT_ITEM", "r3ct_daily:glazed_terracotta", amountMoved);
            }

            if (isBlastFurnace) {
                QuestManager.handleAction(serverPlayer, "BLAST_SMELT", itemId, amountMoved);
            }

            if (itemId.equals("minecraft:iron_nugget") || itemId.equals("minecraft:gold_nugget")) {
                QuestManager.handleAction(serverPlayer, "SMELT_NUGGETS", itemId, amountMoved);
            }
        }
    }
}