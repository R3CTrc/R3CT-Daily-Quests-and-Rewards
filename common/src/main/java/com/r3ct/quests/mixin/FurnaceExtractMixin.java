package com.r3ct.quests.mixin;

import com.r3ct.quests.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceExtractMixin {

    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void onSmelt(ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int amount = this.removeCount > 0 ? this.removeCount : stack.getCount();
            QuestManager.handleAction(serverPlayer, "SMELT_ITEM", itemId, amount);
            if (itemId.equals("minecraft:iron_nugget") || itemId.equals("minecraft:gold_nugget")) {
                QuestManager.handleAction(serverPlayer, "SMELT_NUGGETS", "any", amount);
            }
            if (itemId.contains("glazed_terracotta")) {
                QuestManager.handleAction(serverPlayer, "SMELT_ITEM", "r3ct:glazed_terracotta", amount);
            }
            Slot slot = (Slot)(Object)this;
            if (slot.container instanceof net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity) {
                QuestManager.handleAction(serverPlayer, "BLAST_SMELT", itemId, amount);
            }
        }
    }
}