package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFish.class)
public abstract class FishInteractMixin {

    @Unique
    private boolean r3ct_daily$wasHoldingWaterBucket;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_daily$beforeInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_daily$wasHoldingWaterBucket = player.getItemInHand(hand).is(Items.WATER_BUCKET);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_daily$onCatchFish(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (this.r3ct_daily$wasHoldingWaterBucket) {
                AbstractFish fish = (AbstractFish) (Object) this;
                if (!fish.fromBucket()) {
                    String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()).toString();
                    QuestManager.handleAction(serverPlayer, "CATCH_FISH_BUCKET", mobId, 1);
                }
            }
        }
    }
}