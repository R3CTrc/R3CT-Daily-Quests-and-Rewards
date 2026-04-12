package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPlaceMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            QuestManager.addPlacedBlock(context.getClickedPos(), context.getLevel());
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(((BlockItem)(Object)this).getBlock()).toString();
            QuestManager.handleAction(player, "PLACE_BLOCK", blockId, 1);
            QuestManager.handleAction(player, "PLACE_BLOCK", "any", 1);
        }
    }
}