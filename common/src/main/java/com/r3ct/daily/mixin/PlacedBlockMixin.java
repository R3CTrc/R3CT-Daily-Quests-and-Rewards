package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class PlacedBlockMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlaceBlock(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {

            BlockPos pos = context.getClickedPos();
            ServerLevel level = (ServerLevel) context.getLevel();

            QuestManager.addPlacedBlock(pos, level);

            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

            QuestManager.handleAction(player, "PLACE_BLOCK", blockId, 1);

            if (state.is(BlockTags.SAPLINGS)) {
                QuestManager.handleAction(player, "PLACE_SAPLING", blockId, 1);
            }

            if (block instanceof CropBlock ||
                    block instanceof StemBlock ||
                    block instanceof NetherWartBlock ||
                    block instanceof PitcherCropBlock) {
                QuestManager.handleAction(player, "PLACE_SEED", blockId, 1);
            }

            if (state.is(BlockTags.BEDS)) {
                if (level.isVillage(pos)) {
                    QuestManager.handleAction(player, "PLACE_BED_IN_VILLAGE", blockId, 1);
                }
            }
        }
    }
}