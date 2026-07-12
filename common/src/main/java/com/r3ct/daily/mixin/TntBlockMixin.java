package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {

    @Unique
    private boolean r3ct_daily$wasHoldingIgniter = false;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void r3ct_daily$beforeIgnite(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemInHand = player.getItemInHand(hand);
        this.r3ct_daily$wasHoldingIgniter = itemInHand.is(Items.FLINT_AND_STEEL) || itemInHand.is(Items.FIRE_CHARGE);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void r3ct_daily$onTntIgnite(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {

        InteractionResult result = cir.getReturnValue();

        if (result.consumesAction() && !level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (this.r3ct_daily$wasHoldingIgniter) {
                QuestManager.handleAction(serverPlayer, "IGNITE_TNT", "any", 1);
            }
        }
    }
}