package com.r3ct.daily.logic;

import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.network.OpenRewardsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RewardManager {
    private static final Random RANDOM = new Random();

    public static Component getPrefix() {
        return Component.literal("[Daily] ").withStyle(net.minecraft.ChatFormatting.AQUA);
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

        switch (entry.item) {
            case "r3ct_daily:random_potion":
                var potionLookup = server.registryAccess().lookup(Registries.POTION).orElse(null);
                if (potionLookup != null) {
                    var potionList = potionLookup.listElements().toList();
                    if (!potionList.isEmpty()) {
                        var randomPotion = potionList.get(RANDOM.nextInt(potionList.size()));
                        ItemStack stack = PotionContents.createItemStack(Items.POTION, randomPotion);
                        stack.setCount(amount);
                        return stack;
                    }
                }
                return new ItemStack(Items.POTION, amount);

            case "r3ct_daily:random_enchanted_book":
                var enchLookup = server.registryAccess().lookup(Registries.ENCHANTMENT).orElse(null);
                ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, amount);
                if (enchLookup != null) {
                    var enchList = enchLookup.listElements().filter(ref -> !ref.is(net.minecraft.tags.EnchantmentTags.CURSE)).toList();
                    if (!enchList.isEmpty()) {
                        var randomEnch = enchList.get(RANDOM.nextInt(enchList.size()));
                        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(enchantedBook, builder -> {
                            builder.set(randomEnch, 1);
                        });
                    }
                }
                return enchantedBook;

            case "r3ct_daily:random_disc":
                return getRandomItemWithKeyword("music_disc", amount);

            case "r3ct_daily:random_spawn_egg":
                return getRandomItemWithKeyword("spawn_egg", amount);

            case "r3ct_daily:random_carpet":
                return getRandomItemWithKeyword("carpet", amount);

            case "r3ct_daily:random_head":
                return getRandomVanillaHead(amount);

            case "r3ct_daily:random_trim_template":
                return getRandomItemWithKeyword("smithing_template", amount);

            case "r3ct_daily:random_pottery_sherd":
                return getRandomItemWithKeyword("pottery_sherd", amount);

            case "r3ct_daily:random_dye":
                return getRandomItemWithKeyword("_dye", amount);

            case "r3ct_daily:random_wool":
                return getRandomItemWithKeyword("_wool", amount);

            case "r3ct_daily:random_sapling":
                return getRandomItemWithKeyword("_sapling", amount);

            case "r3ct_daily:random_coral_block":
                return getRandomItemWithKeyword("coral_block", amount);

            default:
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(entry.item)).orElse(Items.AIR);
                return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, amount);
        }
    }

    private static DailyServerConfig.RewardEntry getRandomEntry(List<DailyServerConfig.RewardEntry> bucket) {
        int totalWeight = 0;
        for (DailyServerConfig.RewardEntry entry : bucket) totalWeight += entry.weight;
        if (totalWeight <= 0) return bucket.get(RANDOM.nextInt(bucket.size()));

        int roll = RANDOM.nextInt(totalWeight);
        int cursor = 0;
        for (DailyServerConfig.RewardEntry entry : bucket) {
            cursor += entry.weight;
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

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP),
                net.minecraft.sounds.SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, player.getRandom().nextLong()
        ));

        DailyServerConfig.MilestoneReward mr = null;
        if (bonusDay == 7) {
            QuestManager.grantAdvancement(player, "r3ct_daily:rewards/week_bonus");
            mr = DailyServerConfig.mechanics.milestones.bonus_7;
        } else if (bonusDay == 14) {
            mr = DailyServerConfig.mechanics.milestones.bonus_14;
        } else if (bonusDay == 21) {
            QuestManager.grantAdvancement(player, "r3ct_daily:rewards/cycle_end");
            mr = DailyServerConfig.mechanics.milestones.bonus_21;
        }

        if (mr != null) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr);
            String translationKey = stack.getItem().getDescriptionId();
            QuestManager.giveOrDrop(player, stack);
            String colorStr = mr.getFormattedColor();
            net.minecraft.ChatFormatting format = net.minecraft.ChatFormatting.getByCode(colorStr.charAt(colorStr.length() - 1));
            if (format == null) format = net.minecraft.ChatFormatting.WHITE;
            Component bonusDayComp = Component.literal(String.valueOf(bonusDay)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            net.minecraft.network.chat.MutableComponent bonusRewardText = Component.literal(mr.amount + "x ")
                    .append(Component.translatable(translationKey))
                    .withStyle(format);
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(
                    Component.translatable("r3ct.message.bonus.claim", bonusDayComp, bonusRewardText).withStyle(net.minecraft.ChatFormatting.GREEN)
            ));
        }

        server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();

        int visualStreak = data.streak;
        LocalDate today = java.time.LocalDateTime.now().minusHours(DailyServerConfig.mechanics.technical.questRefreshHour).toLocalDate();
        if (data.lastStreakDate != null && !data.lastStreakDate.isEmpty()) {
            if (!data.lastStreakDate.equals(today.toString()) && !data.lastStreakDate.equals(today.minusDays(1).toString())) {
                visualStreak = 0;
            }
        }

        Services.PLATFORM.sendToPlayer(player, new OpenRewardsPayload(
                data.rewardDay,
                data.lastRewardDate,
                visualStreak,
                data.totalCollected,
                data.claimedRewardHistory,
                data.availableRewardFreezes,
                data.claimedBonusRewards,
                DailyServerConfig.mechanics.streaks.maxStoredRewardShields,
                DailyServerConfig.mechanics.technical.questRefreshHour
        ));
    }
}