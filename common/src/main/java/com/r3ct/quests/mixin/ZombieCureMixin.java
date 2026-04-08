package com.r3ct.quests.mixin;

import com.r3ct.quests.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class ZombieCureMixin {

    @Shadow
    private UUID conversionStarter;

    @Inject(method = "finishConversion", at = @At("HEAD"))
    private void onCured(ServerLevel level, CallbackInfo ci) {
        if (this.conversionStarter != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(this.conversionStarter);
            if (player instanceof ServerPlayer sp) {
                QuestManager.handleAction(sp, "CURE_ZOMBIE_VILLAGER", "any", 1);
            }
        }
    }
}