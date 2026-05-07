package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.animal.armadillo.Armadillo.class)
public abstract class ArmadilloMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void onBrushArmadillo(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (player.getItemInHand(hand).is(net.minecraft.world.item.Items.BRUSH)) {
                QuestManager.handleAction(serverPlayer, "BRUSH_ARMADILLO", "any", 1);
            }
        }
    }
}