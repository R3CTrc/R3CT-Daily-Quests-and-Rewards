package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class PlacedBlockMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlaceBlock(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            net.minecraft.world.level.block.state.BlockState placedState = context.getLevel().getBlockState(context.getClickedPos());
            String blockId = BuiltInRegistries.BLOCK.getKey(placedState.getBlock()).toString();

            QuestManager.addPlacedBlock(context.getClickedPos(), context.getLevel());

            QuestManager.handleAction(player, "PLACE_BLOCK", blockId, 1);
            QuestManager.handleAction(player, "PLACE_BLOCK", "any", 1);

            if (placedState.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                if (player.level().isVillage(context.getClickedPos())) {
                    QuestManager.handleAction(player, "PLACE_BED_IN_VILLAGE", "any", 1);
                }
            }

            if (placedState.is(net.minecraft.tags.BlockTags.SAPLINGS)) {
                QuestManager.handleAction(player, "PLACE_SAPLING", "any", 1);
            }
            if (placedState.getBlock() instanceof net.minecraft.world.level.block.CropBlock) {
                QuestManager.handleAction(player, "PLACE_SEED", "any", 1);
            }
        }
    }
}