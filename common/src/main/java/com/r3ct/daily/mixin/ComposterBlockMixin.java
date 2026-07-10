package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void r3ct_daily$onEmptyComposter(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (state.getValue(ComposterBlock.LEVEL) == 8) {
                QuestManager.handleAction(serverPlayer, "EMPTY_COMPOSTER", "any", 1);
            }
        }
    }
}