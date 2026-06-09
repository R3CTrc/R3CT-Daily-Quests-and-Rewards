package com.r3ct.daily.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.Constants;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DailyServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("r3ct_daily");

    private static final File QUESTS_FILE = CONFIG_DIR.resolve("r3ct_daily_quests.json").toFile();
    private static final File REWARDS_FILE = CONFIG_DIR.resolve("r3ct_daily_rewards.json").toFile();
    private static final File MECHANICS_FILE = CONFIG_DIR.resolve("r3ct_daily_server.json").toFile();

    private static final int QUESTS_VERSION = 2;
    private static final int REWARDS_VERSION = 2;
    private static final int MECHANICS_VERSION = 1;

    public static class RewardEntry {
        public String item;
        public int minAmount;
        public int maxAmount;
        public int weight;
        public String color;

        public RewardEntry(String i, int min, int max, int w, String c) {
            this.item = i;
            this.minAmount = min;
            this.maxAmount = max;
            this.weight = w;
            this.color = c;
        }

        public String getFormattedColor() {
            return this.color != null ? this.color.replace('&', '§') : "§b";
        }
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

    public static class MilestoneReward {
        public String item;
        public int amount;
        public String color;
        public MilestoneReward(String i, int a, String c) { this.item = i; this.amount = a; this.color = c; }
        public String getFormattedColor() {
            return this.color != null ? this.color.replace('&', '§') : "§b";
        }
    }

    public static class MilestonesConfig {
        public MilestoneReward point_50 = new MilestoneReward("minecraft:amethyst_shard", 32, "&d");
        public MilestoneReward point_100 = new MilestoneReward("minecraft:emerald", 16, "&a");
        public MilestoneReward point_150 = new MilestoneReward("minecraft:diamond", 8, "&b");
        public MilestoneReward point_200 = new MilestoneReward("minecraft:netherite_scrap", 4, "&c");
    }

    public static class BonusesConfig {
        public MilestoneReward bonus_7 = new MilestoneReward("minecraft:emerald", 32, "&a");
        public MilestoneReward bonus_14 = new MilestoneReward("minecraft:diamond", 16, "&b");
        public MilestoneReward bonus_21 = new MilestoneReward("minecraft:netherite_scrap", 4, "&c");
    }

    public static MechanicsConfig mechanics = new MechanicsConfig();
    public static MilestonesConfig milestones = new MilestonesConfig();
    public static BonusesConfig bonuses = new BonusesConfig();

    public static List<List<RewardEntry>> rewardsTier1 = new ArrayList<>();
    public static List<List<RewardEntry>> rewardsTier2 = new ArrayList<>();
    public static List<List<RewardEntry>> rewardsTier3 = new ArrayList<>();
    public static List<RewardEntry> dailyQuestRewards = new ArrayList<>();

    private static void checkAndMigrate(File file, String resourceName, int expectedVersion) {
        if (!file.exists()) {
            copyDefaultConfig(resourceName);
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(file)) {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                int version = json.has("version") ? json.get("version").getAsInt() : 0;
                if (version < expectedVersion) {
                    needsUpdate = true;
                }
            } else {
                needsUpdate = true;
            }
        } catch (Exception e) {
            needsUpdate = true;
        }

        if (needsUpdate) {
            try {
                Path path = file.toPath();
                String oldName = path.getFileName().toString().replace(".json", "_OLD.json");
                Path backupPath = path.resolveSibling(oldName);
                Files.move(path, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Constants.LOG.info("Outdated config detected! Backed up to: " + oldName);
                copyDefaultConfig(resourceName);
            } catch (Exception e) {
                Constants.LOG.error("Failed to migrate config: " + resourceName, e);
            }
        }
    }

    public static void loadAll() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            checkAndMigrate(QUESTS_FILE, "r3ct_daily_quests.json", QUESTS_VERSION);
            checkAndMigrate(REWARDS_FILE, "r3ct_daily_rewards.json", REWARDS_VERSION);
            checkAndMigrate(MECHANICS_FILE, "r3ct_daily_server.json", MECHANICS_VERSION);

            loadQuests();
            loadRewards();
            loadMechanics();

        } catch (Exception e) {
            Constants.LOG.error("Error initializing DailyServerConfig!", e);
        }
    }

    private static void copyDefaultConfig(String fileName) {
        Path target = CONFIG_DIR.resolve(fileName);
        try (InputStream is = DailyServerConfig.class.getResourceAsStream("/assets/r3ct_daily/configs/" + fileName)) {
            if (is != null) {
                Files.copy(is, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Constants.LOG.error("Error copying file: " + fileName, e);
        }
    }

    private static void loadMechanics() {
        if (!MECHANICS_FILE.exists()) return;
        try (FileReader reader = new FileReader(MECHANICS_FILE)) {
            mechanics = GSON.fromJson(reader, MechanicsConfig.class);
            if (mechanics == null) mechanics = new MechanicsConfig();
        } catch (Exception e) {
            Constants.LOG.error("Error loading r3ct_daily_server.json!", e);
        }
    }

    private static void loadRewards() {
        rewardsTier1.clear(); rewardsTier2.clear(); rewardsTier3.clear();
        dailyQuestRewards.clear();
        if (!REWARDS_FILE.exists()) return;

        try (FileReader reader = new FileReader(REWARDS_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            parseRewards(root.getAsJsonArray("days_1_to_4"), rewardsTier1);
            parseRewards(root.getAsJsonArray("days_5_to_6"), rewardsTier2);
            parseRewards(root.getAsJsonArray("day_7"), rewardsTier3);

            if (root.has("quest_completion")) {
                parseSimpleRewards(root.getAsJsonArray("quest_completion"), dailyQuestRewards);
            }

            if (root.has("milestones")) {
                milestones = GSON.fromJson(root.get("milestones"), MilestonesConfig.class);
            }

            if (root.has("bonuses")) {
                bonuses = GSON.fromJson(root.get("bonuses"), BonusesConfig.class);
            }

        } catch (Exception e) {
            Constants.LOG.error("Error loading r3ct_daily_rewards.json!", e);
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
            Constants.LOG.error("Error loading r3ct_daily_quests.json!", e);
        }
    }

    private static void parseRewards(com.google.gson.JsonArray array, List<List<RewardEntry>> tierList) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            com.google.gson.JsonArray bucketArray = array.get(i).getAsJsonArray();
            List<RewardEntry> bucket = new ArrayList<>();
            for (int j = 0; j < bucketArray.size(); j++) {
                com.google.gson.JsonObject obj = bucketArray.get(j).getAsJsonObject();
                bucket.add(new RewardEntry(
                        obj.get("item").getAsString(),
                        obj.get("min_amount").getAsInt(),
                        obj.get("max_amount").getAsInt(),
                        obj.get("weight").getAsInt(),
                        obj.has("color") ? obj.get("color").getAsString() : "&b"
                ));
            }
            tierList.add(bucket);
        }
    }

    private static void parseSimpleRewards(com.google.gson.JsonArray array, List<RewardEntry> list) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            com.google.gson.JsonObject obj = array.get(i).getAsJsonObject();
            list.add(new RewardEntry(
                    obj.get("item").getAsString(),
                    obj.get("min_amount").getAsInt(),
                    obj.get("max_amount").getAsInt(),
                    obj.get("weight").getAsInt(),
                    obj.has("color") ? obj.get("color").getAsString() : "&b"
            ));
        }
    }

    private static void parseQuestArray(com.google.gson.JsonArray array, String dimension) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            try {
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
            } catch (Exception e) {
                Constants.LOG.error("Error loading quest (index: " + i + ") in dimension " + dimension + ". Skipping quest.", e);
            }
        }
    }
}