package com.r3ct.daily.logic;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.item.ModItems;
import com.r3ct.daily.network.SyncQuestsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestManager {
    public static final List<Quest> EASY_QUESTS = new ArrayList<>();
    public static final List<Quest> MEDIUM_QUESTS = new ArrayList<>();
    public static final List<Quest> HARD_QUESTS = new ArrayList<>();

    public static final Set<String> PLACED_BLOCKS = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.LinkedHashMap<String, Boolean>(1000, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> eldest) {
                    return size() > DailyServerConfig.mechanics.technical.placedBlocksCacheLimit;
                }
            })
    );

    public static Component getPrefix() {
        return Component.literal("[Daily] ").withStyle(net.minecraft.ChatFormatting.AQUA);
    }

    public static void addPlacedBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level level) {
        String key = level.dimension().identifier() + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ();
        PLACED_BLOCKS.add(key);
    }

    public static boolean removePlacedBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level level) {
        String key = level.dimension().identifier() + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ();
        return PLACED_BLOCKS.remove(key);
    }

    public static void init() {}

    public static Quest getQuestById(String id) {
        for (Quest q : EASY_QUESTS) if (q.id.equals(id)) return q;
        for (Quest q : MEDIUM_QUESTS) if (q.id.equals(id)) return q;
        for (Quest q : HARD_QUESTS) if (q.id.equals(id)) return q;
        return null;
    }

    public static List<Quest> generateDailyQuests(PlayerData data, java.util.UUID playerUuid, java.time.LocalDate date) {

        long mostSigBits = playerUuid.getMostSignificantBits();
        long leastSigBits = playerUuid.getLeastSignificantBits();

        long epochDay = date.toEpochDay();
        long seed = mostSigBits ^ leastSigBits ^ (epochDay * 0x9E3779B97F4A7C15L);

        seed = (seed ^ (seed >>> 30)) * 0xBF58476D1CE4E5B9L;
        seed = (seed ^ (seed >>> 27)) * 0x94D049BB133111EBL;
        seed = seed ^ (seed >>> 31);

        java.util.Random random = new java.util.Random(seed);

        List<Quest> daily = new ArrayList<>();

        if (data.unlockedDimensions.isEmpty()) {
            data.unlockedDimensions.add("minecraft:overworld");
        }

        List<Quest> availableEasy = filterByDimension(EASY_QUESTS, data.unlockedDimensions);
        List<Quest> availableMedium = filterByDimension(MEDIUM_QUESTS, data.unlockedDimensions);
        List<Quest> availableHard = filterByDimension(HARD_QUESTS, data.unlockedDimensions);

        if (!availableEasy.isEmpty()) {
            Collections.shuffle(availableEasy, random);
            daily.add(availableEasy.get(0));
            if (availableEasy.size() > 1) daily.add(availableEasy.get(1));
        }

        if (!availableMedium.isEmpty()) {
            Collections.shuffle(availableMedium, random);
            daily.add(availableMedium.get(0));
            if (availableMedium.size() > 1) daily.add(availableMedium.get(1));
        }

        if (!availableHard.isEmpty()) {
            Collections.shuffle(availableHard, random);
            daily.add(availableHard.get(0));
        }

        while (daily.size() < 5 && !availableEasy.isEmpty()) {
            if (daily.containsAll(availableEasy)) break;
            Quest extra = availableEasy.get(random.nextInt(availableEasy.size()));
            if (!daily.contains(extra)) daily.add(extra);
        }

        return daily;
    }

    private static List<Quest> filterByDimension(List<Quest> pool, List<String> unlocked) {
        return pool.stream().filter(q -> {
            if (q.requiredDimension == null || q.requiredDimension.isEmpty() || q.requiredDimension.equals("minecraft:overworld")) {
                return unlocked.contains("minecraft:overworld");
            }
            return unlocked.contains(q.requiredDimension);
        }).collect(Collectors.toList());
    }

    public static void handleAction(ServerPlayer player, String actionType, String target) {
        handleAction(player, actionType, target, 1);
    }

    public static void handleAction(ServerPlayer player, String actionType, String target, int amount) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;

        if (actionType.contains("DISTANCE") && !hasQuestTypeActive(player, actionType)) {
            return;
        }

        PlayerData data = ModState.getPlayerData(server, player.getUUID());
        boolean needsSync = false;

        for (int i = 0; i < data.activeQuests.size(); i++) {
            Quest q = getQuestById(data.activeQuests.get(i));

            if (q != null && q.actionType.equals(actionType)) {
                boolean targetMatches = q.target.equals("any") || q.target.equals(target);

                if (!targetMatches && (actionType.equals("VISIT_BIOME") || actionType.equals("TIME_IN_BIOME"))) {
                    if (target.contains(q.target)) {
                        targetMatches = true;
                    }
                }

                if (targetMatches) {
                    int oldProg = data.questProgress.get(i);

                    if (oldProg < q.requiredAmount) {
                        int newProg;

                        if (actionType.equals("HAS_ITEMS")) {
                            newProg = amount;
                        } else if (actionType.equals("PEARL_DISTANCE") || actionType.equals("LEVITATION_HEIGHT")) {
                            newProg = Math.max(oldProg, amount);
                        } else {
                            newProg = oldProg + amount;
                        }

                        if (newProg > q.requiredAmount) newProg = q.requiredAmount;
                        if (newProg < 0) newProg = 0;

                        if (newProg != oldProg) {
                            data.questProgress.set(i, newProg);

                            boolean isDistance = actionType.contains("DISTANCE") ||
                                    actionType.equals("ELYTRA_FLIGHT_NO_LAND") ||
                                    actionType.equals("LEVITATION_HEIGHT") ||
                                    actionType.equals("PEARL_DISTANCE");

                            int syncInterval = actionType.equals("ELYTRA_FLIGHT_NO_LAND") ? 50 : 5;
                            boolean passedInterval = (newProg / syncInterval) > (oldProg / syncInterval);

                            if (!isDistance || newProg >= q.requiredAmount || passedInterval) {
                                needsSync = true;
                            }

                            if (newProg >= q.requiredAmount) completeQuest(player, data, q);
                        }
                    }
                }
            }
        }

        if (needsSync) {
            server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
            Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                    data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                    data.activeQuests, data.questProgress, data.streak,
                    data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                    data.questRewardsClaimed, data.claimedPointRewards
            ));
        }
    }

    private static void completeQuest(ServerPlayer player, PlayerData data, Quest q) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));
        Component questNameComp = Component.translatable(q.name).withStyle(net.minecraft.ChatFormatting.YELLOW);
        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.quests.completed", questNameComp).withStyle(net.minecraft.ChatFormatting.GREEN)
        ));
    }

    public static void submitQuestItem(ServerPlayer player, int questIndex, int slotIndex) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (questIndex < 0 || questIndex >= data.activeQuests.size()) return;
        if (data.questRewardsClaimed.get(questIndex)) return;

        Quest q = getQuestById(data.activeQuests.get(questIndex));
        if (q == null || !q.actionType.equals("SUBMIT_ITEMS")) return;

        int currentProg = data.questProgress.get(questIndex);
        if (currentProg >= q.requiredAmount) return;

        int needed = q.requiredAmount - currentProg;
        int taken = 0;

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        if (slotIndex >= 0) {
            ItemStack stack = inv.getItem(slotIndex);
            if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(q.target)) {
                int toTake = Math.min(needed, stack.getCount());
                stack.shrink(toTake);
                taken += toTake;
            }
        } else {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(q.target)) {
                    int toTake = Math.min(needed - taken, stack.getCount());
                    stack.shrink(toTake);
                    taken += toTake;
                    if (taken >= needed) break;
                }
            }
        }

        if (taken > 0) {
            int newProg = currentProg + taken;
            data.questProgress.set(questIndex, newProg);

            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    0.5F, 1.0F, player.getRandom().nextLong()
            ));

            if (newProg >= q.requiredAmount) {
                completeQuest(player, data, q);
            }

            server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
            com.r3ct.daily.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                    data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                    data.activeQuests, data.questProgress, data.streak,
                    data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                    data.questRewardsClaimed, data.claimedPointRewards
            ));
        }
    }

    public static void claimQuestReward(ServerPlayer player, int index) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (index < 0 || index >= data.activeQuests.size()) return;
        if (data.questRewardsClaimed.get(index)) return;

        Quest q = getQuestById(data.activeQuests.get(index));
        if (q == null) return;

        if (data.questProgress.get(index) < q.requiredAmount) return;

        data.questRewardsClaimed.set(index, true);

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));

        int multi = (data.questStreak >= 7) ? 2 : 1;

        int baseXp = (q.difficulty == 0) ? DailyServerConfig.mechanics.quests.xpPerQuestEasy :
                (q.difficulty == 1) ? DailyServerConfig.mechanics.quests.xpPerQuestMedium :
                        DailyServerConfig.mechanics.quests.xpPerQuestHard;

        int xpReward = baseXp * multi;
        player.giveExperiencePoints(xpReward);
        if (q.difficulty == 2) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/hard_work");
        }

        int amountGiven = q.rewardAmount * multi;
        ItemStack rewardToGive;

        if (q.rawRewardId != null && (q.rawRewardId.startsWith("r3ct_daily:"))) {
            rewardToGive = RewardManager.getCustomReward(q.rawRewardId, amountGiven, server);
        } else {
            rewardToGive = q.getItemReward();
            rewardToGive.setCount(amountGiven);
        }

        int maxStack = rewardToGive.getMaxStackSize();
        int remainingToGive = amountGiven;

        while (remainingToGive > 0) {
            int currentStackSize = Math.min(remainingToGive, maxStack);

            ItemStack splitStack = rewardToGive.copy();
            splitStack.setCount(currentStackSize);

            giveOrDrop(player, splitStack);

            remainingToGive -= currentStackSize;
        }

        Component questNameComp = Component.translatable(q.name).withStyle(ChatFormatting.YELLOW);
        Component multiComp = (multi > 1) ? Component.translatable("r3ct_daily.message.quests.streak_bonus").withStyle(ChatFormatting.GOLD) : Component.empty();

        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.quests.claimed_base", questNameComp, multiComp).withStyle(ChatFormatting.GREEN)
        ));

        if (xpReward > 0) {
            Component bulletComp = Component.literal("- ").withStyle(ChatFormatting.GREEN);
            Component xpComp = Component.literal(xpReward + " ").append(Component.translatable("r3ct_daily.unit.xp")).withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(bulletComp).append(xpComp));
        }

        if (!rewardToGive.isEmpty()) {
            Component amountComp = Component.literal(String.valueOf(amountGiven)).withStyle(ChatFormatting.AQUA);
            Component itemNameComp = rewardToGive.getHoverName().copy().withStyle(ChatFormatting.AQUA);

            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.quests.item_gained", amountComp, itemNameComp).withStyle(ChatFormatting.GREEN)
            ));
        }

        data.totalQuestPoints += q.points;
        data.dailyQuestsCompletedToday++;
        data.totalQuestsCompleted++;
        if (data.totalQuestsCompleted >= 100) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/hundred_quests");
        }
        if (data.dailyQuestsCompletedToday == 3) giveDailyReward(player, data);

        if (data.dailyQuestsCompletedToday == 5) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/perfect_day");
            data.perfectDaysCount++;

            if (data.perfectDaysCount >= DailyServerConfig.mechanics.streaks.perfectDaysForShield) {
                data.perfectDaysCount = 0;
                QuestManager.giveOrDrop(player, new ItemStack(ModItems.QUEST_SHIELD));
                player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                        Component.translatable("r3ct_daily.message.quests.shield_item_received").withStyle(net.minecraft.ChatFormatting.GREEN)
                ));
            } else {
                Component daysComp = Component.literal(String.valueOf(data.perfectDaysCount)).withStyle(net.minecraft.ChatFormatting.AQUA);
                Component maxDaysComp = Component.literal(String.valueOf(DailyServerConfig.mechanics.streaks.perfectDaysForShield)).withStyle(net.minecraft.ChatFormatting.AQUA);
                Component shieldComp = Component.translatable("item.r3ct_daily.quest_shield").withStyle(net.minecraft.ChatFormatting.AQUA);

                player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                        Component.translatable("r3ct_daily.message.quests.perfect_day", daysComp, maxDaysComp, shieldComp).withStyle(net.minecraft.ChatFormatting.GREEN)
                ));
            }
        }
        QuestManager.grantAdvancement(player, "r3ct_daily:quests/first_quest");

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));
    }

    public static void claimPointReward(ServerPlayer player, int threshold) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (data.totalQuestPoints < threshold) return;
        if (data.claimedPointRewards.contains(threshold)) return;

        data.claimedPointRewards.add(threshold);

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));

        DailyServerConfig.MilestoneReward mr = null;
        if (threshold == 50) {
            mr = DailyServerConfig.milestones.point_50;
        } else if (threshold == 100) {
            mr = DailyServerConfig.milestones.point_100;
        } else if (threshold == 150) {
            mr = DailyServerConfig.milestones.point_150;
        } else if (threshold == 200) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/points_hunter");
            mr = DailyServerConfig.milestones.point_200;
        }

        if (mr != null) {
            ItemStack stack = getMilestoneRewardStack(mr);
            String colorStr = mr.getFormattedColor();
            net.minecraft.ChatFormatting format = net.minecraft.ChatFormatting.getByCode(colorStr.charAt(colorStr.length() - 1));
            if (format == null) format = net.minecraft.ChatFormatting.WHITE;

            Component itemNameComp = stack.getHoverName().copy().withStyle(format);
            net.minecraft.network.chat.MutableComponent rewardText = Component.literal("(" + mr.amount + "x ")
                    .append(itemNameComp)
                    .append(")")
                    .withStyle(format);

            giveOrDrop(player, stack);

            Component thresholdComp = Component.literal(String.valueOf(threshold)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.points.claim", thresholdComp, rewardText).withStyle(net.minecraft.ChatFormatting.GREEN)
            ));
        }

        if (threshold == 200) {
            data.totalQuestPoints -= 200;
            data.claimedPointRewards.clear();
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.points.reset").withStyle(net.minecraft.ChatFormatting.GRAY)
            ));
        }

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));
    }

    private static void giveDailyReward(ServerPlayer player, PlayerData data) {
        int multi = (data.questStreak >= 7) ? 2 : 1;
        data.questStreak++;
        if (data.questStreak > data.maxQuestStreak) {
            data.maxQuestStreak = data.questStreak;
        }
        data.lastQuestStreakDate = java.time.LocalDate.now().toString();

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.2F, player.getRandom().nextLong()
        ));
        Component multiComp = (multi > 1) ? Component.translatable("r3ct_daily.message.quests.streak_bonus").withStyle(net.minecraft.ChatFormatting.GOLD) : Component.empty();
        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.quests.daily_reward", multiComp).withStyle(net.minecraft.ChatFormatting.GREEN)
        ));

        int xp = DailyServerConfig.mechanics.quests.xpDailyReward * multi;
        player.giveExperiencePoints(xp);

        Component bulletComp2 = Component.literal("- ").withStyle(ChatFormatting.GREEN);
        Component xpComp2 = Component.literal(xp + " ").append(Component.translatable("r3ct_daily.unit.xp")).withStyle(ChatFormatting.YELLOW);
        player.sendSystemMessage(Component.empty().append(getPrefix()).append(bulletComp2).append(xpComp2));

        if (DailyServerConfig.dailyQuestRewards.isEmpty()) return;

        Random rand = new Random();
        int totalWeight = 0;
        for (DailyServerConfig.RewardEntry entry : DailyServerConfig.dailyQuestRewards) totalWeight += entry.chance;

        DailyServerConfig.RewardEntry selectedEntry = DailyServerConfig.dailyQuestRewards.get(0);
        if (totalWeight > 0) {
            int roll = rand.nextInt(totalWeight);
            int cursor = 0;
            for (DailyServerConfig.RewardEntry entry : DailyServerConfig.dailyQuestRewards) {
                cursor += entry.chance;
                if (roll < cursor) { selectedEntry = entry; break; }
            }
        }

        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse(selectedEntry.item)).orElse(Items.COAL);
        int baseAmount = selectedEntry.minAmount + rand.nextInt(Math.max(1, selectedEntry.maxAmount - selectedEntry.minAmount + 1));

        int finalAmount = baseAmount * multi;
        ItemStack reward = new ItemStack(item, finalAmount);

        if (item == Items.DIAMOND) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/lucky_drop");
        }
        if (data.questStreak == 7) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/week_streak");
        }

        Component amountComp = Component.literal(String.valueOf(finalAmount)).withStyle(net.minecraft.ChatFormatting.AQUA);
        Component itemNameComp = reward.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.AQUA);

        QuestManager.giveOrDrop(player, reward);

        player.sendSystemMessage(Component.empty().append(getPrefix()).append("- ").withStyle(net.minecraft.ChatFormatting.GRAY).append(
                Component.translatable("r3ct_daily.message.quests.item_gained", amountComp, itemNameComp).withStyle(net.minecraft.ChatFormatting.GREEN)
        ));
    }

    private static boolean hasQuestTypeActive(ServerPlayer player, String type) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        PlayerData data = ModState.getPlayerData(server, player.getUUID());
        for (String qId : data.activeQuests) {
            Quest q = getQuestById(qId);
            if (q != null && q.actionType.equals(type)) return true;
        }
        return false;
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public static ItemStack getMilestoneRewardStack(DailyServerConfig.MilestoneReward mr) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.Identifier.parse(mr.item)
        ).orElse(net.minecraft.world.item.Items.PAPER);
        return new ItemStack(item, mr.amount);
    }

    public static void grantAdvancement(ServerPlayer player, String advancementId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;

        var advancementHolder = server.getAdvancements().get(net.minecraft.resources.Identifier.parse(advancementId));
        if (advancementHolder != null) {
            var progress = player.getAdvancements().getOrStartProgress(advancementHolder);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancementHolder, criterion);
                }
            }
        }
    }

    public static void rerollQuest(ServerPlayer player, int index) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (!DailyServerConfig.mechanics.quests.enableQuestRerolling) {
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.reroll.disabled").withStyle(net.minecraft.ChatFormatting.RED)
            ));
            return;
        }

        if (index < 0 || index >= data.activeQuests.size()) return;
        if (data.questRewardsClaimed.get(index) || data.questProgress.get(index) >= getQuestById(data.activeQuests.get(index)).requiredAmount) {
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.reroll.already_completed").withStyle(net.minecraft.ChatFormatting.GREEN)
            ));
            return;
        }

        String oldQuestId = data.activeQuests.get(index);
        Quest oldQuest = getQuestById(oldQuestId);
        if (oldQuest == null) return;

        int cost = (oldQuest.difficulty == 0) ? DailyServerConfig.mechanics.quests.rerollCostEasy :
                (oldQuest.difficulty == 1) ? DailyServerConfig.mechanics.quests.rerollCostMedium :
                        DailyServerConfig.mechanics.quests.rerollCostHard;

        Component costComp = Component.literal(String.valueOf(cost)).withStyle(net.minecraft.ChatFormatting.RED);
        Component pointsComp = Component.literal(String.valueOf(data.totalQuestPoints)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

        if (data.totalQuestPoints < cost) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value()),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    1.0F, 1.0F, player.getRandom().nextLong()
            ));
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.reroll.not_enough_points", costComp, pointsComp).withStyle(net.minecraft.ChatFormatting.GREEN)
            ));
            return;
        }

        List<Quest> pool;
        if (oldQuest.difficulty == 0) pool = EASY_QUESTS;
        else if (oldQuest.difficulty == 1) pool = MEDIUM_QUESTS;
        else pool = HARD_QUESTS;

        List<Quest> available = filterByDimension(pool, data.unlockedDimensions);

        List<Quest> candidates = available.stream()
                .filter(q -> !data.activeQuests.contains(q.id))
                .collect(java.util.stream.Collectors.toList());

        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.reroll.no_quests_available").withStyle(net.minecraft.ChatFormatting.GREEN)
            ));
            return;
        }

        data.totalQuestPoints -= cost;

        java.util.Random random = new java.util.Random();
        Quest newQuest = candidates.get(random.nextInt(candidates.size()));

        data.activeQuests.set(index, newQuest.id);
        data.questProgress.set(index, 0);
        data.questRewardsClaimed.set(index, false);

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value()),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));

        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.reroll.success", costComp, pointsComp).withStyle(net.minecraft.ChatFormatting.GREEN)
        ));

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));
    }

    public static boolean forceCompleteQuest(ServerPlayer player, int index, boolean skipSync) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (index < 0 || index >= data.activeQuests.size()) return false;

        Quest q = getQuestById(data.activeQuests.get(index));
        if (q == null) return false;

        int oldProg = data.questProgress.get(index);
        if (oldProg < q.requiredAmount) {
            data.questProgress.set(index, q.requiredAmount);
            completeQuest(player, data, q);

            if (!skipSync) {
                server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
                Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                        data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                        data.activeQuests, data.questProgress, data.streak,
                        data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                        data.questRewardsClaimed, data.claimedPointRewards
                ));
            }
            return true;
        }
        return false;
    }

    public static java.time.LocalDate getCurrentQuestDate() {
        return java.time.LocalDateTime.now()
                .minusHours(DailyServerConfig.mechanics.technical.questRefreshHour)
                .toLocalDate();
    }

    public static void refreshPlayerDailyData(ServerPlayer player, net.minecraft.server.MinecraftServer server, java.time.LocalDate today, PlayerData data) {
        String todayStr = today.toString();
        java.time.LocalDate yesterday = today.minusDays(1);
        List<Component> freezeMessages = new ArrayList<>();

        if (!todayStr.equals(data.lastRewardDate)) {
            if (!data.lastStreakDate.equals(yesterday.toString()) && !data.lastStreakDate.equals(todayStr)) {
                if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(data.lastStreakDate), today);
                    long missed = days - 1;

                    if (data.availableRewardFreezes >= missed) {
                        data.availableRewardFreezes -= (int) missed;
                        data.lastStreakDate = yesterday.toString();
                        Component missedRewardComp = Component.literal(String.valueOf(missed)).withStyle(net.minecraft.ChatFormatting.AQUA);
                        freezeMessages.add(Component.empty().append(getPrefix()).append(
                                Component.translatable("r3ct_daily.message.rewards.freeze_used", missedRewardComp).withStyle(net.minecraft.ChatFormatting.GREEN)
                        ));
                        QuestManager.grantAdvancement(player, "r3ct_daily:rewards/safe_player");
                    } else {
                        data.streak = 0;
                        data.availableRewardFreezes = 0;
                        data.absoluteRewardStreak = 0;
                        freezeMessages.add(Component.empty().append(getPrefix()).append(
                                Component.translatable("r3ct_daily.message.rewards.streak_reset").withStyle(net.minecraft.ChatFormatting.RED)
                        ));
                    }
                }
            }
        }

        if (data.lastQuestStreakDate != null && !data.lastQuestStreakDate.isEmpty() && !data.lastQuestStreakDate.equals(yesterday.toString()) && !data.lastQuestStreakDate.equals(todayStr)) {
            java.time.LocalDate lastStreak = java.time.LocalDate.parse(data.lastQuestStreakDate);
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastStreak, today);
            long missedDays = daysBetween - 1;

            if (data.availableFreezes >= missedDays) {
                data.availableFreezes -= (int) missedDays;
                data.lastQuestStreakDate = yesterday.toString();
                Component missedComp = Component.literal(String.valueOf(missedDays)).withStyle(net.minecraft.ChatFormatting.AQUA);
                freezeMessages.add(Component.empty().append(getPrefix()).append(
                        Component.translatable("r3ct_daily.message.quests.freeze_used", missedComp).withStyle(net.minecraft.ChatFormatting.GREEN)
                ));
                QuestManager.grantAdvancement(player, "r3ct_daily:quests/time_lord");
            } else {
                data.questStreak = 0;
                data.availableFreezes = 0;
                freezeMessages.add(Component.empty().append(getPrefix()).append(
                        Component.translatable("r3ct_daily.message.quests.streak_reset").withStyle(net.minecraft.ChatFormatting.RED)
                ));
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

        Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                data.activeQuests, data.questProgress, data.streak,
                data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                data.questRewardsClaimed, data.claimedPointRewards
        ));

        Component clickHereRewardsComp = Component.translatable("r3ct_daily.message.click_here")
                .withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/daily rewards"))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.translatable("r3ct_daily.message.rewards.open_menu"))));

        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.rewards.new_reward").withStyle(net.minecraft.ChatFormatting.GREEN)
        ).append(clickHereRewardsComp));

        Component clickHereQuestsComp = Component.translatable("r3ct_daily.message.click_here")
                .withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/daily quests"))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.translatable("r3ct_daily.message.quests.open_menu"))));

        player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                Component.translatable("r3ct_daily.message.quests.new_quests").withStyle(net.minecraft.ChatFormatting.GREEN)
        ).append(clickHereQuestsComp));

        for (Component msg : freezeMessages) player.sendSystemMessage(msg);
    }

    public static void resetQuestProgress(ServerPlayer player, String actionType) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());
        boolean needsSync = false;

        for (int i = 0; i < data.activeQuests.size(); i++) {
            Quest q = getQuestById(data.activeQuests.get(i));
            if (q != null && q.actionType.equals(actionType)) {
                int oldProg = data.questProgress.get(i);

                if (oldProg < q.requiredAmount && oldProg > 0) {
                    data.questProgress.set(i, 0);
                    needsSync = true;
                }
            }
        }

        if (needsSync) {
            server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
            Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
                    data.questStreak, data.totalQuestPoints, data.dailyQuestsCompletedToday,
                    data.activeQuests, data.questProgress, data.streak,
                    data.perfectDaysCount, data.availableFreezes, data.availableRewardFreezes,
                    data.questRewardsClaimed, data.claimedPointRewards
            ));
        }
    }
}