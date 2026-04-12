package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class LootMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onLootOrBrew(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            Slot slot = (Slot)(Object)this;

            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) {

                if (!(slot.container instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity)) {
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    QuestManager.handleAction(serverPlayer, "LOOT_ITEM", itemId, stack.getCount());
                }
            }

            if (slot.container instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity) {
                net.minecraft.world.item.alchemy.PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

                if (contents != null && contents.potion().isPresent()) {
                    String potId = contents.potion().get().getRegisteredName();

                    if (!potId.contains("water") && !potId.contains("mundane") && !potId.contains("thick") && !potId.contains("awkward")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION", itemId, 1);
                        QuestManager.handleAction(serverPlayer, "BREW_POTION", "any", 1);
                    }
                    if (potId.contains("strong_")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION_LEVEL_2", "any", 1);
                    } else if (potId.contains("long_")) {
                        QuestManager.handleAction(serverPlayer, "BREW_POTION_EXTENDED", "any", 1);
                    } else if (potId.contains("weakness") && stack.is(net.minecraft.world.item.Items.SPLASH_POTION)) {
                        QuestManager.handleAction(serverPlayer, "BREW_SPLASH_WEAKNESS", "any", 1);
                    }
                }

                if (stack.is(net.minecraft.world.item.Items.SPLASH_POTION)) {
                    QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "minecraft:splash_potion", 1);
                }
            }
        }
    }
}