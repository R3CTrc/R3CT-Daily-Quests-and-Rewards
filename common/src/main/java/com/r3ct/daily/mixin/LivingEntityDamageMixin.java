package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Unique private float dailyDamageTakenBuffer = 0.0f;
    @Unique private float dailyDamageDealtBuffer = 0.0f;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void onHurtHead(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && amount > 0.0F) {
            if (player.isBlocking() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && source.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                    if (player.distanceTo(creeper) <= 5.0f) {
                        String creeperId = BuiltInRegistries.ENTITY_TYPE.getKey(creeper.getType()).toString();
                        QuestManager.handleAction(player, "BLOCK_EXPLOSION", creeperId, 1);
                    }
                } else if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
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
    private void onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || amount <= 0.0F) return;

        LivingEntity victim = (LivingEntity) (Object) this;
        Entity attackerEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        String attackerId = attackerEntity != null ? BuiltInRegistries.ENTITY_TYPE.getKey(attackerEntity.getType()).toString() : "environment";

        if (victim.isDeadOrDying() || victim.getHealth() <= 0.0F) {
            if (attackerEntity instanceof ServerPlayer attackerPlayer) {
                if (victim instanceof net.minecraft.world.entity.monster.Ghast) {
                    if (directEntity instanceof net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball) {
                        QuestManager.handleAction(attackerPlayer, "KILL_GHAST_FIREBALL", victimId, 1);
                    }
                }
            }
        }

        if (victim instanceof ServerPlayer serverVictim) {
            LivingEntityDamageMixin victimMixin = (LivingEntityDamageMixin) (Object) serverVictim;

            victimMixin.dailyDamageTakenBuffer += amount;
            if (victimMixin.dailyDamageTakenBuffer >= 1.0f) {
                int pointsToGive = (int) victimMixin.dailyDamageTakenBuffer;

                QuestManager.handleAction(serverVictim, "TAKE_DAMAGE", attackerId, pointsToGive);
                long timeOfDay = level.getDayTime() % 24000L;
                if (timeOfDay >= 0 && timeOfDay < 12000) {
                    QuestManager.handleAction(serverVictim, "TAKE_DAMAGE_DAY", attackerId, pointsToGive);
                }

                victimMixin.dailyDamageTakenBuffer -= pointsToGive;
            }
        }

        if (attackerEntity instanceof ServerPlayer attackerPlayer) {
            LivingEntityDamageMixin attackerMixin = (LivingEntityDamageMixin) (Object) attackerPlayer;

            attackerMixin.dailyDamageDealtBuffer += amount;
            if (attackerMixin.dailyDamageDealtBuffer >= 1.0f) {
                int pointsToGive = (int) attackerMixin.dailyDamageDealtBuffer;

                QuestManager.handleAction(attackerPlayer, "DEAL_DAMAGE", victimId, pointsToGive);

                if (directEntity != null) {
                    if (directEntity instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion ||
                            directEntity instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion) {
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

                attackerMixin.dailyDamageDealtBuffer -= pointsToGive;
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
                            !attackerPlayer.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) &&
                            !attackerPlayer.isPassenger();

                    if (isCrit) {
                        QuestManager.handleAction(attackerPlayer, "CRITICAL_STRIKE", victimId, 1);
                    }
                }
            }
        }
    }
}