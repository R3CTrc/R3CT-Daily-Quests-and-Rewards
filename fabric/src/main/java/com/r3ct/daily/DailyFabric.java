package com.r3ct.daily;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.item.ModItems;
import com.r3ct.daily.logic.LeaderboardManager;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DailyFabric implements ModInitializer {
	public static final String MOD_ID = "r3ct_daily";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting DailyFabric Daily system!");

		ModItems.register();

		ResourceKey<CreativeModeTab> R3CT_TAB_KEY = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.parse("r3ct_daily:main_tab")
		);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, R3CT_TAB_KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.r3ct_daily.main_tab"))
				.icon(() -> new ItemStack(ModItems.QUEST_SHIELD))
				.displayItems((context, output) -> {
					output.accept(ModItems.QUEST_SHIELD);
					output.accept(ModItems.REWARD_SHIELD);
				})
				.build()
		);

		DailyServerConfig.loadAll();

		PayloadTypeRegistry.clientboundPlay().register(com.r3ct.daily.network.OpenRewardsPayload.ID, com.r3ct.daily.network.OpenRewardsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.r3ct.daily.network.OpenQuestsPayload.ID, com.r3ct.daily.network.OpenQuestsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.r3ct.daily.network.SyncQuestsPayload.ID, com.r3ct.daily.network.SyncQuestsPayload.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(com.r3ct.daily.network.SubmitQuestItemPayload.TYPE, com.r3ct.daily.network.SubmitQuestItemPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(com.r3ct.daily.network.RequestLeaderboardPayload.ID, com.r3ct.daily.network.RequestLeaderboardPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.r3ct.daily.network.LeaderboardResponsePayload.ID, com.r3ct.daily.network.LeaderboardResponsePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(com.r3ct.daily.network.RequestLeaderboardPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				int type = payload.boardType();
				int currentTick = context.server().getTickCount();
				if (LeaderboardManager.lastLeaderboardUpdateTick == -1 || (currentTick - LeaderboardManager.lastLeaderboardUpdateTick) >= DailyServerConfig.mechanics.technical.leaderboardUpdateIntervalTicks) {
					LeaderboardManager.updateLeaderboardCache(context.server());
					LeaderboardManager.lastLeaderboardUpdateTick = currentTick;
				}
				Services.PLATFORM.sendToPlayer(context.player(), type == 0 ? LeaderboardManager.cachedQuestsBoard : LeaderboardManager.cachedRewardsBoard);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(com.r3ct.daily.network.SubmitQuestItemPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				QuestManager.submitQuestItem(context.player(), payload.questIndex(), payload.slotIndex());
			});
		});

		PayloadTypeRegistry.serverboundPlay().register(com.r3ct.daily.network.RerollQuestPayload.ID, com.r3ct.daily.network.RerollQuestPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(com.r3ct.daily.network.RerollQuestPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				QuestManager.rerollQuest(context.player(), payload.questIndex());
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			LocalDate today = QuestManager.getCurrentQuestDate();
			ServerPlayer player = handler.getPlayer();
			PlayerData data = ModState.getPlayerData(server, player.getUUID());
			data.lastKnownName = player.getGameProfile().name();

			final boolean hasRewards = !today.toString().equals(data.lastRewardDate);
			final boolean isFirstLoginToday = !today.toString().equals(data.lastQuestDate);

			List<Component> freezeMessages = new ArrayList<>();

			if (isFirstLoginToday) {
				LocalDate yesterday = today.minusDays(1);

				if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
					if (!data.lastStreakDate.equals(yesterday.toString()) && !data.lastStreakDate.equals(today.toString())) {
						LocalDate lastStreakRewards = LocalDate.parse(data.lastStreakDate);
						long daysBetweenRewards = ChronoUnit.DAYS.between(lastStreakRewards, today);
						long missedRewards = daysBetweenRewards - 1;

						if (data.availableRewardFreezes >= missedRewards) {
							data.availableRewardFreezes -= (int)missedRewards;
							data.lastStreakDate = yesterday.toString();

							Component missedComp = Component.literal(String.valueOf(missedRewards)).withStyle(net.minecraft.ChatFormatting.AQUA);
							freezeMessages.add(Component.empty().append(QuestManager.getPrefix()).append(
									Component.translatable("r3ct_daily.message.rewards.freeze_used", missedComp).withStyle(net.minecraft.ChatFormatting.GREEN)
							));
							QuestManager.grantAdvancement(player, "r3ct_daily:rewards/safe_player");
						} else {
							data.streak = 0;
							data.availableRewardFreezes = 0;
							data.absoluteRewardStreak = 0;
							freezeMessages.add(Component.empty().append(QuestManager.getPrefix()).append(
									Component.translatable("r3ct_daily.message.rewards.streak_reset").withStyle(net.minecraft.ChatFormatting.RED)
							));
						}
					}
				}

				if (data.lastQuestStreakDate != null && !data.lastQuestStreakDate.isEmpty() && !data.lastQuestStreakDate.equals(yesterday.toString()) && !data.lastQuestStreakDate.equals(today.toString())) {
					LocalDate lastStreakQuests = LocalDate.parse(data.lastQuestStreakDate);
					long daysBetweenQuests = ChronoUnit.DAYS.between(lastStreakQuests, today);
					long missedQuests = daysBetweenQuests - 1;

					if (data.availableFreezes >= missedQuests) {
						data.availableFreezes -= (int)missedQuests;
						data.lastQuestStreakDate = yesterday.toString();

						Component missedComp = Component.literal(String.valueOf(missedQuests)).withStyle(net.minecraft.ChatFormatting.AQUA);
						freezeMessages.add(Component.empty().append(QuestManager.getPrefix()).append(
								Component.translatable("r3ct_daily.message.quests.freeze_used", missedComp).withStyle(net.minecraft.ChatFormatting.GREEN)
						));
						QuestManager.grantAdvancement(player, "r3ct_daily:quests/time_lord");
					} else {
						data.questStreak = 0;
						data.availableFreezes = 0;
						freezeMessages.add(Component.empty().append(QuestManager.getPrefix()).append(
								Component.translatable("r3ct_daily.message.quests.streak_reset").withStyle(net.minecraft.ChatFormatting.RED)
						));
					}
				}

				data.lastQuestDate = today.toString();
				data.dailyQuestsCompletedToday = 0;

				List<Quest> newQuests = QuestManager.generateDailyQuests(data, player.getUUID(), today);
				data.activeQuests.clear();
				data.questProgress.clear();
				data.questRewardsClaimed.clear();

				for (Quest q : newQuests) {
					if (q != null && q.id != null) {
						data.activeQuests.add(q.id);
						data.questProgress.add(0);
						data.questRewardsClaimed.add(false);
					}
				}
				server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
			}

			final int remainingQuests = 5 - data.dailyQuestsCompletedToday;

			ServerPlayNetworking.send(player, new com.r3ct.daily.network.SyncQuestsPayload(
					data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
					data.activeQuests, data.questProgress, data.streak,
					data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
					data.questRewardsClaimed, data.claimedPointRewards
			));

			server.execute(() -> {
				if (hasRewards) {
					MutableComponent rewardMsg = Component.empty().append(QuestManager.getPrefix()).append(
							Component.translatable("r3ct_daily.message.rewards.new_reward").withStyle(net.minecraft.ChatFormatting.GREEN)
					).append(Component.translatable("r3ct_daily.message.click_here")
							.withStyle(Style.EMPTY
									.withColor(net.minecraft.ChatFormatting.YELLOW)
									.withBold(true)
									.withClickEvent(new ClickEvent.RunCommand("/daily rewards"))
									.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct_daily.message.rewards.open_menu")))
							)
					);
					player.sendSystemMessage(rewardMsg);
				}

				if (isFirstLoginToday) {
					MutableComponent questMsg = Component.empty().append(QuestManager.getPrefix()).append(
							Component.translatable("r3ct_daily.message.quests.new_quests").withStyle(net.minecraft.ChatFormatting.GREEN)
					).append(Component.translatable("r3ct_daily.message.click_here")
							.withStyle(Style.EMPTY
									.withColor(net.minecraft.ChatFormatting.YELLOW)
									.withBold(true)
									.withClickEvent(new ClickEvent.RunCommand("/daily quests"))
									.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct_daily.message.quests.open_menu")))
							)
					);
					player.sendSystemMessage(questMsg);

					for (Component msg : freezeMessages) {
						player.sendSystemMessage(msg);
					}
				} else if (remainingQuests > 0) {
					Component countComp = Component.literal(String.valueOf(remainingQuests)).withStyle(net.minecraft.ChatFormatting.YELLOW);
					MutableComponent reminderMsg = Component.empty().append(QuestManager.getPrefix()).append(
							Component.translatable("r3ct_daily.message.quests.remaining", countComp).withStyle(net.minecraft.ChatFormatting.GREEN)
					).append(" ").append(Component.translatable("r3ct_daily.message.click_here")
							.withStyle(Style.EMPTY
									.withColor(net.minecraft.ChatFormatting.YELLOW)
									.withBold(true)
									.withClickEvent(new ClickEvent.RunCommand("/daily quests"))
									.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct_daily.message.quests.open_menu")))
							)
					);
					player.sendSystemMessage(reminderMsg);
				}
			});
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 1200 == 0) {
				java.time.LocalDate today = QuestManager.getCurrentQuestDate();
				String todayStr = today.toString();

				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					PlayerData data = ModState.getPlayerData(server, player.getUUID());
					if (!todayStr.equals(data.lastQuestDate)) {
						QuestManager.refreshPlayerDailyData(player, server, today, data);
					}
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			com.r3ct.daily.logic.DailyCommands.register(dispatcher);
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			String rawDimString = destination.dimension().toString();
			String dimId = rawDimString.substring(rawDimString.lastIndexOf("/") + 1, rawDimString.length() - 1).trim();
			com.r3ct.daily.logic.QuestEventHandlers.onDimensionChange((ServerPlayer) player, dimId);
		});

		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, playerWorld, pos, state, blockEntity) -> {
			if (playerWorld instanceof ServerPlayer serverPlayer) {
				com.r3ct.daily.logic.QuestEventHandlers.onBlockBreak(serverPlayer, state, pos, world);
			}
		});

		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			net.minecraft.world.entity.Entity attacker = damageSource.getEntity();
			if (!(attacker instanceof ServerPlayer)) attacker = entity.getLastHurtByMob();
			if (attacker instanceof ServerPlayer serverPlayer) {
				com.r3ct.daily.logic.QuestEventHandlers.onEntityDeath(serverPlayer, entity);
			}
		});
	}
}