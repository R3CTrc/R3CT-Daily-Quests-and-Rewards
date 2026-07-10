package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin {

    @Unique
    private boolean r3ct_daily$wasHoldingShears = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_daily$beforeInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_daily$wasHoldingShears = player.getItemInHand(hand).is(Items.SHEARS);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_daily$onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (this.r3ct_daily$wasHoldingShears) {
                QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:snow_golem_shear", 1);
            }
        }
    }
}