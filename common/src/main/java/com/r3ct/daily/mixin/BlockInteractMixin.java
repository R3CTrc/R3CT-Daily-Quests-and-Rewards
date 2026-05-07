package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockInteractMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onUseItemOn(ServerPlayer player, net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {

        if (cir.getReturnValue().consumesAction()) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(hitResult.getBlockPos());

            if (state.is(net.minecraft.world.level.block.Blocks.BEEHIVE) || state.is(net.minecraft.world.level.block.Blocks.BEE_NEST)) {
                if (stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                    QuestManager.handleAction(player, "COLLECT_HONEY", "any", 1);
                }
            }

            if (state.is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) {
                if (stack.is(net.minecraft.world.item.Items.GLOWSTONE)) {
                    QuestManager.handleAction(player, "CHARGE_RESPAWN_ANCHOR", "any", 1);
                }
            }

            if (state.is(net.minecraft.world.level.block.Blocks.TNT)) {
                if (stack.is(net.minecraft.world.item.Items.FLINT_AND_STEEL) || stack.is(net.minecraft.world.item.Items.FIRE_CHARGE)) {
                    QuestManager.handleAction(player, "IGNITE_TNT", "any", 1);
                }
            }
        }
    }
}