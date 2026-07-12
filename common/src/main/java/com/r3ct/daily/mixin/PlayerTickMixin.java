package com.r3ct.daily.mixin;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestEventHandlers;
import com.r3ct.daily.logic.QuestManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTickMixin {

    @Unique private Vec3 r3ct_daily$lastFlightPos = null;
    @Unique private double r3ct_daily$flightDistanceBuffer = 0.0;
    @Unique private double r3ct_daily$levitationStartY = -1;
    @Unique private double r3ct_daily$maxFallDistance = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void r3ct_daily$onTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {

            if (player.connection == null) return;

            if (player.isFallFlying()) {
                if (this.r3ct_daily$lastFlightPos == null) {
                    this.r3ct_daily$lastFlightPos = player.position();
                } else {
                    double distThisTick = player.position().distanceTo(this.r3ct_daily$lastFlightPos);
                    this.r3ct_daily$flightDistanceBuffer += distThisTick;

                    if (this.r3ct_daily$flightDistanceBuffer >= 1.0) {
                        int blocksToAward = (int) this.r3ct_daily$flightDistanceBuffer;
                        QuestManager.handleAction(player, "ELYTRA_FLIGHT_NO_LAND", "any", blocksToAward);
                        this.r3ct_daily$flightDistanceBuffer -= blocksToAward;
                    }

                    this.r3ct_daily$lastFlightPos = player.position();
                }
            } else {
                if (this.r3ct_daily$lastFlightPos != null) {
                    QuestManager.resetQuestProgress(player, "ELYTRA_FLIGHT_NO_LAND");
                    this.r3ct_daily$lastFlightPos = null;
                    this.r3ct_daily$flightDistanceBuffer = 0.0;
                }
            }

            if (player.hasEffect(MobEffects.LEVITATION)) {
                if (this.r3ct_daily$levitationStartY == -1 || player.onGround()) {
                    this.r3ct_daily$levitationStartY = player.getY();
                } else if (player.tickCount % 10 == 0) {
                    int heightGained = (int) (player.getY() - this.r3ct_daily$levitationStartY);
                    if (heightGained > 0) QuestManager.handleAction(player, "LEVITATION_HEIGHT", "any", heightGained);
                }
            } else {
                this.r3ct_daily$levitationStartY = -1;
            }

            if (player.fallDistance > this.r3ct_daily$maxFallDistance) {
                this.r3ct_daily$maxFallDistance = player.fallDistance;
            }

            if (player.onGround() || player.isInWater() || player.onClimbable() || player.isFallFlying()) {
                if (this.r3ct_daily$maxFallDistance > 0) {
                    MinecraftServer server = player.level().getServer();
                    if (server != null) {
                        PlayerData data = ModState.getPlayerData(server, player.getUUID());

                        for (String qId : data.activeQuests) {
                            Quest q = QuestManager.getQuestById(qId);
                            if (q != null && q.actionType.equals("FALL_FROM_HEIGHT")) {
                                try {
                                    int targetHeight = Integer.parseInt(q.target);

                                    if (this.r3ct_daily$maxFallDistance >= targetHeight) {
                                        QuestManager.handleAction(player, "FALL_FROM_HEIGHT", q.target, 1);
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                    this.r3ct_daily$maxFallDistance = 0;
                }

                QuestManager.resetQuestProgress(player, "LEVITATION_HEIGHT");
            }

            QuestEventHandlers.onPlayerTick(player);
        }
    }
}