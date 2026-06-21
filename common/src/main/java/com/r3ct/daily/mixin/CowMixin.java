package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCow.class)
public abstract class CowMixin {

    @Unique
    private boolean wasHoldingBucket;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void beforeInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.wasHoldingBucket = player.getItemInHand(hand).is(Items.BUCKET);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (this.wasHoldingBucket) {
                QuestManager.handleAction(serverPlayer, "MILK_COW", "any", 1);
            }
        }
    }
}