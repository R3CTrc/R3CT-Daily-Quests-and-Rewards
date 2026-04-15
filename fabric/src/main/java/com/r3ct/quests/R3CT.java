package com.r3ct.quests;

import com.r3ct.quests.config.ConfigLoader;
import com.r3ct.quests.data.ModState;
import com.r3ct.quests.data.PlayerData;
import com.r3ct.quests.data.TopEntry;
import com.r3ct.quests.item.ModItems;
import com.r3ct.quests.logic.Quest;
import com.r3ct.quests.logic.QuestManager;
import com.r3ct.quests.logic.RewardManager;
import com.r3ct.quests.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class R3CT implements ModInitializer {
	public static final String MOD_ID = "r3ct";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static int lastLeaderboardUpdateTick = -1;
	private static LeaderboardResponsePayload cachedQuestsBoard = null;
	private static LeaderboardResponsePayload cachedRewardsBoard = null;

	public static LocalDate getCurrentQuestDate() {
		return java.time.LocalDateTime.now()
				.minusHours(ConfigLoader.mechanics.technical.questRefreshHour)
				.toLocalDate();
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Starting R3CT Daily Quests system!");

		com.r3ct.quests.item.ModItems.register();

		ResourceKey<CreativeModeTab> R3CT_TAB_KEY = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB,
				Identifier.parse("r3ct:main_tab")
		);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, R3CT_TAB_KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.r3ct.main_tab"))
				.icon(() -> new ItemStack(ModItems.QUEST_SHIELD))
				.displayItems((context, output) -> {
					output.accept(ModItems.QUEST_SHIELD);
					output.accept(ModItems.REWARD_SHIELD);
				})
				.build()
		);

		ConfigLoader.loadAll();

		PayloadTypeRegistry.clientboundPlay().register(OpenRewardsPayload.ID, OpenRewardsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenQuestsPayload.ID, OpenQuestsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncQuestsPayload.ID, SyncQuestsPayload.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(RequestLeaderboardPayload.ID, RequestLeaderboardPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LeaderboardResponsePayload.ID, LeaderboardResponsePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestLeaderboardPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				int type = payload.boardType();
				int currentTick = context.server().getTickCount();

				if (lastLeaderboardUpdateTick == -1 || (currentTick - lastLeaderboardUpdateTick) >= ConfigLoader.mechanics.technical.leaderboardUpdateIntervalTicks) {
					updateLeaderboardCache(context.server());
					lastLeaderboardUpdateTick = currentTick;
				}

				ServerPlayNetworking.send(context.player(), type == 0 ? cachedQuestsBoard : cachedRewardsBoard);
			});
		});

		PayloadTypeRegistry.serverboundPlay().register(RerollQuestPayload.ID, RerollQuestPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RerollQuestPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				QuestManager.rerollQuest(context.player(), payload.questIndex());
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			LocalDate today = getCurrentQuestDate();
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
							freezeMessages.add(Component.translatable("r3ct.message.rewards.freeze_used", "§b" + missedRewards));
							QuestManager.grantAdvancement(player, "r3ct:rewards/safe_player");
						} else {
							data.streak = 0;
							data.rewardDay = 1;
							data.availableRewardFreezes = 0;
							data.absoluteRewardStreak = 0;
							freezeMessages.add(Component.translatable("r3ct.message.rewards.streak_reset"));
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
						freezeMessages.add(Component.translatable("r3ct.message.quests.freeze_used", "§b" + missedQuests));
						QuestManager.grantAdvancement(player, "r3ct:quests/time_lord");
					} else {
						data.questStreak = 0;
						data.availableFreezes = 0;
						freezeMessages.add(Component.translatable("r3ct.message.quests.streak_reset"));
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

			ServerPlayNetworking.send(player, new SyncQuestsPayload(
					data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
					data.activeQuests, data.questProgress, data.streak,
					data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
					data.questRewardsClaimed, data.claimedPointRewards
			));

			server.execute(() -> {
				if (hasRewards) {
					MutableComponent rewardMsg = Component.translatable("r3ct.message.rewards.new_reward")
							.append(Component.translatable("r3ct.message.click_here")
									.withStyle(Style.EMPTY
											.withClickEvent(new ClickEvent.RunCommand("/daily rewards"))
											.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.rewards.open_menu")))
									)
							);
					player.sendSystemMessage(rewardMsg);
				}

				if (isFirstLoginToday) {
					MutableComponent questMsg = Component.translatable("r3ct.message.quests.new_quests")
							.append(Component.translatable("r3ct.message.click_here")
									.withStyle(Style.EMPTY
											.withClickEvent(new ClickEvent.RunCommand("/daily quests"))
											.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.quests.open_menu")))
									)
							);
					player.sendSystemMessage(questMsg);

					for (Component msg : freezeMessages) {
						player.sendSystemMessage(msg);
					}
				} else if (remainingQuests > 0) {
					MutableComponent reminderMsg = Component.translatable("r3ct.message.quests.remaining", "§e" + remainingQuests)
							.append(Component.translatable("r3ct.message.click_here")
									.withStyle(Style.EMPTY
											.withClickEvent(new ClickEvent.RunCommand("/daily quests"))
											.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.quests.open_menu")))
									)
							);
					player.sendSystemMessage(reminderMsg);
				}
			});
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 1200 == 0) {
				java.time.LocalDate today = getCurrentQuestDate();
				String todayStr = today.toString();

				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					PlayerData data = ModState.getPlayerData(server, player.getUUID());
					if (!todayStr.equals(data.lastQuestDate)) {
						refreshPlayerDailyData(player, server, today, data);
					}
				}
			}
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			String rawDimString = destination.dimension().toString();
			String dimId = rawDimString.substring(rawDimString.lastIndexOf("/") + 1, rawDimString.length() - 1).trim();

			QuestManager.handleAction(player, "CHANGE_DIMENSION", dimId);
			PlayerData data = ModState.getPlayerData(destination.getServer(), player.getUUID());

			if (!data.unlockedDimensions.contains(dimId)) {
				data.unlockedDimensions.add(dimId);
				String dimName = dimId.contains("nether") ? "Nether" : (dimId.contains("end") ? "End" : dimId);
				player.sendSystemMessage(Component.translatable("r3ct.message.dimension_discovered", "§l" + dimName));
				destination.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
			}

			if (data.unlockedDimensions.contains("minecraft:overworld") &&
					data.unlockedDimensions.contains("minecraft:the_nether") &&
					data.unlockedDimensions.contains("minecraft:the_end")) {
				QuestManager.grantAdvancement((ServerPlayer) player, "r3ct:quests/dimension_master");
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

			Predicate<CommandSourceStack> isOp = source -> {
				if (source.getPlayer() != null) {
					NameAndId nameAndId = new NameAndId(source.getPlayer().getGameProfile());
					return source.getServer().getPlayerList().isOp(nameAndId);
				}
				return true;
			};

			dispatcher.register(Commands.literal("daily")
					.then(Commands.literal("reload")
							.requires(isOp)
							.executes(context -> {
								ConfigLoader.loadAll();
								context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.reload.success"), true);
								return 1;
							})
					)
					.then(Commands.literal("forcecomplete")
							.requires(isOp)
							.then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
									.then(Commands.literal("all").executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										boolean anyCompleted = false;

										for (int i = 0; i < 5; i++) {
											if (QuestManager.forceCompleteQuest(target, i, true)) {
												anyCompleted = true;
											}
										}

										if (anyCompleted) {
											PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
											syncPlayerData(target, data);
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.all_success", "§e" + target.getName().getString()), true);
										} else {
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.all_already_done", "§b" + target.getName().getString()), false);
										}
										return 1;
									}))
									.then(Commands.argument("index", IntegerArgumentType.integer(1, 5)).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										int index = IntegerArgumentType.getInteger(context, "index") - 1;

										boolean success = QuestManager.forceCompleteQuest(target, index, false);

										if (success) {
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_success", "§e" + (index + 1), "§b" + target.getName().getString()), true);
										} else {
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_already_done", "§e" + (index + 1), "§b" + target.getName().getString()), false);
										}
										return 1;
									}))
							)
					)
					.then(Commands.literal("admin")
							.requires(isOp)
							.then(Commands.literal("reset_cooldown")
									.then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
											.executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());

												data.lastQuestDate = "1970-01-01";
												data.lastRewardDate = "1970-01-01";
												data.lastStreakDate = "1970-01-01";
												data.lastQuestStreakDate = "1970-01-01";

												syncPlayerData(target, data);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.reset_cooldown.success", "§b" + target.getName().getString()), true);
												return 1;
											})
									)
							)
							.then(Commands.literal("points")
									.then(Commands.literal("add").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										int amount = IntegerArgumentType.getInteger(context, "amount");
										PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
										data.totalQuestPoints += amount;
										syncPlayerData(target, data);
										context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.add", "§e" + amount, "§b" + target.getName().getString(), "§e" + data.totalQuestPoints), true);
										return 1;
									}))))
									.then(Commands.literal("set").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										int amount = IntegerArgumentType.getInteger(context, "amount");
										PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
										data.totalQuestPoints = amount;
										syncPlayerData(target, data);
										context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.set", "§e" + amount, "§b" + target.getName().getString()), true);
										return 1;
									}))))
									.then(Commands.literal("remove").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										int amount = IntegerArgumentType.getInteger(context, "amount");
										PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
										data.totalQuestPoints = Math.max(0, data.totalQuestPoints - amount);
										syncPlayerData(target, data);
										context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.remove", "§e" + amount, "§b" + target.getName().getString(), "§e" + data.totalQuestPoints), true);
										return 1;
									}))))
							)
							.then(Commands.literal("streak")
									.then(Commands.literal("set")
											.then(Commands.literal("quests").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												int amount = IntegerArgumentType.getInteger(context, "amount");
												PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
												data.questStreak = amount;
												syncPlayerData(target, data);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.streak.quests", "§b" + target.getName().getString(), "§e" + amount), true);
												return 1;
											}))))
											.then(Commands.literal("rewards").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												int amount = IntegerArgumentType.getInteger(context, "amount");
												PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
												data.streak = amount;
												syncPlayerData(target, data);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.streak.rewards", "§b" + target.getName().getString(), "§e" + amount), true);
												return 1;
											}))))
									)
							)
							.then(Commands.literal("shields")
									.then(Commands.literal("set")
											.then(Commands.literal("quests").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												int amount = IntegerArgumentType.getInteger(context, "amount");
												PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
												data.availableFreezes = amount;
												syncPlayerData(target, data);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.shields.quests", "§b" + target.getName().getString(), "§e" + amount), true);
												return 1;
											}))))
											.then(Commands.literal("rewards").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												int amount = IntegerArgumentType.getInteger(context, "amount");
												PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
												data.availableRewardFreezes = amount;
												syncPlayerData(target, data);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.shields.rewards", "§b" + target.getName().getString(), "§e" + amount), true);
												return 1;
											}))))
									)
							)
							.then(Commands.literal("dimensions")
									.then(Commands.literal("unlock").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("dim", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										String dim = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "dim");
										PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
										if (!data.unlockedDimensions.contains(dim)) {
											data.unlockedDimensions.add(dim);
											syncPlayerData(target, data);
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.unlock.success", "§e" + dim, "§b" + target.getName().getString()), true);
										} else {
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.unlock.fail", "§b" + target.getName().getString(), "§c" + dim), false);
										}
										return 1;
									}))))
									.then(Commands.literal("revoke").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("dim", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(context -> {
										ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
										String dim = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "dim");
										PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
										if (data.unlockedDimensions.contains(dim)) {
											data.unlockedDimensions.remove(dim);
											syncPlayerData(target, data);
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.revoke.success", "§e" + dim, "§b" + target.getName().getString()), true);
										} else {
											context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.revoke.fail", "§b" + target.getName().getString(), "§c" + dim), false);
										}
										return 1;
									}))))
							)
							.then(Commands.literal("clear_data")
									.then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
											.executes(context -> {
												ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
												ModState state = ModState.get(context.getSource().getServer());

												PlayerData newData = new PlayerData();
												newData.lastKnownName = target.getGameProfile().name();
												state.players.put(target.getUUID(), newData);

												syncPlayerData(target, newData);
												context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.clear_data.success", "§b" + target.getName().getString()), true);
												return 1;
											})
									)
							)
					)

					.then(Commands.literal("quests")
							.executes(context -> {
								ServerPlayer player = context.getSource().getPlayer();
								if (player == null) return 0;
								QuestManager.grantAdvancement(player, "r3ct:quests/root");
								PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
								ServerPlayNetworking.send(player, new OpenQuestsPayload(
										data.questStreak,
										data.totalQuestPoints,
										data.dailyQuestsCompletedToday,
										data.activeQuests,
										data.questProgress,
										data.streak,
										data.perfectDaysCount,
										data.availableFreezes,
										data.questRewardsClaimed,
										data.claimedPointRewards,
										ConfigLoader.mechanics.quests.enableQuestRerolling,
										ConfigLoader.mechanics.quests.rerollCostEasy,
										ConfigLoader.mechanics.quests.rerollCostMedium,
										ConfigLoader.mechanics.quests.rerollCostHard,
										ConfigLoader.mechanics.quests.xpDailyReward,
										ConfigLoader.mechanics.quests.xpPerQuestEasy,
										ConfigLoader.mechanics.quests.xpPerQuestMedium,
										ConfigLoader.mechanics.quests.xpPerQuestHard,
										ConfigLoader.mechanics.streaks.perfectDaysForShield,
										ConfigLoader.mechanics.streaks.maxStoredQuestShields
								));
								return 1;
							})
					)
					.then(Commands.literal("claimquest")
							.then(Commands.argument("index", IntegerArgumentType.integer(0, 4))
									.executes(context -> {
										ServerPlayer player = context.getSource().getPlayer();
										if (player == null) return 0;
										int index = IntegerArgumentType.getInteger(context, "index");
										QuestManager.claimQuestReward(player, index);
										return 1;
									})
							)
					)
					.then(Commands.literal("claimpoints")
							.then(Commands.argument("prog", IntegerArgumentType.integer(0, 200))
									.executes(context -> {
										ServerPlayer player = context.getSource().getPlayer();
										if (player == null) return 0;
										int prog = IntegerArgumentType.getInteger(context, "prog");
										QuestManager.claimPointReward(player, prog);
										return 1;
									})
							)
					)
					.then(Commands.literal("claimbonus")
							.then(Commands.argument("dzien", IntegerArgumentType.integer(7, 21))
									.executes(context -> {
										ServerPlayer player = context.getSource().getPlayer();
										if (player == null) return 0;
										int dzien = IntegerArgumentType.getInteger(context, "dzien");
										RewardManager.claimBonusReward(player, dzien);
										return 1;
									})
							)
					)
					.then(Commands.literal("rewards")
							.executes(context -> {
								ServerPlayer player = context.getSource().getPlayer();
								if (player == null) return 0;
								QuestManager.grantAdvancement(player, "r3ct:rewards/root");
								PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
								LocalDate today = getCurrentQuestDate();
								LocalDate yesterday = today.minusDays(1);

								int visualStreak = data.streak;
								if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
									if (!data.lastStreakDate.equals(today.toString()) && !data.lastStreakDate.equals(yesterday.toString())) {
										visualStreak = 0;
									}
								}

								ServerPlayNetworking.send(player, new OpenRewardsPayload(
										data.rewardDay,
										data.lastRewardDate,
										visualStreak,
										data.totalCollected,
										data.claimedRewardHistory,
										data.availableRewardFreezes,
										data.claimedBonusRewards,
										ConfigLoader.mechanics.streaks.maxStoredRewardShields,
										ConfigLoader.mechanics.technical.questRefreshHour
								));
								return 1;
							})
					)
					.then(Commands.literal("claimreward")
							.executes(context -> {
								ServerPlayer player = context.getSource().getPlayer();
								if (player == null) return 0;
								QuestManager.grantAdvancement(player, "r3ct:rewards/first_reward");
								LocalDate today = getCurrentQuestDate();
								PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
								if (today.toString().equals(data.lastRewardDate)) return 1;

								int multi = (data.streak >= 7 ? 2 : 1);

								if (data.lastStreakDate != null && data.lastStreakDate.equals(today.minusDays(1).toString())) {
									data.streak = Math.min(data.streak + 1, 7);
									data.absoluteRewardStreak++;
								} else {
									data.streak = 1;
									data.absoluteRewardStreak = 1;
								}

								if (data.absoluteRewardStreak > data.maxRewardStreak) {
									data.maxRewardStreak = data.absoluteRewardStreak;
								}
								if (data.streak == 7) {
									QuestManager.grantAdvancement(player, "r3ct:rewards/always_on_time");
								}
								data.lastStreakDate = today.toString();

								List<ItemStack> rewards = (data.rewardDay <= 4) ? RewardManager.getTier1Rewards(context.getSource().getServer()) : (data.rewardDay <= 6 ? RewardManager.getTier2Rewards(context.getSource().getServer()) : RewardManager.getTier3Rewards(context.getSource().getServer()));

								StringBuilder historyBuilder = new StringBuilder();
								for (ItemStack rewardStack : rewards) {
									int amount = rewardStack.getCount() * multi;
									rewardStack.setCount(amount);
									String itemName = rewardStack.getHoverName().getString();

									QuestManager.giveOrDrop(player, rewardStack);

									player.sendSystemMessage(Component.translatable("r3ct.message.rewards.received", "§f" + amount, "§b" + itemName));
									if (!historyBuilder.isEmpty()) historyBuilder.append(", ");
									historyBuilder.append("§a").append(amount).append("x ").append(itemName);
								}

								while(data.claimedRewardHistory.size() < 7) {
									data.claimedRewardHistory.add("");
								}

								data.claimedRewardHistory.set(data.rewardDay - 1, historyBuilder.toString());
								data.totalCollected++;

								QuestManager.grantAdvancement(player, "r3ct:rewards/first_reward");
								if (data.totalCollected >= 50) {
									QuestManager.grantAdvancement(player, "r3ct:rewards/login_veteran");
								}
								if (data.rewardDay == 7) {
									QuestManager.grantAdvancement(player, "r3ct:rewards/rich_week");
								}

								if (data.rewardDay == 7) {
									QuestManager.grantAdvancement(player, "r3ct:rewards/rich_week");
									QuestManager.giveOrDrop(player, new ItemStack(ModItems.REWARD_SHIELD));
									player.sendSystemMessage(Component.translatable("r3ct.message.rewards.shield_item_received"));
								}

								data.lastRewardDate = today.toString();
								data.rewardDay = (data.rewardDay >= 7) ? 1 : data.rewardDay + 1;

								context.getSource().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

								ServerPlayNetworking.send(player, new OpenRewardsPayload(
										data.rewardDay,
										data.lastRewardDate,
										data.streak,
										data.totalCollected,
										data.claimedRewardHistory,
										data.availableRewardFreezes,
										data.claimedBonusRewards,
										ConfigLoader.mechanics.streaks.maxStoredRewardShields,
										ConfigLoader.mechanics.technical.questRefreshHour
								));

								return 1;
							})
					)
			);
		});

		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, playerWorld, pos, state, blockEntity) -> {
			if (playerWorld instanceof ServerPlayer serverPlayer) {
				String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

				if (QuestManager.removePlacedBlock(pos, world)) {
					QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", blockId, -1);
					QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "any", -1);
					QuestManager.handleAction(serverPlayer, "PLACE_SAPLING", "any", -1);
					QuestManager.handleAction(serverPlayer, "PLACE_SEED", "any", -1);
					return;
				}

				QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", blockId);

				if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
					QuestManager.handleAction(serverPlayer, "BREAK_LEAVES", "any", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.JUNGLE_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:jungle_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.PALE_OAK_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:pale_oak_logs", 1);
				}
				if (blockId.contains("lapis_ore")) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:lapis_ores", 1);
				}
				if (blockId.equals("minecraft:obsidian")) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:obsidian", 1);
				}
				if (blockId.equals("minecraft:nether_quartz_ore")) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:nether_quartz_ores", 1);
				}
				if (blockId.equals("minecraft:cobweb")) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:cobweb", 1);
				}
				if (blockId.contains("diamond_ore")) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:diamond_ores", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.GOLD_ORES)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:gold_ores", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.FLOWERS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_FLOWER", "any", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.OAK_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:oak_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.BIRCH_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:birch_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.IRON_ORES)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:iron_ores", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.COPPER_ORES)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:copper_ores", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.ACACIA_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:acacia_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.DARK_OAK_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:dark_oak_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.MANGROVE_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:mangrove_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.SPRUCE_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:spruce_logs", 1);
				}
				if (state.is(net.minecraft.tags.BlockTags.CHERRY_LOGS)) {
					QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:cherry_logs", 1);
				}
				if (state.is(net.minecraft.world.level.block.Blocks.BEE_NEST) || state.is(net.minecraft.world.level.block.Blocks.BEEHIVE)) {
					if (blockEntity instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity beehive) {
						if (!beehive.isEmpty()) {

							net.minecraft.world.item.enchantment.ItemEnchantments enchantments = serverPlayer.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

							boolean hasSilkTouch = enchantments.keySet().stream()
									.anyMatch(ench -> ench.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH));

							if (hasSilkTouch) {
								QuestManager.handleAction(serverPlayer, "SILK_TOUCH_BEE_NEST", "any", 1);
							}
						}
					}
				}
			}
		});

		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			net.minecraft.world.entity.Entity attacker = damageSource.getEntity();
			if (!(attacker instanceof ServerPlayer)) attacker = entity.getLastHurtByMob();

			if (attacker instanceof ServerPlayer serverPlayer) {
				String mobId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
				QuestManager.handleAction(serverPlayer, "KILL_MOB", mobId);

				if (serverPlayer.getHealth() <= 4.0f) {
					QuestManager.handleAction(serverPlayer, "KILL_LOW_HP", "any", 1);
				}

				if (mobId.equals("minecraft:skeleton") && serverPlayer.level().dimension().identifier().toString().equals("minecraft:the_nether")) {
					QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_NETHER", "minecraft:skeleton", 1);
				}
				if (mobId.equals("minecraft:cave_spider")) {
					QuestManager.handleAction(serverPlayer, "KILL_MOB", "minecraft:spider", 1);
				}

				if (entity instanceof net.minecraft.world.entity.monster.illager.Pillager pillager) {
					if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
						net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(pillager.blockPosition(), 9216);

						if (raid == null) {
							QuestManager.handleAction(serverPlayer, "KILL_MOB_NO_RAID", "minecraft:pillager", 1);
						}
					}
				}
				else if (entity instanceof net.minecraft.world.entity.monster.Ravager ravagerEntity) {
					if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
						net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(ravagerEntity.blockPosition(), 9216);

						if (raid != null) {
							QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_RAID", "minecraft:ravager", 1);
						}
					}
				}
				if (entity instanceof net.minecraft.world.entity.monster.Monster) {
					if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
						if (serverLevel.isVillage(entity.blockPosition())) {
							QuestManager.handleAction(serverPlayer, "KILL_MOB_VILLAGE", "any", 1);
						}
					}
				}
			}
		});

		net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.STOP_SLEEPING.register((entity, blockPos) -> {
			if (entity instanceof ServerPlayer serverPlayer) {
				QuestManager.handleAction(serverPlayer, "SLEEP", "any", 1);

				if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
					if (serverLevel.isVillage(blockPos)) {
						QuestManager.handleAction(serverPlayer, "SLEEP_IN_VILLAGE", "any", 1);
					}
				}
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (hitResult != null) return InteractionResult.PASS;
			if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
				ItemStack stack = player.getItemInHand(hand);
				String mobId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

				if (mobId.equals("minecraft:cow") && stack.is(Items.BUCKET)) {
					if (entity instanceof net.minecraft.world.entity.animal.cow.Cow cow && !cow.isBaby()) {
						QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:cow_milk", 1);
					}
				}
				if (mobId.equals("minecraft:armadillo") && stack.is(Items.BRUSH)) {
					QuestManager.handleAction(serverPlayer, "BRUSH_ARMADILLO", "any", 1);
				}
				if (mobId.equals("minecraft:turtle") && stack.is(Items.SEAGRASS)) {
					QuestManager.handleAction(serverPlayer, "BREED_TURTLE", "any", 1);
				}
				if (mobId.equals("minecraft:sheep") && stack.is(Items.SHEARS)) {
					if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) {
						if (!sheep.isBaby() && !sheep.isSheared()) {
							QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:sheep_shear", 1);
						}
					}
				}
				if (mobId.equals("minecraft:tropical_fish") && stack.is(Items.WATER_BUCKET)) {
					if (entity instanceof net.minecraft.world.entity.animal.fish.TropicalFish fish && !fish.fromBucket()) {
						QuestManager.handleAction(serverPlayer, "CATCH_FISH_BUCKET", "minecraft:tropical_fish", 1);
					}
				}
			}
			return InteractionResult.PASS;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
				ItemStack stack = player.getItemInHand(hand);
				BlockState targetState = world.getBlockState(hitResult.getBlockPos());

				if (stack.is(Items.BONE_MEAL)) {
					QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:bone_meal", 1);
				}
				if (stack.is(Items.FLINT_AND_STEEL)) {
					net.minecraft.core.BlockPos firePos = hitResult.getBlockPos().relative(hitResult.getDirection());
					if (world.getBlockState(firePos).isAir() || targetState.is(Blocks.TNT) || targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
						QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:flint_and_steel", 1);
					}
				}
				if (targetState.is(Blocks.RESPAWN_ANCHOR) && stack.is(Items.GLOWSTONE)) {
					int charges = targetState.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE);
					if (charges < 4) {
						QuestManager.handleAction(serverPlayer, "CHARGE_RESPAWN_ANCHOR", "any", 1);
					}
				}
				if (targetState.is(Blocks.BELL)) {
					QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:bell", 1);
				}
				if (targetState.is(Blocks.STONECUTTER)) {
					QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:stonecutter", 1);
				}
				if (targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
					if (stack.get(net.minecraft.core.component.DataComponents.FOOD) != null) {
						if (world.getBlockEntity(hitResult.getBlockPos()) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
							boolean hasSpace = false;
							for (ItemStack item : campfire.getItems()) {
								if (item.isEmpty()) { hasSpace = true; break; }
							}
							if (hasSpace) {
								QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:campfire", 1);
							}
						}
					}
				}
				if (targetState.is(Blocks.COMPOSTER)) {
					if (targetState.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) == 8) {
						QuestManager.handleAction(serverPlayer, "EMPTY_COMPOSTER", "any", 1);
					}
				}
				if (targetState.is(net.minecraft.world.level.block.Blocks.TNT) && stack.is(net.minecraft.world.item.Items.FLINT_AND_STEEL)) {
					QuestManager.handleAction(serverPlayer, "IGNITE_TNT", "any", 1);
				}
				if ((targetState.is(Blocks.BEE_NEST) || targetState.is(Blocks.BEEHIVE)) && stack.is(Items.GLASS_BOTTLE)) {
					if (targetState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 5) {
						QuestManager.handleAction(serverPlayer, "COLLECT_HONEY", "any", 1);
					}
				}
			}
			return InteractionResult.PASS;
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity.tickCount == 0) {
				if (entity instanceof net.minecraft.world.entity.animal.golem.CopperGolem) {
					net.minecraft.world.entity.player.Player nearest = world.getNearestPlayer(entity, 10.0D);
					if (nearest instanceof ServerPlayer sp) {
						QuestManager.handleAction(sp, "BUILD_GOLEM", "copper", 1);
					}
				}
				if (entity instanceof net.minecraft.world.entity.animal.golem.IronGolem) {
					net.minecraft.world.entity.player.Player nearest = world.getNearestPlayer(entity, 10.0D);
					if (nearest instanceof ServerPlayer sp) {
						QuestManager.handleAction(sp, "BUILD_GOLEM", "iron", 1);
					}
				}
				if (entity instanceof net.minecraft.world.entity.animal.golem.SnowGolem) {
					net.minecraft.world.entity.player.Player nearest = world.getNearestPlayer(entity, 10.0D);
					if (nearest instanceof ServerPlayer sp) {
						QuestManager.handleAction(sp, "BUILD_GOLEM", "snow", 1);
					}
				}
			}
		});
	}

	private static void updateLeaderboardCache(net.minecraft.server.MinecraftServer server) {
		ModState state = ModState.get(server);
		java.util.List<java.util.Map.Entry<java.util.UUID, PlayerData>> allPlayers = new java.util.ArrayList<>(state.players.entrySet());

		java.util.List<TopEntry> leftQ = new java.util.ArrayList<>();
		java.util.List<TopEntry> rightQ = new java.util.ArrayList<>();

		allPlayers.sort((a, b) -> Integer.compare(b.getValue().totalQuestsCompleted, a.getValue().totalQuestsCompleted));
		for (int i = 0; i < allPlayers.size() && leftQ.size() < 10; i++) {
			PlayerData p = allPlayers.get(i).getValue();
			if (p.totalQuestsCompleted > 0) leftQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
		}

		allPlayers.sort((a, b) -> Integer.compare(b.getValue().maxQuestStreak, a.getValue().maxQuestStreak));
		for (int i = 0; i < allPlayers.size() && rightQ.size() < 10; i++) {
			PlayerData p = allPlayers.get(i).getValue();
			if (p.maxQuestStreak > 0) rightQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
		}
		cachedQuestsBoard = new LeaderboardResponsePayload(0, leftQ, rightQ);

		java.util.List<TopEntry> leftR = new java.util.ArrayList<>();
		java.util.List<TopEntry> rightR = new java.util.ArrayList<>();

		allPlayers.sort((a, b) -> Integer.compare(b.getValue().totalCollected, a.getValue().totalCollected));
		for (int i = 0; i < allPlayers.size() && leftR.size() < 10; i++) {
			PlayerData p = allPlayers.get(i).getValue();
			if (p.totalCollected > 0) leftR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
		}

		allPlayers.sort((a, b) -> Integer.compare(b.getValue().maxRewardStreak, a.getValue().maxRewardStreak));
		for (int i = 0; i < allPlayers.size() && rightR.size() < 10; i++) {
			PlayerData p = allPlayers.get(i).getValue();
			if (p.maxRewardStreak > 0) rightR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
		}
		cachedRewardsBoard = new LeaderboardResponsePayload(1, leftR, rightR);
	}

	private static void syncPlayerData(ServerPlayer target, PlayerData data) {
		net.minecraft.server.MinecraftServer server = target.level().getServer();

		if (server != null) {
			server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
		}
		ServerPlayNetworking.send(target, new SyncQuestsPayload(
				data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
				data.activeQuests, data.questProgress, data.streak,
				data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
				data.questRewardsClaimed, data.claimedPointRewards
		));
	}

	private void refreshPlayerDailyData(net.minecraft.server.level.ServerPlayer player, net.minecraft.server.MinecraftServer server, java.time.LocalDate today, PlayerData data) {
		String todayStr = today.toString();
		java.time.LocalDate yesterday = today.minusDays(1);
		List<Component> freezeMessages = new ArrayList<>();

		if (!todayStr.equals(data.lastRewardDate)) {
			if (!data.lastStreakDate.equals(yesterday.toString()) && !data.lastStreakDate.equals(todayStr)) {
				if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
					long days = ChronoUnit.DAYS.between(LocalDate.parse(data.lastStreakDate), today);
					long missed = days - 1;

					if (data.availableRewardFreezes >= missed) {
						data.availableRewardFreezes -= (int)missed;
						data.lastStreakDate = yesterday.toString();
						freezeMessages.add(Component.translatable("r3ct.message.rewards.freeze_used", "§b" + missed));
						QuestManager.grantAdvancement(player, "r3ct:rewards/safe_player");
					} else {
						data.streak = 0;
						data.rewardDay = 1;
						data.availableRewardFreezes = 0;
						data.absoluteRewardStreak = 0;
						freezeMessages.add(Component.translatable("r3ct.message.rewards.streak_reset"));
					}
				}
			}
		}

		if (data.lastQuestStreakDate != null && !data.lastQuestStreakDate.isEmpty() && !data.lastQuestStreakDate.equals(yesterday.toString()) && !data.lastQuestStreakDate.equals(todayStr)) {
			LocalDate lastStreak = LocalDate.parse(data.lastQuestStreakDate);
			long daysBetween = ChronoUnit.DAYS.between(lastStreak, today);
			long missedDays = daysBetween - 1;

			if (data.availableFreezes >= missedDays) {
				data.availableFreezes -= (int)missedDays;
				data.lastQuestStreakDate = yesterday.toString();
				freezeMessages.add(Component.translatable("r3ct.message.quests.freeze_used", "§b" + missedDays));
				QuestManager.grantAdvancement(player, "r3ct:quests/time_lord");
			} else {
				data.questStreak = 0;
				data.availableFreezes = 0;
				freezeMessages.add(Component.translatable("r3ct.message.quests.streak_reset"));
			}
		}

		data.lastQuestDate = todayStr;
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

		server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

		ServerPlayNetworking.send(player, new SyncQuestsPayload(
				data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
				data.activeQuests, data.questProgress, data.streak,
				data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
				data.questRewardsClaimed, data.claimedPointRewards
		));

		MutableComponent rewardMsg = Component.translatable("r3ct.message.rewards.new_reward")
				.append(Component.translatable("r3ct.message.click_here")
						.withStyle(Style.EMPTY
								.withClickEvent(new ClickEvent.RunCommand("/daily rewards"))
								.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.rewards.open_menu")))
						)
				);
		player.sendSystemMessage(rewardMsg);

		MutableComponent questMsg = Component.translatable("r3ct.message.quests.new_quests")
				.append(Component.translatable("r3ct.message.click_here")
						.withStyle(Style.EMPTY
								.withClickEvent(new ClickEvent.RunCommand("/daily quests"))
								.withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.quests.open_menu")))
						)
				);
		player.sendSystemMessage(questMsg);

		for (Component msg : freezeMessages) {
			player.sendSystemMessage(msg);
		}
	}
}