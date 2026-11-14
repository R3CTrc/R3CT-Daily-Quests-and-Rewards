package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Cushion.class)
public abstract class CushionMixin {

    @Inject(method = "interact", at = @At("RETURN"))
    private void r3ct_daily$onSitOnCushion(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.SUCCESS_SERVER && player instanceof ServerPlayer serverPlayer) {
            Cushion cushion = (Cushion) (Object) this;
            String colorId = "minecraft:" + cushion.getColor().getName() + "_cushion";

            QuestManager.handleAction(serverPlayer, "SIT_ON_CUSHION", colorId, 1);
            QuestManager.handleAction(serverPlayer, "SIT_ON_CUSHION", "any", 1);
        }
    }
}