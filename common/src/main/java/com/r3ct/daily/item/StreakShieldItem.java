package com.r3ct.daily.item;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.network.SyncQuestsPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StreakShieldItem extends Item {
    private final boolean isQuestShield;

    public StreakShieldItem(Properties properties, boolean isQuestShield) {
        super(properties);
        this.isQuestShield = isQuestShield;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {

            PlayerData data = ModState.getPlayerData(world.getServer(), serverPlayer.getUUID());

            if (this.isQuestShield) {
                int maxQuestShields = DailyServerConfig.mechanics.streaks.maxStoredQuestShields;

                if (data.availableFreezes < maxQuestShields) {
                    data.availableFreezes++;
                    stack.shrink(1);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.translatable("r3ct.message.shield.quest.used", "§b" + data.availableFreezes, "§b" + maxQuestShields));
                    if (data.availableFreezes == maxQuestShields) {
                        QuestManager.grantAdvancement(serverPlayer, "r3ct_daily:quests/hamster");
                    }
                } else {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.translatable("r3ct.message.shield.quest.full", "§b" + maxQuestShields));
                    return InteractionResult.FAIL;
                }
            } else {
                int maxRewardShields = DailyServerConfig.mechanics.streaks.maxStoredRewardShields;

                if (data.availableRewardFreezes < maxRewardShields) {
                    data.availableRewardFreezes++;
                    stack.shrink(1);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.translatable("r3ct.message.shield.reward.used", "§b" + data.availableRewardFreezes, "§b" + maxRewardShields));
                    if (data.availableRewardFreezes == maxRewardShields) {
                        QuestManager.grantAdvancement(serverPlayer, "r3ct_daily:rewards/shield_collector");
                    }
                } else {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.translatable("r3ct.message.shield.reward.full", "§b" + maxRewardShields));
                    return InteractionResult.FAIL;
                }
            }

            world.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

            Services.PLATFORM.sendToPlayer(serverPlayer, new SyncQuestsPayload(
                    data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                    data.activeQuests, data.questProgress, data.streak,
                    data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                    data.questRewardsClaimed, data.claimedPointRewards
            ));

            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }
}