package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class SlotMixin {

    @Inject(method = "clicked", at = @At("HEAD"))
    private void onMenuClick(int slotId, int button, ContainerInput clickType, Player player, CallbackInfo ci) {
        if (slotId < 0 || player == null) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (slotId < menu.slots.size()) {
            Slot slot = menu.getSlot(slotId);
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

                if (player instanceof ServerPlayer serverPlayer) {
                    if (menu instanceof AnvilMenu && slotId == 2) {
                        QuestManager.handleAction(serverPlayer, "COMBINE_ITEM", itemId, 1);
                        QuestManager.handleAction(serverPlayer, "COMBINE_ITEM", "any", 1);
                    }
                    else if (menu instanceof GrindstoneMenu && slotId == 2) {
                        QuestManager.handleAction(serverPlayer, "GRINDSTONE_ITEM", "any", 1);
                    }
                    else if (menu instanceof SmithingMenu && slotId == 3) {
                        QuestManager.handleAction(serverPlayer, "TRIM_ARMOR", "any", 1);
                    }
                    else if (menu instanceof LoomMenu && slotId == 3) {
                        QuestManager.handleAction(serverPlayer, "USE_LOOM", "any", 1);
                    }
                }
            }
        }
    }
}