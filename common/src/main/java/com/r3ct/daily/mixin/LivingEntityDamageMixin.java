package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Unique private float r3ct_daily$dailyDamageTakenBuffer = 0.0f;
    @Unique private float r3ct_daily$dailyDamageDealtBuffer = 0.0f;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void r3ct_daily$onHurtHead(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && player.connection != null && amount > 0.0F) {
            if (player.isBlocking() && !source.is(DamageTypeTags.BYPASSES_SHIELD)) {
                if (source.is(DamageTypeTags.IS_EXPLOSION) && source.getEntity() instanceof Creeper creeper) {
                    if (player.distanceTo(creeper) <= 5.0f) {
                        String creeperId = BuiltInRegistries.ENTITY_TYPE.getKey(creeper.getType()).toString();
                        QuestManager.handleAction(player, "BLOCK_EXPLOSION", creeperId, 1);
                    }
                } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                    if (source.getDirectEntity() != null) {
                        String projId = BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType()).toString();
                        QuestManager.handleAction(player, "BLOCK_PROJECTILE", projId, 1);
                    } else {
                        QuestManager.handleAction(player, "BLOCK_PROJECTILE", "unknown_projectile", 1);
                    }
                }
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void r3ct_daily$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || amount <= 0.0F) return;

        LivingEntity victim = (LivingEntity) (Object) this;
        Entity attackerEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        boolean isVictimRealPlayer = victim instanceof ServerPlayer sv && sv.connection != null;
        boolean isAttackerRealPlayer = attackerEntity instanceof ServerPlayer ap && ap.connection != null;

        if (!isVictimRealPlayer && !isAttackerRealPlayer) return;

        String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        String attackerId = attackerEntity != null ? BuiltInRegistries.ENTITY_TYPE.getKey(attackerEntity.getType()).toString() : "environment";

        if (isAttackerRealPlayer) {
            ServerPlayer attackerPlayer = (ServerPlayer) attackerEntity;
            LivingEntityDamageMixin attackerMixin = (LivingEntityDamageMixin) (Object) attackerPlayer;

            if (victim.isDeadOrDying() || victim.getHealth() <= 0.0F) {
                if (victim instanceof Ghast && directEntity instanceof LargeFireball) {
                    QuestManager.handleAction(attackerPlayer, "KILL_GHAST_FIREBALL", victimId, 1);
                }
            }

            attackerMixin.r3ct_daily$dailyDamageDealtBuffer += amount;
            if (attackerMixin.r3ct_daily$dailyDamageDealtBuffer >= 1.0f) {
                int pointsToGive = (int) attackerMixin.r3ct_daily$dailyDamageDealtBuffer;

                QuestManager.handleAction(attackerPlayer, "DEAL_DAMAGE", victimId, pointsToGive);

                if (directEntity != null) {
                    if (directEntity instanceof ThrownSplashPotion ||
                            directEntity instanceof ThrownLingeringPotion) {
                        QuestManager.handleAction(attackerPlayer, "POTION_DAMAGE", victimId, pointsToGive);
                    }
                    else if (directEntity instanceof AbstractArrow && !(directEntity instanceof ThrownTrident)) {
                        QuestManager.handleAction(attackerPlayer, "PROJECTILE_DAMAGE", victimId, pointsToGive);
                    }
                    else if (directEntity == attackerPlayer) {
                        if (attackerPlayer.isSprinting()) {
                            QuestManager.handleAction(attackerPlayer, "KNOCKBACK_ATTACK", victimId, pointsToGive);
                        }
                    }
                }

                attackerMixin.r3ct_daily$dailyDamageDealtBuffer -= pointsToGive;
            }

            if (directEntity != null) {
                if (directEntity instanceof ThrownTrident) {
                    if (attackerPlayer.distanceTo(victim) >= 20.0) {
                        QuestManager.handleAction(attackerPlayer, "TRIDENT_SNIPER", victimId, 1);
                    }
                } else if (directEntity == attackerPlayer) {
                    if (attackerPlayer.fallDistance >= 3.0F) {
                        QuestManager.handleAction(attackerPlayer, "FALLING_ATTACK", victimId, 1);
                    }

                    boolean isCrit = attackerPlayer.fallDistance > 0.0F &&
                            !attackerPlayer.onGround() &&
                            !attackerPlayer.onClimbable() &&
                            !attackerPlayer.isInWater() &&
                            !attackerPlayer.hasEffect(MobEffects.BLINDNESS) &&
                            !attackerPlayer.isPassenger() &&
                            !attackerPlayer.isFallFlying();

                    if (isCrit) {
                        QuestManager.handleAction(attackerPlayer, "CRITICAL_STRIKE", victimId, 1);
                    }

                    if (attackerPlayer.fallDistance > 1.5F && !attackerPlayer.isFallFlying()) {
                        String mainHandItemId = BuiltInRegistries.ITEM.getKey(attackerPlayer.getMainHandItem().getItem()).toString();
                        if (mainHandItemId.equals("minecraft:mace")) {
                            QuestManager.handleAction(attackerPlayer, "MACE_SMASH", victimId, 1);
                        }
                    }
                }
            }
        }

        if (isVictimRealPlayer) {
            ServerPlayer serverVictim = (ServerPlayer) victim;
            LivingEntityDamageMixin victimMixin = (LivingEntityDamageMixin) (Object) serverVictim;

            victimMixin.r3ct_daily$dailyDamageTakenBuffer += amount;
            if (victimMixin.r3ct_daily$dailyDamageTakenBuffer >= 1.0f) {
                int pointsToGive = (int) victimMixin.r3ct_daily$dailyDamageTakenBuffer;

                QuestManager.handleAction(serverVictim, "TAKE_DAMAGE", attackerId, pointsToGive);
                long timeOfDay = level.getOverworldClockTime() % 24000L;
                if (timeOfDay >= 0 && timeOfDay < 12000) {
                    QuestManager.handleAction(serverVictim, "TAKE_DAMAGE_DAY", attackerId, pointsToGive);
                }

                victimMixin.r3ct_daily$dailyDamageTakenBuffer -= pointsToGive;
            }
        }
    }
}