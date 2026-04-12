package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        Entity attackerEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        if (amount <= 0.0F) return;

        if (victim instanceof ServerPlayer playerVictim) {
            if (playerVictim.isBlocking() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && attackerEntity instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                    if (playerVictim.distanceTo(creeper) <= 5.0f) {
                        QuestManager.handleAction(playerVictim, "BLOCK_EXPLOSION", "any", 1);
                    }
                }
                else if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                    QuestManager.handleAction(playerVictim, "BLOCK_PROJECTILE", "any", 1);
                }
            }

            QuestManager.handleAction(playerVictim, "TAKE_DAMAGE", "any", (int)amount);

            long time = level.getGameTime() % 24000;
            if (time >= 0 && time < 13000) {
                QuestManager.handleAction(playerVictim, "TAKE_DAMAGE_DAY", "any", (int)amount);
            }
        }

        if (attackerEntity instanceof ServerPlayer attackerPlayer) {
            QuestManager.handleAction(attackerPlayer, "DEAL_DAMAGE", "any", (int)amount);

            if (directEntity instanceof ThrownTrident) {
                if (directEntity.distanceTo(attackerPlayer) >= 20.0f) {
                    QuestManager.handleAction(attackerPlayer, "TRIDENT_SNIPER", "any", 1);
                }
            } else if (directEntity instanceof ThrownSplashPotion) {
                QuestManager.handleAction(attackerPlayer, "POTION_DAMAGE", "any", (int)amount);
            }

            if (victim instanceof net.minecraft.world.entity.monster.Ghast) {
                if (directEntity instanceof net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball) {
                    QuestManager.handleAction(attackerPlayer, "KILL_GHAST_FIREBALL", "any", 1);
                }
            }

            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                QuestManager.handleAction(attackerPlayer, "PROJECTILE_DAMAGE", "any", (int)amount);
            } else {
                if (attackerPlayer.isSprinting()) {
                    QuestManager.handleAction(attackerPlayer, "KNOCKBACK_ATTACK", "any", (int)amount);
                }
                if (attackerPlayer.fallDistance >= 3.0f) {
                    QuestManager.handleAction(attackerPlayer, "FALLING_ATTACK", "any", (int)amount);
                } else if (attackerPlayer.fallDistance > 0.0f && !attackerPlayer.onGround() && !attackerPlayer.onClimbable() && !attackerPlayer.isInWater()) {
                    QuestManager.handleAction(attackerPlayer, "CRITICAL_STRIKE", "any", 1);
                }
            }
        }
    }
}