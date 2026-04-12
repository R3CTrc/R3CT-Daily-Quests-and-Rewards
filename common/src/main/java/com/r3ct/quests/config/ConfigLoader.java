package com.r3ct.quests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.r3ct.quests.Constants;
import com.r3ct.quests.logic.Quest;
import com.r3ct.quests.logic.QuestManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = com.r3ct.quests.platform.Services.PLATFORM.getConfigDir().resolve("r3ct_quests");

    private static final File QUESTS_FILE = CONFIG_DIR.resolve("quests.json").toFile();
    private static final File REWARDS_FILE = CONFIG_DIR.resolve("daily_rewards.json").toFile();
    private static final File DAILY_QUEST_REWARDS_FILE = CONFIG_DIR.resolve("daily_quest_rewards.json").toFile();

    private static final File MECHANICS_FILE = CONFIG_DIR.resolve("r3ctdailyquests.json").toFile();

    public static class RewardEntry {
        public String item;
        public int minAmount;
        public int maxAmount;
        public int weight;
        public RewardEntry(String i, int min, int max, int w) { this.item = i; this.minAmount = min; this.maxAmount = max; this.weight = w; }
    }

    public static class QuestsSettings {
        public boolean enableQuestRerolling = true;
        public int rerollCostEasy = 1;
        public int rerollCostMedium = 2;
        public int rerollCostHard = 4;
        public int xpPerQuestEasy = 30;
        public int xpPerQuestMedium = 60;
        public int xpPerQuestHard = 90;
        public int xpDailyReward = 90;
    }

    public static class StreaksSettings {
        public int perfectDaysForShield = 3;
        public int maxStoredQuestShields = 3;
        public int maxStoredRewardShields = 3;
    }

    public static class TechnicalSettings {
        public int placedBlocksCacheLimit = 10000;
        public int leaderboardUpdateIntervalTicks = 1200;
        public int questRefreshHour = 0;
    }

    public static class MechanicsConfig {
        public QuestsSettings quests = new QuestsSettings();
        public StreaksSettings streaks = new StreaksSettings();
        public TechnicalSettings technical = new TechnicalSettings();
    }

    public static MechanicsConfig mechanics = new MechanicsConfig();

    public static List<List<RewardEntry>> rewardsTier1 = new ArrayList<>();
    public static List<List<RewardEntry>> rewardsTier2 = new ArrayList<>();
    public static List<List<RewardEntry>> rewardsTier3 = new ArrayList<>();
    public static List<RewardEntry> dailyQuestRewards = new ArrayList<>();

    public static void loadAll() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            copyDefaultConfig("quests.json");
            copyDefaultConfig("daily_rewards.json");
            copyDefaultConfig("daily_quest_rewards.json");

            copyDefaultConfig("r3ctdailyquests.json");

            loadQuests();
            loadRewards();
            loadDailyQuestRewards();

            loadMechanics();

        } catch (Exception e) {
            Constants.LOG.error("Error initializing ConfigLoader!", e);
        }
    }

    private static void copyDefaultConfig(String fileName) {
        Path target = CONFIG_DIR.resolve(fileName);
        if (!Files.exists(target)) {
            try (InputStream is = ConfigLoader.class.getResourceAsStream("/assets/r3ct/configs" + fileName)) {
                if (is != null) {
                    Files.copy(is, target);
                }
            } catch (IOException e) {
                Constants.LOG.error("Error copying file: " + fileName, e);
            }
        }
    }

    private static void loadMechanics() {
        if (!MECHANICS_FILE.exists()) return;

        try (FileReader reader = new FileReader(MECHANICS_FILE)) {
            mechanics = GSON.fromJson(reader, MechanicsConfig.class);
            if (mechanics == null) mechanics = new MechanicsConfig();
        } catch (Exception e) {
            Constants.LOG.error("Error loading r3ctdailyquests.json!", e);
        }
    }

    private static void loadRewards() {
        rewardsTier1.clear(); rewardsTier2.clear(); rewardsTier3.clear();
        if (!REWARDS_FILE.exists()) return;

        try (FileReader reader = new FileReader(REWARDS_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            parseRewards(root.getAsJsonArray("days_1_to_4"), rewardsTier1);
            parseRewards(root.getAsJsonArray("days_5_to_6"), rewardsTier2);
            parseRewards(root.getAsJsonArray("day_7"), rewardsTier3);
        } catch (Exception e) {
            Constants.LOG.error("Error loading daily_rewards.json!", e);
        }
    }

    private static void loadDailyQuestRewards() {
        dailyQuestRewards.clear();
        if (!DAILY_QUEST_REWARDS_FILE.exists()) return;

        try (FileReader reader = new FileReader(DAILY_QUEST_REWARDS_FILE)) {
            JsonArray root = GSON.fromJson(reader, JsonArray.class);
            parseSimpleRewards(root, dailyQuestRewards);
        } catch (Exception e) {
            Constants.LOG.error("Error loading daily_quest_rewards.json!", e);
        }
    }

    private static void loadQuests() {
        QuestManager.EASY_QUESTS.clear();
        QuestManager.MEDIUM_QUESTS.clear();
        QuestManager.HARD_QUESTS.clear();

        if (!QUESTS_FILE.exists()) return;

        try (FileReader reader = new FileReader(QUESTS_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            parseQuestArray(root.getAsJsonArray("overworld_quests"), "minecraft:overworld");
            parseQuestArray(root.getAsJsonArray("nether_quests"), "minecraft:the_nether");
            parseQuestArray(root.getAsJsonArray("end_quests"), "minecraft:the_end");
        } catch (Exception e) {
            Constants.LOG.error("Error loading quests.json!", e);
        }
    }

    private static void parseRewards(JsonArray array, List<List<RewardEntry>> tierList) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            JsonArray bucketArray = array.get(i).getAsJsonArray();
            List<RewardEntry> bucket = new ArrayList<>();
            for (int j = 0; j < bucketArray.size(); j++) {
                JsonObject obj = bucketArray.get(j).getAsJsonObject();
                bucket.add(new RewardEntry(obj.get("item").getAsString(), obj.get("min_amount").getAsInt(), obj.get("max_amount").getAsInt(), obj.get("weight").getAsInt()));
            }
            tierList.add(bucket);
        }
    }

    private static void parseSimpleRewards(JsonArray array, List<RewardEntry> list) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            list.add(new RewardEntry(obj.get("item").getAsString(), obj.get("min_amount").getAsInt(), obj.get("max_amount").getAsInt(), obj.get("weight").getAsInt()));
        }
    }

    private static void parseQuestArray(com.google.gson.JsonArray array, String dimension) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            com.google.gson.JsonObject obj = array.get(i).getAsJsonObject();
            int diffInt = obj.get("difficulty").getAsInt();

            String itemStr = obj.get("reward_item").getAsString();
            int rewardAmount = obj.get("reward_amount").getAsInt();

            Quest q = new Quest(
                    obj.get("id").getAsString(),
                    obj.has("name") ? obj.get("name").getAsString() : "Quest",
                    obj.get("description").getAsString(),
                    obj.get("amount").getAsInt(),
                    diffInt,
                    obj.get("points").getAsInt(),
                    rewardAmount,
                    dimension,
                    obj.get("action_type").getAsString(),
                    obj.has("target") ? obj.get("target").getAsString() : "any",
                    itemStr
            );

            if (diffInt == 0) QuestManager.EASY_QUESTS.add(q);
            else if (diffInt == 1) QuestManager.MEDIUM_QUESTS.add(q);
            else QuestManager.HARD_QUESTS.add(q);
        }
    }
}