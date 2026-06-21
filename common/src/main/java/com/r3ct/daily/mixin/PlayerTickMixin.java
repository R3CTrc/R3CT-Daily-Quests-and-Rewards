package com.r3ct.daily.mixin;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestEventHandlers;
import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTickMixin {

    @Unique private BlockPos lastFlightPos = null;
    @Unique private double levitationStartY = -1;
    @Unique private double maxFallDistance = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {

            if (player.isFallFlying()) {
                if (lastFlightPos == null) {
                    lastFlightPos = player.blockPosition();
                } else if (player.tickCount % 20 == 0) {
                    double dist = Math.sqrt(player.blockPosition().distSqr(lastFlightPos));
                    if (dist >= 1.0) {
                        QuestManager.handleAction(player, "ELYTRA_FLIGHT_NO_LAND", "any", (int) dist);
                    }
                    lastFlightPos = player.blockPosition();
                }
            } else {
                if (lastFlightPos != null) {
                    QuestManager.resetQuestProgress(player, "ELYTRA_FLIGHT_NO_LAND");
                    lastFlightPos = null;
                }
            }

            if (player.hasEffect(MobEffects.LEVITATION)) {
                if (levitationStartY == -1 || player.onGround()) {
                    levitationStartY = player.getY();
                } else if (player.tickCount % 10 == 0) {
                    int heightGained = (int) (player.getY() - levitationStartY);
                    if (heightGained > 0) QuestManager.handleAction(player, "LEVITATION_HEIGHT", "any", heightGained);
                }
            } else {
                levitationStartY = -1;
            }

            if (player.fallDistance > maxFallDistance) {
                maxFallDistance = player.fallDistance;
            }

            if (player.onGround() || player.isInWater() || player.onClimbable() || player.isFallFlying()) {
                if (maxFallDistance > 0) {
                    MinecraftServer server = player.level().getServer();
                    if (server != null) {
                        PlayerData data = ModState.getPlayerData(server, player.getUUID());

                        for (String qId : data.activeQuests) {
                            Quest q = QuestManager.getQuestById(qId);
                            if (q != null && q.actionType.equals("FALL_FROM_HEIGHT")) {
                                try {
                                    int targetHeight = Integer.parseInt(q.target);

                                    if (maxFallDistance >= targetHeight) {
                                        QuestManager.handleAction(player, "FALL_FROM_HEIGHT", q.target, 1);
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                    maxFallDistance = 0;
                }

                QuestManager.resetQuestProgress(player, "LEVITATION_HEIGHT");
            }

            QuestEventHandlers.onPlayerTick(player);
        }
    }
}