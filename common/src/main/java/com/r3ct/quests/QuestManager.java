package com.r3ct.quests;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QuestManager {
    public static final List<Quest> EASY_QUESTS = new ArrayList<>();
    public static final List<Quest> MEDIUM_QUESTS = new ArrayList<>();
    public static final List<Quest> HARD_QUESTS = new ArrayList<>();

    public static final Set<String> PLACED_BLOCKS = ConcurrentHashMap.newKeySet();

    public static void addPlacedBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.Level level) {
        if (PLACED_BLOCKS.size() > ConfigLoader.mechanics.technical.placedBlocksCacheLimit) PLACED_BLOCKS.clear();
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

        long seed = playerUuid.hashCode() + date.toEpochDay();
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
            Quest extra = availableEasy.get(random.nextInt(availableEasy.size()));
            if (!daily.contains(extra)) daily.add(extra);
            else if (availableEasy.size() < daily.size()) break;
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

                if (!targetMatches && actionType.equals("VISIT_BIOME")) {
                    if (target.contains(q.target)) {
                        targetMatches = true;
                    }
                }

                if (targetMatches) {
                    int oldProg = data.questProgress.get(i);

                    if (oldProg < q.requiredAmount) {
                        int newProg;

                        if (actionType.equals("HAS_ITEMS") || actionType.equals("PICKUP_ITEM") || actionType.equals("ELYTRA_FLIGHT_NO_LAND")) {
                            newProg = amount;
                        } else {
                            newProg = oldProg + amount;
                        }

                        if (newProg > q.requiredAmount) newProg = q.requiredAmount;
                        if (newProg < 0) newProg = 0;

                        if (newProg != oldProg) {
                            data.questProgress.set(i, newProg);

                            if (!actionType.contains("DISTANCE") || newProg >= q.requiredAmount || (newProg) > (oldProg)) {
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
            com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.completed", "§f" + Component.translatable(q.name).getString()));
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

        int baseXp = (q.difficulty == 0) ? ConfigLoader.mechanics.quests.xpPerQuestEasy :
                (q.difficulty == 1) ? ConfigLoader.mechanics.quests.xpPerQuestMedium :
                        ConfigLoader.mechanics.quests.xpPerQuestHard;

        int xpReward = baseXp * multi;
        player.giveExperiencePoints(xpReward);
        if (q.difficulty == 2) {
            QuestManager.grantAdvancement(player, "r3ct:quests/hard_work");
        }

        int amountGiven = q.itemReward.getCount() * multi;
        ItemStack rewardToGive = q.itemReward.copy();
        rewardToGive.setCount(amountGiven);

        if (q.rawRewardId != null && q.rawRewardId.startsWith("r3ct:")) {
            Random rand = new Random();

            if (q.rawRewardId.equals("r3ct:random_dye")) {
                Item[] dyes = {Items.RED_DYE, Items.GREEN_DYE, Items.BLUE_DYE, Items.YELLOW_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.PINK_DYE, Items.LIME_DYE, Items.CYAN_DYE, Items.PURPLE_DYE};
                rewardToGive = new ItemStack(dyes[rand.nextInt(dyes.length)], rewardToGive.getCount());
            } else if (q.rawRewardId.equals("r3ct:random_head")) {
                Item[] heads = {Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.CREEPER_HEAD, Items.PIGLIN_HEAD};
                rewardToGive = new ItemStack(heads[rand.nextInt(heads.length)], rewardToGive.getCount());
            } else if (q.rawRewardId.equals("r3ct:random_sapling")) {
                Item[] saplings = {Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.CHERRY_SAPLING};
                rewardToGive = new ItemStack(saplings[rand.nextInt(saplings.length)], rewardToGive.getCount());
            }
            else if (q.rawRewardId.equals("r3ct:random_potion")) {
                var potionLookup = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.POTION);
                var potionList = potionLookup.listElements().toList();
                var randomPotion = potionList.get(rand.nextInt(potionList.size()));

                ItemStack potStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, randomPotion);
                potStack.setCount(rewardToGive.getCount());
                rewardToGive = potStack;
            }
            else if (q.rawRewardId.equals("r3ct:random_coral_block")) {
                Item[] corals = {Items.BRAIN_CORAL_BLOCK, Items.BUBBLE_CORAL_BLOCK, Items.FIRE_CORAL_BLOCK, Items.HORN_CORAL_BLOCK, Items.TUBE_CORAL_BLOCK};
                rewardToGive = new ItemStack(corals[rand.nextInt(corals.length)], rewardToGive.getCount());
            }
            else if (q.rawRewardId.equals("r3ct:random_carpet")) {
                Item[] carpets = {
                        Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET,
                        Items.YELLOW_CARPET, Items.LIME_CARPET, Items.PINK_CARPET, Items.GRAY_CARPET,
                        Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
                        Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET
                };
                rewardToGive = new ItemStack(carpets[rand.nextInt(carpets.length)], rewardToGive.getCount());
            }
            else if (q.rawRewardId.equals("r3ct:random_wool")) {
                Item[] wools = {Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL};
                rewardToGive = new ItemStack(wools[rand.nextInt(wools.length)], rewardToGive.getCount());
            }
            else if (q.rawRewardId.equals("r3ct:unbreaking_2_book")) {
                ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var unbreaking = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book, builder -> {
                    builder.set(unbreaking, 2);
                });
                rewardToGive = book;
            }
            else if (q.rawRewardId.equals("r3ct:efficiency_3_book")) {
                ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book, builder -> {
                    builder.set(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 3);
                });
                rewardToGive = book;
            }
            else if (q.rawRewardId.equals("r3ct:firework_tier_3")) {
                ItemStack rockets = new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET, rewardToGive.getCount());
                rockets.set(net.minecraft.core.component.DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(3, java.util.List.of()));
                rewardToGive = rockets;
            }
            else if (q.rawRewardId.equals("r3ct:healing_2_potion")) {
                ItemStack potion = new ItemStack(net.minecraft.world.item.Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.STRONG_HEALING));
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:infinity_book")) {
                ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book, builder -> {
                    builder.set(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY), 1);
                });
                rewardToGive = book;
            }
            else if (q.rawRewardId.equals("r3ct:feather_falling_3_book")) {
                ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book, builder -> {
                    builder.set(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING), 3);
                });
                rewardToGive = book;
            }
            else if (q.rawRewardId.equals("r3ct:water_breathing_potion")) {
                ItemStack potion = new ItemStack(net.minecraft.world.item.Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER_BREATHING)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:fire_resistance_potion")) {
                ItemStack potion = new ItemStack(net.minecraft.world.item.Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.FIRE_RESISTANCE)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:slow_falling_potion")) {
                ItemStack potion = new ItemStack(net.minecraft.world.item.Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SLOW_FALLING)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:night_vision_potion")) {
                ItemStack potion = new ItemStack(net.minecraft.world.item.Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.NIGHT_VISION)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:sharpness_2_book")) {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book, builder -> {
                    builder.set(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 2);
                });
                rewardToGive = book;
            }
            else if (q.rawRewardId.equals("r3ct:regeneration_potion")) {
                ItemStack potion = new ItemStack(Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.REGENERATION)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:speed_potion")) {
                ItemStack potion = new ItemStack(Items.POTION);
                potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SWIFTNESS)
                );
                rewardToGive = potion;
            }
            else if (q.rawRewardId.equals("r3ct:random_pottery_sherd")) {
                Item[] sherds = {Items.ANGLER_POTTERY_SHERD, Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD};
                rewardToGive = new ItemStack(sherds[player.getRandom().nextInt(sherds.length)], rewardToGive.getCount());
            }
            else if (q.rawRewardId.equals("r3ct:random_job_block")) {
                net.minecraft.world.item.Item[] blocks = {
                        net.minecraft.world.item.Items.LECTERN,
                        net.minecraft.world.item.Items.COMPOSTER,
                        net.minecraft.world.item.Items.BARREL,
                        net.minecraft.world.item.Items.LOOM,
                        net.minecraft.world.item.Items.SMOKER,
                        net.minecraft.world.item.Items.FLETCHING_TABLE,
                        net.minecraft.world.item.Items.GRINDSTONE,
                        net.minecraft.world.item.Items.BLAST_FURNACE,
                        net.minecraft.world.item.Items.STONECUTTER
                };
                rewardToGive = new ItemStack(blocks[player.getRandom().nextInt(blocks.length)]);
            }
        }

        String itemName = rewardToGive.getHoverName().getString();
        player.getInventory().add(rewardToGive);
        if (!rewardToGive.isEmpty()) {
            player.drop(rewardToGive, false);
        }

        String multiStr = (multi > 1) ? Component.translatable("r3ct.message.quests.streak_bonus").getString() : "";
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.claimed",
                "§f" + Component.translatable(q.name).getString(),
                "§e" + xpReward,
                "§b" + amountGiven,
                "§b" + itemName,
                multiStr
        ));

        data.totalQuestPoints += q.points;
        data.dailyQuestsCompletedToday++;
        data.totalQuestsCompleted++;
        if (data.totalQuestsCompleted >= 100) {
            QuestManager.grantAdvancement(player, "r3ct:quests/hundred_quests");
        }
        if (data.dailyQuestsCompletedToday == 3) giveDailyReward(player, data);

        if (data.dailyQuestsCompletedToday == 5) {
            QuestManager.grantAdvancement(player, "r3ct:quests/perfect_day");
            data.perfectDaysCount++;

            if (data.perfectDaysCount >= ConfigLoader.mechanics.streaks.perfectDaysForShield) {
                data.perfectDaysCount = 0;

                if (data.availableFreezes < ConfigLoader.mechanics.streaks.maxStoredShields) {
                    data.availableFreezes++;
                    player.sendSystemMessage(Component.translatable("r3ct.message.quests.shield_earned"));
                } else {
                    player.sendSystemMessage(Component.translatable("r3ct.message.quests.shield_limit_reached"));
                }

                if (data.availableFreezes >= 3) {
                    QuestManager.grantAdvancement(player, "r3ct:quests/hamster");
                }
            } else {
                player.sendSystemMessage(Component.translatable("r3ct.message.quests.perfect_day", "§b" + data.perfectDaysCount));
            }
        }
        QuestManager.grantAdvancement(player, "r3ct:quests/first_quest");

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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

        if (threshold == 50) {
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 32));
            player.sendSystemMessage(Component.translatable("r3ct.message.points.claim_50"));
        } else if (threshold == 100) {
            giveOrDrop(player, new ItemStack(Items.EMERALD, 16));
            player.sendSystemMessage(Component.translatable("r3ct.message.points.claim_100"));
        } else if (threshold == 150) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 8));
            player.sendSystemMessage(Component.translatable("r3ct.message.points.claim_150"));
        } else if (threshold == 200) {
            QuestManager.grantAdvancement(player, "r3ct:quests/points_hunter");
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 4));
            player.sendSystemMessage(Component.translatable("r3ct.message.points.claim_200"));

            data.totalQuestPoints -= 200;
            data.claimedPointRewards.clear();
            player.sendSystemMessage(Component.translatable("r3ct.message.points.reset"));
        }

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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
        String multiStr = (multi > 1) ? Component.translatable("r3ct.message.quests.streak_bonus").getString() : "";
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.daily_reward", multiStr));

        int xp = ConfigLoader.mechanics.quests.xpDailyReward * multi;
        player.giveExperiencePoints(xp);
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.xp_gained", "§e" + xp));

        if (ConfigLoader.dailyQuestRewards.isEmpty()) return;

        Random rand = new Random();
        int totalWeight = 0;
        for (ConfigLoader.RewardEntry entry : ConfigLoader.dailyQuestRewards) totalWeight += entry.weight;

        ConfigLoader.RewardEntry selectedEntry = ConfigLoader.dailyQuestRewards.get(0);
        if (totalWeight > 0) {
            int roll = rand.nextInt(totalWeight);
            int cursor = 0;
            for (ConfigLoader.RewardEntry entry : ConfigLoader.dailyQuestRewards) {
                cursor += entry.weight;
                if (roll < cursor) { selectedEntry = entry; break; }
            }
        }

        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse(selectedEntry.item)).orElse(Items.COAL);
        int baseAmount = selectedEntry.minAmount + rand.nextInt(Math.max(1, selectedEntry.maxAmount - selectedEntry.minAmount + 1));

        int finalAmount = baseAmount * multi;
        ItemStack reward = new ItemStack(item, finalAmount);
        String itemName = reward.getHoverName().getString();

        if (item == Items.DIAMOND) {
            QuestManager.grantAdvancement(player, "r3ct:quests/lucky_drop");
        }
        if (data.questStreak == 7) {
            QuestManager.grantAdvancement(player, "r3ct:quests/week_streak");
        }

        QuestManager.giveOrDrop(player, reward);
        player.sendSystemMessage(Component.translatable("r3ct.message.quests.item_gained", "§f" + finalAmount, "§b" + itemName));
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

        if (!ConfigLoader.mechanics.quests.enableQuestRerolling) {
            player.sendSystemMessage(Component.translatable("r3ct.message.reroll.disabled"));
            return;
        }

        if (index < 0 || index >= data.activeQuests.size()) return;
        if (data.questRewardsClaimed.get(index) || data.questProgress.get(index) >= getQuestById(data.activeQuests.get(index)).requiredAmount) {
            player.sendSystemMessage(Component.translatable("r3ct.message.reroll.already_completed"));
            return;
        }

        String oldQuestId = data.activeQuests.get(index);
        Quest oldQuest = getQuestById(oldQuestId);
        if (oldQuest == null) return;

        int cost = (oldQuest.difficulty == 0) ? ConfigLoader.mechanics.quests.rerollCostEasy :
                (oldQuest.difficulty == 1) ? ConfigLoader.mechanics.quests.rerollCostMedium :
                        ConfigLoader.mechanics.quests.rerollCostHard;

        if (data.totalQuestPoints < cost) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value()),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    1.0F, 1.0F, player.getRandom().nextLong()
            ));
            player.sendSystemMessage(Component.translatable("r3ct.message.reroll.not_enough_points", "§c" + cost, "§d" + data.totalQuestPoints));
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
            player.sendSystemMessage(Component.translatable("r3ct.message.reroll.no_quests_available"));
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

        player.sendSystemMessage(Component.translatable("r3ct.message.reroll.success", "§c" + cost, "§d" + data.totalQuestPoints));

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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
                com.r3ct.quests.platform.Services.PLATFORM.sendToPlayer(player, new SyncQuestsPayload(
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
}