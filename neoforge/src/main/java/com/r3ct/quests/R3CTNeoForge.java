package com.r3ct.quests;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.lwjgl.glfw.GLFW;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mod(Constants.MOD_ID)
public class R3CTNeoForge {

    private static int lastLeaderboardUpdateTick = -1;
    private static LeaderboardResponsePayload cachedQuestsBoard = null;
    private static LeaderboardResponsePayload cachedRewardsBoard = null;

    public static LocalDate getCurrentQuestDate() {
        return java.time.LocalDateTime.now()
                .minusHours(ConfigLoader.mechanics.technical.questRefreshHour)
                .toLocalDate();
    }

    public R3CTNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        Constants.LOG.info("Starting R3CT Daily Quests system!");
        ConfigLoader.loadAll();

        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (client, parent) -> createConfigScreen(parent));
    }

    private net.minecraft.client.gui.screens.Screen createConfigScreen(net.minecraft.client.gui.screens.Screen parent) {
        me.shedaniel.clothconfig2.api.ConfigBuilder builder = me.shedaniel.clothconfig2.api.ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("r3ct.config.title"));

        me.shedaniel.clothconfig2.api.ConfigCategory general = builder.getOrCreateCategory(Component.translatable("r3ct.config.category.hud"));
        me.shedaniel.clothconfig2.api.ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("r3ct.config.entry.enable_hud"), com.r3ct.quests.config.R3CTQuestsConfig.getInstance().enableHud)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> com.r3ct.quests.config.R3CTQuestsConfig.getInstance().enableHud = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("r3ct.config.entry.hud_x"), com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudXOffset)
                .setDefaultValue(10)
                .setSaveConsumer(newValue -> com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudXOffset = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("r3ct.config.entry.hud_y"), com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudYOffset)
                .setDefaultValue(70)
                .setSaveConsumer(newValue -> com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudYOffset = newValue)
                .build());

        builder.setSavingRunnable(() -> {
            com.r3ct.quests.config.R3CTQuestsConfig.save();
        });

        return builder.build();
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToClient(OpenRewardsPayload.ID, OpenRewardsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientPayloadHandlers.handleOpenRewards(payload));
        });
        registrar.playToClient(OpenQuestsPayload.ID, OpenQuestsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientPayloadHandlers.handleOpenQuests(payload));
        });
        registrar.playToClient(SyncQuestsPayload.ID, SyncQuestsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientPayloadHandlers.handleSyncQuests(payload));
        });
        registrar.playToClient(LeaderboardResponsePayload.ID, LeaderboardResponsePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientPayloadHandlers.handleLeaderboardResponse(payload));
        });

        registrar.playToServer(RequestLeaderboardPayload.ID, RequestLeaderboardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    int type = payload.boardType();
                    int currentTick = player.level().getServer().getTickCount();

                    if (lastLeaderboardUpdateTick == -1 || (currentTick - lastLeaderboardUpdateTick) >= ConfigLoader.mechanics.technical.leaderboardUpdateIntervalTicks) {
                        updateLeaderboardCache(player.level().getServer());
                        lastLeaderboardUpdateTick = currentTick;
                    }
                    com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, type == 0 ? cachedQuestsBoard : cachedRewardsBoard);
                }
            });
        });
        registrar.playToServer(RerollQuestPayload.ID, RerollQuestPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    QuestManager.rerollQuest(player, payload.questIndex());
                }
            });
        });
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        net.minecraft.server.MinecraftServer server = player.level().getServer();

        LocalDate today = getCurrentQuestDate();
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
                    data.availableFreezes -= (int) missedQuests;
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

        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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
                                        .withClickEvent(new ClickEvent.RunCommand("/rdq rewards"))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.rewards.open_menu")))
                                )
                        );
                player.sendSystemMessage(rewardMsg);
            }

            if (isFirstLoginToday) {
                MutableComponent questMsg = Component.translatable("r3ct.message.quests.new_quests")
                        .append(Component.translatable("r3ct.message.click_here")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent.RunCommand("/rdq quests"))
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
                                        .withClickEvent(new ClickEvent.RunCommand("/rdq quests"))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.quests.open_menu")))
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
            java.time.LocalDate today = getCurrentQuestDate();
            String todayStr = today.toString();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerData data = ModState.getPlayerData(server, player.getUUID());
                if (!todayStr.equals(data.lastQuestDate)) {
                    refreshPlayerDailyData(player, server, today, data);
                }
            }
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String rawDimString = event.getTo().identifier().toString();
            String dimId = rawDimString;

            QuestManager.handleAction(player, "CHANGE_DIMENSION", dimId);
            PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());

            if (!data.unlockedDimensions.contains(dimId)) {
                data.unlockedDimensions.add(dimId);
                String dimName = dimId.contains("nether") ? "Nether" : (dimId.contains("end") ? "End" : dimId);
                player.sendSystemMessage(Component.translatable("r3ct.message.dimension_discovered", "§l" + dimName));
                player.level().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
            }

            if (data.unlockedDimensions.contains("minecraft:overworld") &&
                    data.unlockedDimensions.contains("minecraft:the_nether") &&
                    data.unlockedDimensions.contains("minecraft:the_end")) {
                QuestManager.grantAdvancement(player, "r3ct:quests/dimension_master");
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            BlockState state = event.getState();
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

            if (QuestManager.removePlacedBlock(event.getPos(), serverPlayer.level())) {
                QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", blockId, -1);
                QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "any", -1);
                QuestManager.handleAction(serverPlayer, "PLACE_SAPLING", "any", -1);
                QuestManager.handleAction(serverPlayer, "PLACE_SEED", "any", -1);
                return;
            }

            QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", blockId);

            if (state.is(net.minecraft.tags.BlockTags.LEAVES)) QuestManager.handleAction(serverPlayer, "BREAK_LEAVES", "any", 1);
            if (state.is(net.minecraft.tags.BlockTags.JUNGLE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:jungle_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.PALE_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:pale_oak_logs", 1);
            if (blockId.contains("lapis_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:lapis_ores", 1);
            if (blockId.equals("minecraft:obsidian")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:obsidian", 1);
            if (blockId.equals("minecraft:nether_quartz_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:nether_quartz_ores", 1);
            if (blockId.equals("minecraft:cobweb")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:cobweb", 1);
            if (blockId.contains("diamond_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:diamond_ores", 1);
            if (state.is(net.minecraft.tags.BlockTags.GOLD_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:gold_ores", 1);
            if (state.is(net.minecraft.tags.BlockTags.FLOWERS)) QuestManager.handleAction(serverPlayer, "BREAK_FLOWER", "any", 1);
            if (state.is(net.minecraft.tags.BlockTags.OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:oak_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.BIRCH_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:birch_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.IRON_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:iron_ores", 1);
            if (state.is(net.minecraft.tags.BlockTags.COPPER_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:copper_ores", 1);
            if (state.is(net.minecraft.tags.BlockTags.ACACIA_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:acacia_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.DARK_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:dark_oak_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.MANGROVE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:mangrove_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.SPRUCE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:spruce_logs", 1);
            if (state.is(net.minecraft.tags.BlockTags.CHERRY_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:cherry_logs", 1);

            if (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) {
                if (serverPlayer.level().getBlockEntity(event.getPos()) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity beehive) {
                    if (!beehive.isEmpty()) {
                        ItemEnchantments enchantments = serverPlayer.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                        boolean hasSilkTouch = enchantments.keySet().stream().anyMatch(ench -> ench.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH));
                        if (hasSilkTouch) {
                            QuestManager.handleAction(serverPlayer, "SILK_TOUCH_BEE_NEST", "any", 1);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer)) attacker = event.getEntity().getLastHurtByMob();

        if (attacker instanceof ServerPlayer serverPlayer) {
            String mobId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
            QuestManager.handleAction(serverPlayer, "KILL_MOB", mobId);

            if (serverPlayer.getHealth() <= 4.0f) QuestManager.handleAction(serverPlayer, "KILL_LOW_HP", "any", 1);

            if (mobId.equals("minecraft:skeleton") && serverPlayer.level().dimension().identifier().toString().equals("minecraft:the_nether")) {
                QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_NETHER", "minecraft:skeleton", 1);
            }
            if (mobId.equals("minecraft:cave_spider")) QuestManager.handleAction(serverPlayer, "KILL_MOB", "minecraft:spider", 1);

            if (event.getEntity() instanceof net.minecraft.world.entity.monster.illager.Pillager pillager) {
                if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(pillager.blockPosition(), 9216);
                    if (raid == null) QuestManager.handleAction(serverPlayer, "KILL_MOB_NO_RAID", "minecraft:pillager", 1);
                }
            } else if (event.getEntity() instanceof net.minecraft.world.entity.monster.Ravager ravagerEntity) {
                if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(ravagerEntity.blockPosition(), 9216);
                    if (raid != null) QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_RAID", "minecraft:ravager", 1);
                }
            }
            if (event.getEntity() instanceof net.minecraft.world.entity.monster.Monster) {
                if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    if (serverLevel.isVillage(event.getEntity().blockPosition())) {
                        QuestManager.handleAction(serverPlayer, "KILL_MOB_VILLAGE", "any", 1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestManager.handleAction(serverPlayer, "SLEEP", "any", 1);
            if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (serverLevel.isVillage(serverPlayer.blockPosition())) {
                    QuestManager.handleAction(serverPlayer, "SLEEP_IN_VILLAGE", "any", 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getHand() == InteractionHand.MAIN_HAND) {
            ItemStack stack = serverPlayer.getItemInHand(event.getHand());
            String mobId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()).toString();

            if (mobId.equals("minecraft:cow") && stack.is(Items.BUCKET)) {
                if (event.getTarget() instanceof net.minecraft.world.entity.animal.cow.Cow cow && !cow.isBaby()) {
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
                if (event.getTarget() instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) {
                    if (!sheep.isBaby() && !sheep.isSheared()) {
                        QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:sheep_shear", 1);
                    }
                }
            }
            if (mobId.equals("minecraft:tropical_fish") && stack.is(Items.WATER_BUCKET)) {
                if (event.getTarget() instanceof net.minecraft.world.entity.animal.fish.TropicalFish fish && !fish.fromBucket()) {
                    QuestManager.handleAction(serverPlayer, "CATCH_FISH_BUCKET", "minecraft:tropical_fish", 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getHand() == InteractionHand.MAIN_HAND) {
            ItemStack stack = serverPlayer.getItemInHand(event.getHand());
            BlockState targetState = serverPlayer.level().getBlockState(event.getPos());

            if (stack.is(Items.BONE_MEAL)) QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:bone_meal", 1);
            if (stack.is(Items.FLINT_AND_STEEL)) {
                net.minecraft.core.BlockPos firePos = event.getPos().relative(event.getFace());
                if (serverPlayer.level().getBlockState(firePos).isAir() || targetState.is(Blocks.TNT) || targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
                    QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:flint_and_steel", 1);
                }
            }
            if (targetState.is(Blocks.RESPAWN_ANCHOR) && stack.is(Items.GLOWSTONE)) {
                int charges = targetState.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE);
                if (charges < 4) QuestManager.handleAction(serverPlayer, "CHARGE_RESPAWN_ANCHOR", "any", 1);
            }
            if (targetState.is(Blocks.BELL)) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:bell", 1);
            if (targetState.is(Blocks.STONECUTTER)) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:stonecutter", 1);
            if (targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
                if (stack.get(net.minecraft.core.component.DataComponents.FOOD) != null) {
                    if (serverPlayer.level().getBlockEntity(event.getPos()) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
                        boolean hasSpace = false;
                        for (ItemStack item : campfire.getItems()) {
                            if (item.isEmpty()) { hasSpace = true; break; }
                        }
                        if (hasSpace) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:campfire", 1);
                    }
                }
            }
            if (targetState.is(Blocks.COMPOSTER)) {
                if (targetState.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) == 8) {
                    QuestManager.handleAction(serverPlayer, "EMPTY_COMPOSTER", "any", 1);
                }
            }
            if (targetState.is(Blocks.TNT) && stack.is(Items.FLINT_AND_STEEL)) QuestManager.handleAction(serverPlayer, "IGNITE_TNT", "any", 1);
            if ((targetState.is(Blocks.BEE_NEST) || targetState.is(Blocks.BEEHIVE)) && stack.is(Items.GLASS_BOTTLE)) {
                if (targetState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 5) {
                    QuestManager.handleAction(serverPlayer, "COLLECT_HONEY", "any", 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityLoad(EntityJoinLevelEvent event) {
        if (event.getEntity().tickCount == 0) {
            if (event.getEntity() instanceof net.minecraft.world.entity.animal.golem.CopperGolem) {
                net.minecraft.world.entity.player.Player nearest = event.getLevel().getNearestPlayer(event.getEntity(), 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "copper", 1);
            }
            if (event.getEntity() instanceof net.minecraft.world.entity.animal.golem.IronGolem) {
                net.minecraft.world.entity.player.Player nearest = event.getLevel().getNearestPlayer(event.getEntity(), 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "iron", 1);
            }
            if (event.getEntity() instanceof net.minecraft.world.entity.animal.golem.SnowGolem) {
                net.minecraft.world.entity.player.Player nearest = event.getLevel().getNearestPlayer(event.getEntity(), 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "snow", 1);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Predicate<CommandSourceStack> isOp = source -> {
            if (source.getPlayer() != null) {
                NameAndId nameAndId = new NameAndId(source.getPlayer().getGameProfile());
                return source.getServer().getPlayerList().isOp(nameAndId);
            }
            return true;
        };

        event.getDispatcher().register(Commands.literal("rdq")
                .then(Commands.literal("reload").requires(isOp).executes(context -> {
                    ConfigLoader.loadAll();
                    context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.reload.success"), true);
                    return 1;
                }))
                .then(Commands.literal("forcecomplete").requires(isOp).then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.literal("all").executes(context -> {
                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                            boolean anyCompleted = false;
                            for (int i = 0; i < 5; i++) {
                                if (QuestManager.forceCompleteQuest(target, i, true)) anyCompleted = true;
                            }
                            if (anyCompleted) {
                                syncPlayerData(target, ModState.getPlayerData(context.getSource().getServer(), target.getUUID()));
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
                            if (success) context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_success", "§e" + (index + 1), "§b" + target.getName().getString()), true);
                            else context.getSource().sendSuccess(() -> Component.translatable("r3ct.command.forcecomplete.single_already_done", "§e" + (index + 1), "§b" + target.getName().getString()), false);
                            return 1;
                        }))
                ))
                .then(Commands.literal("quests").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.grantAdvancement(player, "r3ct:quests/root");
                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
                    com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new OpenQuestsPayload(
                            data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                            data.activeQuests, data.questProgress, data.streak,
                            data.perfectDaysCount, data.availableFreezes,
                            data.questRewardsClaimed, data.claimedPointRewards
                    ));
                    return 1;
                }))
                .then(Commands.literal("claimquest").then(Commands.argument("index", IntegerArgumentType.integer(0, 4)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.claimQuestReward(player, IntegerArgumentType.getInteger(context, "index"));
                    return 1;
                })))
                .then(Commands.literal("claimpoints").then(Commands.argument("prog", IntegerArgumentType.integer(0, 200)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.claimPointReward(player, IntegerArgumentType.getInteger(context, "prog"));
                    return 1;
                })))
                .then(Commands.literal("claimbonus").then(Commands.argument("dzien", IntegerArgumentType.integer(7, 21)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    RewardManager.claimBonusReward(player, IntegerArgumentType.getInteger(context, "dzien"));
                    return 1;
                })))
                .then(Commands.literal("rewards").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    QuestManager.grantAdvancement(player, "r3ct:rewards/root");
                    PlayerData data = ModState.getPlayerData(context.getSource().getServer(), player.getUUID());
                    LocalDate today = getCurrentQuestDate();
                    int visualStreak = data.streak;
                    if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty() && !data.lastStreakDate.equals(today.toString()) && !data.lastStreakDate.equals(today.minusDays(1).toString())) {
                        visualStreak = 0;
                    }
                    com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new OpenRewardsPayload(data.rewardDay, data.lastRewardDate, visualStreak, data.totalCollected, data.claimedRewardHistory, data.availableRewardFreezes, data.claimedBonusRewards));
                    return 1;
                }))
                .then(Commands.literal("claimreward").executes(context -> {
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
                    if (data.absoluteRewardStreak > data.maxRewardStreak) data.maxRewardStreak = data.absoluteRewardStreak;
                    if (data.streak == 7) QuestManager.grantAdvancement(player, "r3ct:rewards/always_on_time");
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

                    while (data.claimedRewardHistory.size() < 7) data.claimedRewardHistory.add("");
                    data.claimedRewardHistory.set(data.rewardDay - 1, historyBuilder.toString());
                    data.totalCollected++;

                    QuestManager.grantAdvancement(player, "r3ct:rewards/first_reward");
                    if (data.totalCollected >= 50) QuestManager.grantAdvancement(player, "r3ct:rewards/login_veteran");
                    if (data.rewardDay == 7) {
                        QuestManager.grantAdvancement(player, "r3ct:rewards/rich_week");
                        data.availableRewardFreezes++;
                        player.sendSystemMessage(Component.translatable("r3ct.message.rewards.shield_earned"));
                        if (data.availableRewardFreezes >= 3) QuestManager.grantAdvancement(player, "r3ct:rewards/shield_collector");
                    }

                    data.lastRewardDate = today.toString();
                    data.rewardDay = (data.rewardDay >= 7) ? 1 : data.rewardDay + 1;
                    context.getSource().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

                    com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new OpenRewardsPayload(data.rewardDay, data.lastRewardDate, data.streak, data.totalCollected, data.claimedRewardHistory, data.availableRewardFreezes, data.claimedBonusRewards));
                    return 1;
                }))
        );
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
        if (server != null) server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(target, new SyncQuestsPayload(
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
                        data.availableRewardFreezes -= (int) missed;
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
                data.availableFreezes -= (int) missedDays;
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

        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));

        player.sendSystemMessage(Component.translatable("r3ct.message.rewards.new_reward").append(Component.translatable("r3ct.message.click_here").withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/rdq rewards")).withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.rewards.open_menu"))))));
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.new_quests").append(Component.translatable("r3ct.message.click_here").withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/rdq quests")).withHoverEvent(new HoverEvent.ShowText(Component.translatable("r3ct.message.quests.open_menu"))))));
        for (Component msg : freezeMessages) player.sendSystemMessage(msg);
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        public static KeyMapping openRewardsKey;
        public static KeyMapping openQuestsKey;
        private static KeyMapping toggleHudKey;

        private static final KeyMapping.Category R3CT_CATEGORY = KeyMapping.Category.register(net.minecraft.resources.Identifier.parse(Constants.MOD_ID + ":main"));

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            QuestManager.init();
            com.r3ct.quests.config.R3CTQuestsConfig.load();
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            openRewardsKey = new KeyMapping("key.r3ct.open_rewards", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, R3CT_CATEGORY);
            openQuestsKey = new KeyMapping("key.r3ct.open_quests", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, R3CT_CATEGORY);
            toggleHudKey = new KeyMapping("key.r3ct.toggle_hud", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, R3CT_CATEGORY);

            event.register(openRewardsKey);
            event.register(openQuestsKey);
            event.register(toggleHudKey);
        }
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientGameEvents {
        private static boolean minimizedHud = false;
        public static PlayerData clientQuestData = null;
        public static final long[] flashTimestamps = new long[10];
        public static final boolean[] flashIsGreen = new boolean[10];

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            while (ClientModEvents.openRewardsKey.consumeClick()) client.player.connection.sendCommand("rdq rewards");
            while (ClientModEvents.openQuestsKey.consumeClick()) client.player.connection.sendCommand("rdq quests");
            while (ClientModEvents.toggleHudKey.consumeClick()) {
                minimizedHud = !minimizedHud;
                client.player.displayClientMessage(Component.translatable("r3ct.message.hud_toggle", minimizedHud ? "§4OFF" : "§aON"), true);
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui || client.getDebugOverlay().showDebugScreen() || client.player == null) return;
            if (client.screen != null && !(client.screen instanceof ChatScreen)) return;
            if (!com.r3ct.quests.config.R3CTQuestsConfig.getInstance().enableHud) return;

            int screenWidth = event.getGuiGraphics().guiWidth();
            int rawXOffset = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudXOffset;
            int rawYOffset = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudYOffset;

            double currentGuiScale = Math.max(1.0, client.getWindow().getGuiScale());
            float targetGuiScale = 2.0f;
            float scale = (float) (targetGuiScale / currentGuiScale);

            int alpha = 255;
            if (client.player.isSleeping()) {
                float sleepTimer = client.player.getSleepTimer();
                alpha = 255 - (int) ((sleepTimer / 100.0f) * 255);
            }
            if (alpha <= 5) return;

            int baseColor = (alpha << 24) | 0xFFFFFF;

            event.getGuiGraphics().pose().pushMatrix();
            event.getGuiGraphics().pose().scale(scale, scale);

            int virtualWidth = (int) (screenWidth / scale);
            int xOffset = (int) (rawXOffset / scale);
            int currentY = (int) (rawYOffset / scale);

            if (clientQuestData == null || clientQuestData.activeQuests.isEmpty()) {
                if (!minimizedHud) {
                    String loadingMsg = "§e" + Component.translatable("r3ct.hud.loading").getString();
                    event.getGuiGraphics().drawString(client.font, loadingMsg, virtualWidth - client.font.width(loadingMsg) - xOffset, currentY, baseColor, true);
                }
                event.getGuiGraphics().pose().popMatrix();
                return;
            }

            if (!minimizedHud) {
                String title = "§e§l" + Component.translatable("r3ct.quests.header.daily_quests").getString();
                event.getGuiGraphics().drawString(client.font, title, virtualWidth - client.font.width(title) - xOffset, currentY, baseColor, true);
            }
            currentY += 12;

            for (int i = 0; i < clientQuestData.activeQuests.size(); i++) {
                Quest q = QuestManager.getQuestById(clientQuestData.activeQuests.get(i));
                if (q == null) continue;

                int progress = clientQuestData.questProgress.get(i);
                boolean done = progress >= q.requiredAmount;

                String mark = done ? "§a" + Component.translatable("r3ct.quests.status.claimed").getString() : "§c" + Component.translatable("r3ct.quests.status.incomplete").getString();

                if (minimizedHud) {
                    event.getGuiGraphics().drawString(client.font, mark, virtualWidth - client.font.width(mark) - xOffset, currentY, baseColor, true);
                } else {
                    String questName = (q.name != null && !q.name.isEmpty()) ? Component.translatable(q.name).getString() : Component.translatable(q.description).getString().split(" ")[0];
                    String diffIndicator = (q.difficulty == 0) ? "§2★ " : (q.difficulty == 1 ? "§6★ " : "§4★ ");
                    String progressColor = "§f";

                    if (System.currentTimeMillis() - flashTimestamps[i] < 600) {
                        progressColor = flashIsGreen[i] ? "§a" : "§c";
                    } else if (done) {
                        progressColor = "§7";
                    }

                    String lineText = diffIndicator + progressColor + questName + " (" + progress + "/" + q.requiredAmount + ") " + mark;
                    event.getGuiGraphics().drawString(client.font, lineText, virtualWidth - client.font.width(lineText) - xOffset, currentY, baseColor, true);
                }
                currentY += 10;
            }
            event.getGuiGraphics().pose().popMatrix();
        }
    }

    public static class ClientPayloadHandlers {
        public static void handleOpenRewards(OpenRewardsPayload payload) {
            PlayerData data = new PlayerData();
            data.rewardDay = payload.rewardDay();
            data.lastRewardDate = payload.lastRewardDate();
            data.streak = payload.streak();
            data.totalCollected = payload.totalCollected();
            data.claimedRewardHistory = payload.claimedRewardHistory();
            data.availableRewardFreezes = payload.availableRewardFreezes();
            data.claimedBonusRewards = payload.claimedBonusRewards();
            Minecraft.getInstance().setScreen(new RewardScreen(data));
        }

        public static void handleOpenQuests(OpenQuestsPayload payload) {
            PlayerData data = new PlayerData();
            data.questStreak = payload.questStreak();
            data.totalQuestPoints = payload.totalQuestPoints();
            data.dailyQuestsCompletedToday = payload.dailyQuestsCompletedToday();
            data.activeQuests = payload.activeQuests();
            data.questProgress = payload.questProgress();
            data.streak = payload.streak();
            data.perfectDaysCount = payload.perfectDaysCount();
            data.availableFreezes = payload.availableFreezes();
            data.questRewardsClaimed = payload.questRewardsClaimed();
            data.claimedPointRewards = payload.claimedPointRewards();

            ClientGameEvents.clientQuestData = data;
            Minecraft.getInstance().setScreen(new QuestScreen(data));
        }

        public static void handleSyncQuests(SyncQuestsPayload payload) {
            if (ClientGameEvents.clientQuestData != null && ClientGameEvents.clientQuestData.questProgress != null) {
                for (int i = 0; i < payload.questProgress().size() && i < ClientGameEvents.clientQuestData.questProgress.size(); i++) {
                    int oldVal = ClientGameEvents.clientQuestData.questProgress.get(i);
                    int newVal = payload.questProgress().get(i);
                    if (newVal > oldVal) {
                        ClientGameEvents.flashTimestamps[i] = System.currentTimeMillis();
                        ClientGameEvents.flashIsGreen[i] = true;
                    } else if (newVal < oldVal) {
                        ClientGameEvents.flashTimestamps[i] = System.currentTimeMillis();
                        ClientGameEvents.flashIsGreen[i] = false;
                    }
                }
            }

            if (ClientGameEvents.clientQuestData == null) ClientGameEvents.clientQuestData = new PlayerData();
            ClientGameEvents.clientQuestData.questStreak = payload.questStreak();
            ClientGameEvents.clientQuestData.totalQuestPoints = payload.totalQuestPoints();
            ClientGameEvents.clientQuestData.dailyQuestsCompletedToday = payload.dailyQuestsCompletedToday();
            ClientGameEvents.clientQuestData.activeQuests = payload.activeQuests();
            ClientGameEvents.clientQuestData.questProgress = payload.questProgress();
            ClientGameEvents.clientQuestData.streak = payload.streak();
            ClientGameEvents.clientQuestData.perfectDaysCount = payload.perfectDaysCount();
            ClientGameEvents.clientQuestData.availableFreezes = payload.availableFreezes();
            ClientGameEvents.clientQuestData.availableRewardFreezes = payload.availableRewardFreezes();
            ClientGameEvents.clientQuestData.questRewardsClaimed = payload.questRewardsClaimed();
            ClientGameEvents.clientQuestData.claimedPointRewards = payload.claimedPointRewards();
        }

        public static void handleLeaderboardResponse(LeaderboardResponsePayload payload) {
            Minecraft.getInstance().setScreen(new LeaderboardScreen(payload.boardType(), payload.leftList(), payload.rightList()));
        }
    }
}