package com.r3ct.daily.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.item.ModItems;
import com.r3ct.daily.network.OpenQuestsPayload;
import com.r3ct.daily.network.OpenRewardsPayload;
import com.r3ct.daily.network.SyncQuestsPayload;
import com.r3ct.daily.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

public class DailyCommands {

    public static LocalDate getCurrentQuestDate() {
        return java.time.LocalDateTime.now()
                .minusHours(DailyServerConfig.mechanics.technical.questRefreshHour)
                .toLocalDate();
    }

    private static void syncPlayerData(ServerPlayer target, PlayerData data) {
        net.minecraft.server.MinecraftServer server = target.level().getServer();
        if (server != null) {
            server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        }
        Services.PLATFORM.sendToPlayer(target, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Predicate<CommandSourceStack> isOp = source -> {
            if (source.getPlayer() != null) {
                NameAndId nameAndId = new NameAndId(source.getPlayer().getGameProfile());
                return source.getServer().getPlayerList().isOp(nameAndId);
            }
            return true;
        };

        dispatcher.register(Commands.literal("daily")

                .then(Commands.literal("quests").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.grantAdvancement(player, "r3ct_daily:quests/root");
                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
                    Services.PLATFORM.sendToPlayer(player, new OpenQuestsPayload(
                            data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                            data.activeQuests, data.questProgress, data.streak,
                            data.perfectDaysCount, data.availableFreezes,
                            data.questRewardsClaimed, data.claimedPointRewards,
                            DailyServerConfig.mechanics.quests.enableQuestRerolling,
                            DailyServerConfig.mechanics.quests.rerollCostEasy,
                            DailyServerConfig.mechanics.quests.rerollCostMedium,
                            DailyServerConfig.mechanics.quests.rerollCostHard,
                            DailyServerConfig.mechanics.quests.xpDailyReward,
                            DailyServerConfig.mechanics.quests.xpPerQuestEasy,
                            DailyServerConfig.mechanics.quests.xpPerQuestMedium,
                            DailyServerConfig.mechanics.quests.xpPerQuestHard,
                            DailyServerConfig.mechanics.streaks.perfectDaysForShield,
                            DailyServerConfig.mechanics.streaks.maxStoredQuestShields
                    ));
                    return 1;
                }))
                .then(Commands.literal("claimquest").then(Commands.argument("index", IntegerArgumentType.integer(0, 4)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) QuestManager.claimQuestReward(player, IntegerArgumentType.getInteger(context, "index"));
                    return 1;
                })))
                .then(Commands.literal("claimpoints").then(Commands.argument("prog", IntegerArgumentType.integer(0, 200)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) QuestManager.claimPointReward(player, IntegerArgumentType.getInteger(context, "prog"));
                    return 1;
                })))
                .then(Commands.literal("claimbonus").then(Commands.argument("dzien", IntegerArgumentType.integer(7, 21)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) RewardManager.claimBonusReward(player, IntegerArgumentType.getInteger(context, "dzien"));
                    return 1;
                })))
                .then(Commands.literal("rewards").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.grantAdvancement(player, "r3ct_daily:rewards/root");
                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
                    LocalDate today = getCurrentQuestDate();
                    LocalDate yesterday = today.minusDays(1);

                    int visualStreak = data.streak;
                    if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
                        if (!data.lastStreakDate.equals(today.toString()) && !data.lastStreakDate.equals(yesterday.toString())) {
                            visualStreak = 0;
                        }
                    }
                    Services.PLATFORM.sendToPlayer(player, new OpenRewardsPayload(
                            data.rewardDay, data.lastRewardDate, visualStreak,
                            data.totalCollected, data.claimedRewardHistory,
                            data.availableRewardFreezes, data.claimedBonusRewards,
                            DailyServerConfig.mechanics.streaks.maxStoredRewardShields,
                            DailyServerConfig.mechanics.technical.questRefreshHour
                    ));
                    return 1;
                }))
                .then(Commands.literal("claimreward").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.grantAdvancement(player, "r3ct_daily:rewards/first_reward");
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
                    if (data.absoluteRewardStreak > data.maxRewardStreak) data.maxRewardStreak = data.absoluteRewardStreak;
                    if (data.streak == 7) QuestManager.grantAdvancement(player, "r3ct_daily:rewards/always_on_time");
                    data.lastStreakDate = today.toString();

                    List<ItemStack> rewards = (data.rewardDay <= 4) ? RewardManager.getTier1Rewards(context.getSource().getServer()) : (data.rewardDay <= 6 ? RewardManager.getTier2Rewards(context.getSource().getServer()) : RewardManager.getTier3Rewards(context.getSource().getServer()));
                    StringBuilder historyBuilder = new StringBuilder();

                    Component dayComp = Component.literal(String.valueOf(data.rewardDay)).withStyle(net.minecraft.ChatFormatting.YELLOW);
                    player.sendSystemMessage(Component.empty().append(QuestManager.getPrefix()).append(
                            Component.translatable("r3ct.message.rewards.claimed_base", dayComp).withStyle(net.minecraft.ChatFormatting.GREEN)
                    ));

                    for (ItemStack rewardStack : rewards) {
                        int amount = rewardStack.getCount() * multi;
                        rewardStack.setCount(amount);
                        String translationKey = rewardStack.getItem().getDescriptionId();
                        Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.AQUA);
                        Component translatedItem = rewardStack.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.AQUA);

                        QuestManager.giveOrDrop(player, rewardStack);

                        player.sendSystemMessage(Component.empty().append(QuestManager.getPrefix()).append("- ").withStyle(net.minecraft.ChatFormatting.GRAY).append(
                                Component.translatable("r3ct.message.quests.item_gained", amountComp, translatedItem).withStyle(net.minecraft.ChatFormatting.GREEN)
                        ));

                        if (!historyBuilder.isEmpty()) historyBuilder.append(",");
                        historyBuilder.append(amount).append(";").append(translationKey);
                    }

                    while(data.claimedRewardHistory.size() < 7) {
                        data.claimedRewardHistory.add("");
                    }
                    data.claimedRewardHistory.set(data.rewardDay - 1, historyBuilder.toString());
                    data.totalCollected++;

                    QuestManager.grantAdvancement(player, "r3ct_daily:rewards/first_reward");
                    if (data.totalCollected >= 50) QuestManager.grantAdvancement(player, "r3ct_daily:rewards/login_veteran");
                    if (data.rewardDay == 7) {
                        QuestManager.grantAdvancement(player, "r3ct_daily:rewards/rich_week");
                        QuestManager.giveOrDrop(player, new ItemStack(ModItems.REWARD_SHIELD));
                        player.sendSystemMessage(Component.empty().append(QuestManager.getPrefix()).append(
                                Component.translatable("r3ct.message.rewards.shield_item_received").withStyle(net.minecraft.ChatFormatting.GREEN)
                        ));
                    }

                    data.lastRewardDate = today.toString();
                    data.rewardDay = (data.rewardDay >= 7) ? 1 : data.rewardDay + 1;
                    context.getSource().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

                    Services.PLATFORM.sendToPlayer(player, new OpenRewardsPayload(
                            data.rewardDay, data.lastRewardDate, data.streak,
                            data.totalCollected, data.claimedRewardHistory,
                            data.availableRewardFreezes, data.claimedBonusRewards,
                            DailyServerConfig.mechanics.streaks.maxStoredRewardShields,
                            DailyServerConfig.mechanics.technical.questRefreshHour
                    ));
                    return 1;
                }))

                .then(Commands.literal("admin").requires(isOp)

                        .then(Commands.literal("reload").executes(context -> {
                            DailyServerConfig.loadAll();
                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.reload.success").withStyle(net.minecraft.ChatFormatting.GREEN), true);
                            return 1;
                        }))

                        .then(Commands.literal("forcecomplete").then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("all").executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                    boolean anyCompleted = false;
                                    for (int i = 0; i < 5; i++) {
                                        if (QuestManager.forceCompleteQuest(target, i, true)) anyCompleted = true;
                                    }
                                    if (anyCompleted) {
                                        syncPlayerData(target, ModState.getPlayerData(context.getSource().getServer(), target.getUUID()));
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.all_success", targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.all_already_done", targetName).withStyle(net.minecraft.ChatFormatting.RED), false);
                                    }
                                    return 1;
                                }))
                                .then(Commands.argument("index", IntegerArgumentType.integer(1, 5)).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    int index = IntegerArgumentType.getInteger(context, "index") - 1;
                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                    Component indexComp = Component.literal(String.valueOf(index + 1)).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                    boolean success = QuestManager.forceCompleteQuest(target, index, false);
                                    if (success) context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_success", indexComp, targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    else context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_already_done", indexComp, targetName).withStyle(net.minecraft.ChatFormatting.RED), false);
                                    return 1;
                                }))
                        ))

                        .then(Commands.literal("reset_progress").then(Commands.argument("target", EntityArgument.player()).executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());

                            data.lastRewardDate = "1970-01-01";
                            data.dailyQuestsCompletedToday = 0;

                            for(int i=0; i < data.questProgress.size(); i++) {
                                data.questProgress.set(i, 0);
                                data.questRewardsClaimed.set(i, false);
                            }

                            syncPlayerData(target, data);
                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.test.reset_progress", Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW)).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                            return 1;
                        })))

                        .then(Commands.literal("new_quests").then(Commands.argument("target", EntityArgument.player()).executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());

                            data.lastRewardDate = "1970-01-01";
                            data.dailyQuestsCompletedToday = 0;

                            java.time.LocalDate today = getCurrentQuestDate();
                            List<Quest> newQuests = QuestManager.generateDailyQuests(data, java.util.UUID.randomUUID(), today);

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

                            syncPlayerData(target, data);
                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.test.new_quests", Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW)).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                            return 1;
                        })))

                        .then(Commands.literal("points")
                                .then(Commands.literal("add").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                    data.totalQuestPoints += amount;
                                    syncPlayerData(target, data);

                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                    Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                                    Component totalComp = Component.literal(String.valueOf(data.totalQuestPoints)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

                                    context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.add", amountComp, targetName, totalComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    return 1;
                                }))))
                                .then(Commands.literal("set").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                    data.totalQuestPoints = amount;
                                    syncPlayerData(target, data);

                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                    Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

                                    context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.set", amountComp, targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    return 1;
                                }))))
                                .then(Commands.literal("remove").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                    data.totalQuestPoints = Math.max(0, data.totalQuestPoints - amount);
                                    syncPlayerData(target, data);

                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                    Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                                    Component totalComp = Component.literal(String.valueOf(data.totalQuestPoints)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

                                    context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.points.remove", amountComp, targetName, totalComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    return 1;
                                }))))
                        )

                        .then(Commands.literal("streak")
                                .then(Commands.literal("set")
                                        .then(Commands.literal("quests").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                            data.questStreak = amount;
                                            syncPlayerData(target, data);

                                            Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                            Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.streak.quests", targetName, amountComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))
                                        .then(Commands.literal("rewards").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                            data.streak = amount;
                                            syncPlayerData(target, data);

                                            Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                            Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.streak.rewards", targetName, amountComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))
                                )
                        )

                        .then(Commands.literal("shields")
                                .then(Commands.literal("set")
                                        .then(Commands.literal("quests").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                            data.availableFreezes = amount;
                                            syncPlayerData(target, data);

                                            Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                            Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.AQUA);

                                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.shields.quests", targetName, amountComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))
                                        .then(Commands.literal("rewards").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());
                                            data.availableRewardFreezes = amount;
                                            syncPlayerData(target, data);

                                            Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                            Component amountComp = Component.literal(String.valueOf(amount)).withStyle(net.minecraft.ChatFormatting.AQUA);

                                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.shields.rewards", targetName, amountComp).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))
                                )
                        )

                        .then(Commands.literal("dimensions")
                                .then(Commands.literal("unlock").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("dim", StringArgumentType.word()).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    String dim = StringArgumentType.getString(context, "dim");
                                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());

                                    Component dimComp = Component.literal(dim).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                    if (!data.unlockedDimensions.contains(dim)) {
                                        data.unlockedDimensions.add(dim);
                                        syncPlayerData(target, data);
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.unlock.success", dimComp, targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.unlock.fail", targetName, dimComp).withStyle(net.minecraft.ChatFormatting.RED), false);
                                    }
                                    return 1;
                                }))))
                                .then(Commands.literal("revoke").then(Commands.argument("target", EntityArgument.player()).then(Commands.argument("dim", StringArgumentType.word()).executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    String dim = StringArgumentType.getString(context, "dim");
                                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), target.getUUID());

                                    Component dimComp = Component.literal(dim).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                                    Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);

                                    if (data.unlockedDimensions.contains(dim)) {
                                        data.unlockedDimensions.remove(dim);
                                        syncPlayerData(target, data);
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.revoke.success", dimComp, targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.dimensions.revoke.fail", targetName, dimComp).withStyle(net.minecraft.ChatFormatting.RED), false);
                                    }
                                    return 1;
                                }))))
                        )

                        .then(Commands.literal("clear_data")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            ModState state = ModState.get(context.getSource().getServer());
                                            PlayerData newData = new PlayerData();
                                            newData.lastKnownName = target.getGameProfile().name();
                                            state.players.put(target.getUUID(), newData);
                                            syncPlayerData(target, newData);

                                            Component targetName = Component.literal(target.getName().getString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                                            context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.admin.clear_data.success", targetName).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}