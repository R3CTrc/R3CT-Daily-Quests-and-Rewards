package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class BrewedPotionMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void r3ct_daily$onBrewTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            Slot slot = (Slot)(Object)this;

            if (slot.container instanceof BrewingStandBlockEntity) {

                if (stack.is(Items.SPLASH_POTION)) {
                    QuestManager.handleAction(serverPlayer, "BREW_SPLASH_POTION", "any", stack.getCount());
                } else if (stack.is(Items.LINGERING_POTION)) {
                    QuestManager.handleAction(serverPlayer, "BREW_LINGERING_POTION", "any", stack.getCount());
                }

                PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

                contents.potion().flatMap(holder -> holder.unwrapKey()).ifPresent(key -> {

                    String potId = key.identifier().toString();

                    if (!potId.contains("water") && !potId.contains("mundane") && !potId.contains("thick") && !potId.contains("awkward")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION", potId, stack.getCount());
                    }

                    if (potId.contains("strong_")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION_LEVEL_2", potId, stack.getCount());
                    } else if (potId.contains("long_")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION_EXTENDED", potId, stack.getCount());
                    }
                });
            }
        }
    }
}