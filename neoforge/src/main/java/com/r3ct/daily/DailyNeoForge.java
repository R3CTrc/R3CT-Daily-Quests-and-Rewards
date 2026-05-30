package com.r3ct.daily;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.item.ModItems;
import com.r3ct.daily.logic.LeaderboardManager;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.platform.Services;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Mod(Constants.MOD_ID)
public class DailyNeoForge {

    public DailyNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        Constants.LOG.info("Starting R3CT Daily system!");
        DailyServerConfig.loadAll();
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onRegister);
        NeoForge.EVENT_BUS.register(this);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            DailyNeoForgeClient.init(modContainer);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToClient(com.r3ct.daily.network.OpenRewardsPayload.ID, com.r3ct.daily.network.OpenRewardsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> DailyNeoForgeClient.ClientPayloadHandlers.handleOpenRewards(payload));
        });
        registrar.playToClient(com.r3ct.daily.network.OpenQuestsPayload.ID, com.r3ct.daily.network.OpenQuestsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> DailyNeoForgeClient.ClientPayloadHandlers.handleOpenQuests(payload));
        });
        registrar.playToClient(com.r3ct.daily.network.SyncQuestsPayload.ID, com.r3ct.daily.network.SyncQuestsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> DailyNeoForgeClient.ClientPayloadHandlers.handleSyncQuests(payload));
        });
        registrar.playToClient(com.r3ct.daily.network.LeaderboardResponsePayload.ID, com.r3ct.daily.network.LeaderboardResponsePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> DailyNeoForgeClient.ClientPayloadHandlers.handleLeaderboardResponse(payload));
        });

        registrar.playToServer(com.r3ct.daily.network.RequestLeaderboardPayload.ID, com.r3ct.daily.network.RequestLeaderboardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    int type = payload.boardType();
                    int currentTick = player.level().getServer().getTickCount();

                    if (LeaderboardManager.lastLeaderboardUpdateTick == -1 || (currentTick - LeaderboardManager.lastLeaderboardUpdateTick) >= DailyServerConfig.mechanics.technical.leaderboardUpdateIntervalTicks) {
                        LeaderboardManager.updateLeaderboardCache(player.level().getServer());
                        LeaderboardManager.lastLeaderboardUpdateTick = currentTick;
                    }
                    Services.PLATFORM.sendToPlayer(player, type == 0 ? LeaderboardManager.cachedQuestsBoard : LeaderboardManager.cachedRewardsBoard);
                }
            });
        });
        registrar.playToServer(com.r3ct.daily.network.RerollQuestPayload.ID, com.r3ct.daily.network.RerollQuestPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    QuestManager.rerollQuest(player, payload.questIndex());
                }
            });
        });
    }

    private void onRegister(net.neoforged.neoforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) {
            ModItems.register();
        }

        event.register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(net.minecraft.resources.Identifier.parse("r3ct_daily:main_tab"),
                    net.minecraft.world.item.CreativeModeTab.builder()
                            .title(net.minecraft.network.chat.Component.translatable("itemGroup.r3ct_daily.main_tab"))
                            .icon(() -> new net.minecraft.world.item.ItemStack(ModItems.QUEST_SHIELD))
                            .displayItems((context, output) -> {
                                output.accept(ModItems.QUEST_SHIELD);
                                output.accept(ModItems.REWARD_SHIELD);
                            })
                            .build()
            );
        });
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        net.minecraft.server.MinecraftServer server = player.level().getServer();

        LocalDate today = QuestManager.getCurrentQuestDate();
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
                        data.availableRewardFreezes -= (int) missedRewards;
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
                    data.availableFreezes -= (int) missedQuests;
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

        Services.PLATFORM.sendToPlayer(player, new com.r3ct.daily.network.SyncQuestsPayload(
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
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
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
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String dimId = event.getTo().identifier().toString();
            com.r3ct.daily.logic.QuestEventHandlers.onDimensionChange(player, dimId);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            com.r3ct.daily.logic.QuestEventHandlers.onBlockBreak(serverPlayer, event.getState(), event.getPos(), serverPlayer.level());
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer)) attacker = event.getEntity().getLastHurtByMob();

        if (attacker instanceof ServerPlayer serverPlayer) {
            com.r3ct.daily.logic.QuestEventHandlers.onEntityDeath(serverPlayer, event.getEntity());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        com.r3ct.daily.logic.DailyCommands.register(event.getDispatcher());
    }
}