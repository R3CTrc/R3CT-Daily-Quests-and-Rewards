package com.r3ct.daily.logic;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.network.OpenRewardsPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RewardManager {
    private static final Random RANDOM = new Random();

    public static Component getPrefix() {
        return Component.literal("[Daily] ").withStyle(ChatFormatting.AQUA);
    }

    public static List<ItemStack> getTier1Rewards(MinecraftServer server) {
        return processBuckets(DailyServerConfig.rewardsTier1, server);
    }

    public static List<ItemStack> getTier2Rewards(MinecraftServer server) {
        return processBuckets(DailyServerConfig.rewardsTier2, server);
    }

    public static List<ItemStack> getTier3Rewards(MinecraftServer server) {
        return processBuckets(DailyServerConfig.rewardsTier3, server);
    }

    public static ItemStack getStreakBonusReward(int rewardDay, MinecraftServer server) {
        List<DailyServerConfig.RewardEntry> pool;

        if (rewardDay <= 4) {
            pool = DailyServerConfig.streakRewardsTier1;
        } else if (rewardDay <= 6) {
            pool = DailyServerConfig.streakRewardsTier2;
        } else {
            pool = DailyServerConfig.streakRewardsTier3;
        }

        if (pool == null || pool.isEmpty()) return ItemStack.EMPTY;

        DailyServerConfig.RewardEntry entry = getRandomEntry(pool);
        return createSpecialOrStandardItem(entry, server);
    }

    private static List<ItemStack> processBuckets(List<List<DailyServerConfig.RewardEntry>> tiers, MinecraftServer server) {
        List<ItemStack> finalRewards = new ArrayList<>();

        for (List<DailyServerConfig.RewardEntry> bucket : tiers) {
            if (bucket.isEmpty()) continue;

            DailyServerConfig.RewardEntry entry = getRandomEntry(bucket);
            ItemStack reward = createSpecialOrStandardItem(entry, server);

            if (!reward.isEmpty()) {
                finalRewards.add(reward);
            }
        }
        return finalRewards;
    }

    private static ItemStack createSpecialOrStandardItem(DailyServerConfig.RewardEntry entry, MinecraftServer server) {
        int amount = entry.minAmount + RANDOM.nextInt(Math.max(1, entry.maxAmount - entry.minAmount + 1));
        return getCustomReward(entry.item, amount, server);
    }

    public static ItemStack getCustomReward(String rewardId, int amount, MinecraftServer server) {
        String rewardAction = rewardId.contains(":") ? rewardId.substring(rewardId.indexOf(":") + 1) : rewardId;

        switch (rewardAction) {
            case "random_potion":
                List<Potion> potionList = BuiltInRegistries.POTION.stream().filter(p -> {
                    String path = BuiltInRegistries.POTION.getKey(p).getPath();
                    return !path.equals("empty") && !path.equals("water") && !path.equals("mundane")
                            && !path.equals("thick") && !path.equals("awkward");
                }).toList();

                if (!potionList.isEmpty()) {
                    Potion randomPotion = potionList.get(RANDOM.nextInt(potionList.size()));
                    ItemStack stack = new ItemStack(Items.POTION, amount);
                    PotionUtils.setPotion(stack, randomPotion);
                    return stack;
                }
                return new ItemStack(Items.POTION, amount);

            case "random_enchanted_book":
                List<Enchantment> enchList = BuiltInRegistries.ENCHANTMENT.stream().filter(e -> !e.isCurse()).toList();
                ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                if (!enchList.isEmpty()) {
                    Enchantment randomEnch = enchList.get(RANDOM.nextInt(enchList.size()));
                    EnchantedBookItem.addEnchantment(enchantedBook, new EnchantmentInstance(randomEnch, 1));
                }
                return enchantedBook;

            case "random_disc":
                return getRandomItemWithKeyword("music_disc", amount);
            case "random_spawn_egg":
                return getRandomSpawnEgg(amount);
            case "random_carpet":
                return getRandomItemWithKeyword("_carpet", amount);
            case "random_head":
                return getRandomVanillaHead(amount);
            case "random_trim_template":
                return getRandomItemWithKeyword("smithing_template", amount);
            case "random_pottery_sherd":
                return getRandomItemWithKeyword("pottery_sherd", amount);
            case "random_dye":
                return getRandomItemWithKeyword("_dye", amount);
            case "random_wool":
                return getRandomItemWithKeyword("_wool", amount);
            case "random_sapling":
                return getRandomItemWithKeyword("_sapling", amount);
            case "random_coral_block":
                return getRandomCoralBlock(amount);
            case "random_job_block":
                Item[] blocks = {
                        Items.BARREL, Items.BLAST_FURNACE, Items.BREWING_STAND,
                        Items.CARTOGRAPHY_TABLE, Items.CAULDRON, Items.COMPOSTER,
                        Items.FLETCHING_TABLE, Items.GRINDSTONE, Items.LECTERN,
                        Items.LOOM, Items.SMITHING_TABLE, Items.SMOKER, Items.STONECUTTER
                };
                return new ItemStack(blocks[RANDOM.nextInt(blocks.length)], amount);

            case "unbreaking_2_book":
                ItemStack unbreakingBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                EnchantedBookItem.addEnchantment(unbreakingBook, new EnchantmentInstance(Enchantments.UNBREAKING, 2));
                return unbreakingBook;

            case "efficiency_3_book":
                ItemStack effBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                EnchantedBookItem.addEnchantment(effBook, new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 3));
                return effBook;

            case "infinity_book":
                ItemStack infBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                EnchantedBookItem.addEnchantment(infBook, new EnchantmentInstance(Enchantments.INFINITY_ARROWS, 1));
                return infBook;

            case "feather_falling_3_book":
                ItemStack featherBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                EnchantedBookItem.addEnchantment(featherBook, new EnchantmentInstance(Enchantments.FALL_PROTECTION, 3));
                return featherBook;

            case "sharpness_2_book":
                ItemStack sharpBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                EnchantedBookItem.addEnchantment(sharpBook, new EnchantmentInstance(Enchantments.SHARPNESS, 2));
                return sharpBook;

            case "firework_tier_3":
                ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, amount);
                CompoundTag tag = rockets.getOrCreateTagElement("Fireworks");
                tag.putByte("Flight", (byte)3);
                return rockets;

            case "healing_2_potion":
                ItemStack healPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(healPot, Potions.STRONG_HEALING);
                return healPot;

            case "water_breathing_potion":
                ItemStack waterPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(waterPot, Potions.WATER_BREATHING);
                return waterPot;

            case "fire_resistance_potion":
                ItemStack firePot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(firePot, Potions.FIRE_RESISTANCE);
                return firePot;

            case "slow_falling_potion":
                ItemStack slowPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(slowPot, Potions.SLOW_FALLING);
                return slowPot;

            case "night_vision_potion":
                ItemStack nightPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(nightPot, Potions.NIGHT_VISION);
                return nightPot;

            case "regeneration_potion":
                ItemStack regenPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(regenPot, Potions.REGENERATION);
                return regenPot;

            case "speed_potion":
                ItemStack speedPot = new ItemStack(Items.POTION, amount);
                PotionUtils.setPotion(speedPot, Potions.SWIFTNESS);
                return speedPot;

            default:
                var item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(rewardId)).orElse(Items.AIR);
                return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, amount);
        }
    }

    private static DailyServerConfig.RewardEntry getRandomEntry(List<DailyServerConfig.RewardEntry> bucket) {
        int totalWeight = 0;
        for (DailyServerConfig.RewardEntry entry : bucket) totalWeight += entry.chance;
        if (totalWeight <= 0) return bucket.get(RANDOM.nextInt(bucket.size()));

        int roll = RANDOM.nextInt(totalWeight);
        int cursor = 0;
        for (DailyServerConfig.RewardEntry entry : bucket) {
            cursor += entry.chance;
            if (roll < cursor) return entry;
        }
        return bucket.get(0);
    }

    private static ItemStack getRandomItemWithKeyword(String keyword, int amount) {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(i -> BuiltInRegistries.ITEM.getKey(i).getPath().contains(keyword))
                .toList();
        return new ItemStack(items.isEmpty() ? Items.PAPER : items.get(RANDOM.nextInt(items.size())), amount);
    }

    private static ItemStack getRandomCoralBlock(int amount) {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(i -> {
                    String path = BuiltInRegistries.ITEM.getKey(i).getPath();
                    return path.endsWith("coral_block") && !path.contains("dead");
                })
                .toList();
        return new ItemStack(items.isEmpty() ? Items.PAPER : items.get(RANDOM.nextInt(items.size())), amount);
    }

    private static ItemStack getRandomVanillaHead(int amount) {
        Item[] heads = {
                Items.ZOMBIE_HEAD,
                Items.SKELETON_SKULL,
                Items.CREEPER_HEAD,
                Items.PIGLIN_HEAD,
                Items.WITHER_SKELETON_SKULL
        };
        return new ItemStack(heads[RANDOM.nextInt(heads.length)], amount);
    }

    private static ItemStack getRandomSpawnEgg(int amount) {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(i -> {
                    String path = BuiltInRegistries.ITEM.getKey(i).getPath();
                    return path.endsWith("spawn_egg") && !path.contains("ender_dragon") && !path.contains("wither") && !path.contains("warden");
                })
                .toList();
        return new ItemStack(items.isEmpty() ? Items.PAPER : items.get(RANDOM.nextInt(items.size())), amount);
    }

    public static void claimBonusReward(ServerPlayer player, int bonusDay) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (data.totalCollected == 0) return;

        int cycle = (data.totalCollected - 1) / 21;
        int absoluteTarget = cycle * 21 + bonusDay;

        if (data.totalCollected < absoluteTarget) return;
        if (data.claimedBonusRewards.contains(absoluteTarget)) return;

        data.claimedBonusRewards.add(absoluteTarget);

        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP),
                SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));

        DailyServerConfig.MilestoneReward mr = null;
        if (bonusDay == 7) {
            QuestManager.grantAdvancement(player, "r3ct_daily:rewards/week_bonus");
            mr = DailyServerConfig.bonuses.bonus_7;
        } else if (bonusDay == 14) {
            mr = DailyServerConfig.bonuses.bonus_14;
        } else if (bonusDay == 21) {
            QuestManager.grantAdvancement(player, "r3ct_daily:rewards/cycle_end");
            mr = DailyServerConfig.bonuses.bonus_21;
        }

        if (mr != null) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr);
            String colorStr = mr.getFormattedColor();
            ChatFormatting format = ChatFormatting.getByCode(colorStr.charAt(colorStr.length() - 1));
            if (format == null) format = ChatFormatting.WHITE;
            Component bonusDayComp = Component.literal(String.valueOf(bonusDay)).withStyle(ChatFormatting.LIGHT_PURPLE);

            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.bonus.claimed_base", bonusDayComp).withStyle(ChatFormatting.GREEN)
            ));

            Component amountComp = Component.literal(String.valueOf(mr.amount)).withStyle(ChatFormatting.AQUA);
            Component itemNameComp = stack.getHoverName().copy().withStyle(format);

            QuestManager.giveOrDrop(player, stack);

            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct_daily.message.quests.item_gained", amountComp, itemNameComp).withStyle(ChatFormatting.GREEN)
            ));
        }

        ModState.get(server).setDirty();

        int visualStreak = data.streak;
        LocalDate today = LocalDateTime.now().minusHours(DailyServerConfig.mechanics.technical.questRefreshHour).toLocalDate();
        if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
            if (!data.lastStreakDate.equals(today.toString()) && !data.lastStreakDate.equals(today.minusDays(1).toString())) {
                visualStreak = 0;
            }
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new OpenRewardsPayload(
                data.rewardDay,
                data.lastRewardDate,
                visualStreak,
                data.totalCollected,
                data.claimedRewardHistory,
                data.availableRewardFreezes,
                data.claimedBonusRewards,
                DailyServerConfig.mechanics.streaks.maxStoredRewardShields,
                DailyServerConfig.mechanics.technical.questRefreshHour
        ).write(buf);

        Services.PLATFORM.sendToPlayer(player, OpenRewardsPayload.ID, buf);
    }
}