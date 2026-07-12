package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Armadillo.class)
public abstract class ArmadilloMixin {

    @Unique
    private boolean r3ct_daily$wasHoldingBrush = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_daily$checkBrushBeforeInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_daily$wasHoldingBrush = player.getItemInHand(hand).is(Items.BRUSH);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_daily$onBrushArmadillo(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (this.r3ct_daily$wasHoldingBrush) {
                QuestManager.handleAction(serverPlayer, "BRUSH_ARMADILLO", "any", 1);
            }
        }
    }
}