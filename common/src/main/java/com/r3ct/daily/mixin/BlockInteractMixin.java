package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockInteractMixin {

    @Unique private ItemStack daily$cachedBlockStack = ItemStack.EMPTY;
    @Unique private ItemStack daily$cachedAirStack = ItemStack.EMPTY;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOnHead(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        this.daily$cachedBlockStack = stack.copy();
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onUseItemOnReturn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.daily$cachedBlockStack.isEmpty()) {
            ItemStack currentStack = player.getItemInHand(hand);

            if (cir.getReturnValue().consumesAction()) {
                BlockState state = level.getBlockState(hitResult.getBlockPos());
                if (state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST)) {
                    if (this.daily$cachedBlockStack.is(Items.GLASS_BOTTLE)) QuestManager.handleAction(player, "COLLECT_HONEY", "any", 1);
                }
                if (state.is(Blocks.RESPAWN_ANCHOR)) {
                    if (this.daily$cachedBlockStack.is(Items.GLOWSTONE)) QuestManager.handleAction(player, "CHARGE_RESPAWN_ANCHOR", "any", 1);
                }
                if (state.is(Blocks.JUKEBOX)) {
                    if (this.daily$cachedBlockStack.has(DataComponents.JUKEBOX_PLAYABLE)) QuestManager.handleAction(player, "PLAY_JUKEBOX", "any", 1);
                }
                if (state.is(Blocks.VAULT)) {
                    if (this.daily$cachedBlockStack.is(Items.TRIAL_KEY) || this.daily$cachedBlockStack.is(Items.OMINOUS_TRIAL_KEY)) QuestManager.handleAction(player, "OPEN_VAULT", "any", 1);
                }
            }

            daily$checkAndAwardUseItem(player, this.daily$cachedBlockStack, currentStack);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void onUseItemHead(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.daily$cachedAirStack = stack.copy();
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void onUseItemReturn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.daily$cachedAirStack.isEmpty()) {
            ItemStack currentStack = player.getItemInHand(hand);

            daily$checkAndAwardUseItem(player, this.daily$cachedAirStack, currentStack);
        }
    }

    @Unique
    private void daily$checkAndAwardUseItem(ServerPlayer player, ItemStack cachedStack, ItemStack currentStack) {
        int amountUsed = 0;

        if (currentStack.getCount() < cachedStack.getCount()) {
            amountUsed = cachedStack.getCount() - currentStack.getCount();
        }
        else if (currentStack.getItem() != cachedStack.getItem()) {
            amountUsed = 1;
        }
        else if (currentStack.isDamageableItem() && currentStack.getDamageValue() > cachedStack.getDamageValue()) {
            amountUsed = 1;
        }

        if (amountUsed > 0) {
            String itemId = BuiltInRegistries.ITEM.getKey(cachedStack.getItem()).toString();
            QuestManager.handleAction(player, "USE_ITEM", itemId, amountUsed);

            if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg") || itemId.equals("minecraft:blue_egg")) {
                QuestManager.handleAction(player, "THROW_EGG", "any", amountUsed);
            }
        }
    }
}