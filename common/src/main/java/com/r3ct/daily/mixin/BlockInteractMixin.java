package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockInteractMixin {

    @Unique
    private net.minecraft.world.item.ItemStack daily$cachedStack = net.minecraft.world.item.ItemStack.EMPTY;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOnHead(ServerPlayer player, net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        this.daily$cachedStack = stack.copy();
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onUseItemOnReturn(ServerPlayer player, net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {

        if (cir.getReturnValue().consumesAction() && !this.daily$cachedStack.isEmpty()) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(hitResult.getBlockPos());

            if (state.is(net.minecraft.world.level.block.Blocks.BEEHIVE) || state.is(net.minecraft.world.level.block.Blocks.BEE_NEST)) {

                if (this.daily$cachedStack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                    QuestManager.handleAction(player, "COLLECT_HONEY", "any", 1);
                }
            }

            if (state.is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) {
                if (this.daily$cachedStack.is(net.minecraft.world.item.Items.GLOWSTONE)) {
                    QuestManager.handleAction(player, "CHARGE_RESPAWN_ANCHOR", "any", 1);
                }
            }

            if (state.is(net.minecraft.world.level.block.Blocks.JUKEBOX)) {
                if (this.daily$cachedStack.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE)) {
                    QuestManager.handleAction(player, "PLAY_JUKEBOX", "any", 1);
                }
            }

            if (state.is(net.minecraft.world.level.block.Blocks.VAULT)) {
                if (this.daily$cachedStack.is(net.minecraft.world.item.Items.TRIAL_KEY) || this.daily$cachedStack.is(net.minecraft.world.item.Items.OMINOUS_TRIAL_KEY)) {
                    QuestManager.handleAction(player, "OPEN_VAULT", "any", 1);
                }
            }
        }
    }
}