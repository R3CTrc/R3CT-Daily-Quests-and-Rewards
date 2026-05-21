package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin {
    @Inject(method = "dropContent", at = @At("HEAD"))
    private void onDropContent(ServerLevel level, LivingEntity user, ItemStack brush, CallbackInfo ci) {
        if (user instanceof ServerPlayer serverPlayer) {
            QuestManager.handleAction(serverPlayer, "BRUSH_BLOCK", "any", 1);
        }
    }
}