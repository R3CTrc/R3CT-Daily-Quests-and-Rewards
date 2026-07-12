package com.r3ct.daily.config;

import com.google.gson.*;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.Constants;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DailyServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("r3ct_daily");

    private static final File QUESTS_FILE = CONFIG_DIR.resolve("r3ct_daily_quests.json").toFile();
    private static final File REWARDS_FILE = CONFIG_DIR.resolve("r3ct_daily_rewards.json").toFile();
    private static final File MECHANICS_FILE = CONFIG_DIR.resolve("r3ct_daily_server.json").toFile();

    private static final int QUESTS_VERSION = 2;
    private static final int REWARDS_VERSION = 2;
    private static final int MECHANICS_VERSION = 2;

    public static class RewardEntry {
        public String item;
        public int minAmount;
        public int maxAmount;
        public int chance;
        public String color;

        public RewardEntry(String i, int min, int max, int w, String c) {
            this.item = i != null ? i.toLowerCase(Locale.ROOT) : "minecraft:paper";
            this.minAmount = min;
            this.maxAmount = max;
            this.chance = w;
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
        public int xpPerQuestEasy = 25;
        public int xpPerQuestMedium = 50;
        public int xpPerQuestHard = 75;
        public int xpDailyReward = 75;
        public float questStreakXpMultiplier = 1.5f;
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
        public int actionSyncInterval = 1;
        public int distanceSyncInterval = 5;
        public int elytraSyncInterval = 50;
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

        public MilestoneReward(String i, int a, String c) {
            this.item = i != null ? i.toLowerCase(Locale.ROOT) : "minecraft:paper";
            this.amount = a;
            this.color = c;
        }

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
    public static List<RewardEntry> streakRewardsTier1 = new ArrayList<>();
    public static List<RewardEntry> streakRewardsTier2 = new ArrayList<>();
    public static List<RewardEntry> streakRewardsTier3 = new ArrayList<>();
    public static List<RewardEntry> dailyQuestRewards = new ArrayList<>();

    private static void checkAndMigrate(File file, String resourceName, int expectedVersion) {
        if (!file.exists()) {
            copyDefaultConfig(resourceName);
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
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
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
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
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
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
        streakRewardsTier1.clear(); streakRewardsTier2.clear(); streakRewardsTier3.clear();

        if (!REWARDS_FILE.exists()) return;

        try (FileReader reader = new FileReader(REWARDS_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            parseRewards(root.getAsJsonArray("days_1_to_4"), rewardsTier1);
            parseRewards(root.getAsJsonArray("days_5_to_6"), rewardsTier2);
            parseRewards(root.getAsJsonArray("day_7"), rewardsTier3);

            if (root.has("streak_days_1_to_4")) {
                parseSimpleRewards(root.getAsJsonArray("streak_days_1_to_4"), streakRewardsTier1);
            }
            if (root.has("streak_days_5_to_6")) {
                parseSimpleRewards(root.getAsJsonArray("streak_days_5_to_6"), streakRewardsTier2);
            }
            if (root.has("streak_day_7")) {
                parseSimpleRewards(root.getAsJsonArray("streak_day_7"), streakRewardsTier3);
            }

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
        QuestManager.QUEST_MAP.clear();
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

    private static void parseRewards(JsonArray array, List<List<RewardEntry>> tierList) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            JsonArray bucketArray = array.get(i).getAsJsonArray();
            List<RewardEntry> bucket = new ArrayList<>();
            for (int j = 0; j < bucketArray.size(); j++) {
                try {
                    JsonObject obj = bucketArray.get(j).getAsJsonObject();
                    bucket.add(new RewardEntry(
                            getString(obj, "item"),
                            getInt(obj, "min_amount"),
                            getInt(obj, "max_amount"),
                            getInt(obj, "chance"),
                            obj.has("color") ? obj.get("color").getAsString() : "&b"
                    ));
                } catch (Exception e) {
                    Constants.LOG.error("Error loading reward entry in list (group: " + i + ", item: " + j + "). Skipping entry. Reason: " + e.getMessage());
                }
            }
            tierList.add(bucket);
        }
    }

    private static void parseSimpleRewards(JsonArray array, List<RewardEntry> list) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            try {
                JsonObject obj = array.get(i).getAsJsonObject();
                list.add(new RewardEntry(
                        getString(obj, "item"),
                        getInt(obj, "min_amount"),
                        getInt(obj, "max_amount"),
                        getInt(obj, "chance"),
                        obj.has("color") ? obj.get("color").getAsString() : "&b"
                ));
            } catch (Exception e) {
                Constants.LOG.error("Error loading simple reward entry (index: " + i + "). Skipping entry. Reason: " + e.getMessage());
            }
        }
    }

    private static void parseQuestArray(JsonArray array, String dimension) {
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            try {
                JsonObject obj = array.get(i).getAsJsonObject();

                int diffInt = getInt(obj, "difficulty");

                Quest q = new Quest(
                        getString(obj, "id"),
                        getString(obj, "name"),
                        getString(obj, "description"),
                        getInt(obj, "amount"),
                        diffInt,
                        getInt(obj, "reward_amount"),
                        dimension,
                        getString(obj, "required_location"),
                        getString(obj, "action_type"),
                        getString(obj, "target"),
                        getString(obj, "reward_item")
                );

                if (diffInt == 0) QuestManager.EASY_QUESTS.add(q);
                else if (diffInt == 1) QuestManager.MEDIUM_QUESTS.add(q);
                else QuestManager.HARD_QUESTS.add(q);
                QuestManager.QUEST_MAP.put(q.id, q);

            } catch (Exception e) {
                Constants.LOG.error("Error loading quest (index: " + i + ") in dimension " + dimension + ". Skipping quest. Reason: " + e.getMessage());
            }
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing required string field: '" + key + "'");
        }
        return obj.get(key).getAsString();
    }

    private static int getInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing required integer field: '" + key + "'");
        }
        return obj.get(key).getAsInt();
    }

    public static String getConfigFileAsString(File file) {
        if (!file.exists()) return "{}";
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Constants.LOG.error("Failed to read config file: " + file.getName(), e);
            return "{}";
        }
    }

    public static String getQuestsConfigString() { return getConfigFileAsString(QUESTS_FILE); }
    public static String getRewardsConfigString() { return getConfigFileAsString(REWARDS_FILE); }
    public static String getServerConfigString() { return getConfigFileAsString(MECHANICS_FILE); }

    public static void syncFromServer(String questsJson, String rewardsJson, String serverJson) {
        try {
            JsonObject qRoot = JsonParser.parseString(questsJson).getAsJsonObject();
            QuestManager.QUEST_MAP.clear();
            QuestManager.EASY_QUESTS.clear();
            QuestManager.MEDIUM_QUESTS.clear();
            QuestManager.HARD_QUESTS.clear();

            if (qRoot.has("overworld_quests")) parseQuestArray(qRoot.getAsJsonArray("overworld_quests"), "minecraft:overworld");
            if (qRoot.has("nether_quests")) parseQuestArray(qRoot.getAsJsonArray("nether_quests"), "minecraft:the_nether");
            if (qRoot.has("end_quests")) parseQuestArray(qRoot.getAsJsonArray("end_quests"), "minecraft:the_end");

            JsonObject rRoot = JsonParser.parseString(rewardsJson).getAsJsonObject();
            rewardsTier1.clear(); rewardsTier2.clear(); rewardsTier3.clear(); dailyQuestRewards.clear();
            streakRewardsTier1.clear(); streakRewardsTier2.clear(); streakRewardsTier3.clear();

            if (rRoot.has("days_1_to_4")) parseRewards(rRoot.getAsJsonArray("days_1_to_4"), rewardsTier1);
            if (rRoot.has("days_5_to_6")) parseRewards(rRoot.getAsJsonArray("days_5_to_6"), rewardsTier2);
            if (rRoot.has("day_7")) parseRewards(rRoot.getAsJsonArray("day_7"), rewardsTier3);

            if (rRoot.has("streak_days_1_to_4")) parseSimpleRewards(rRoot.getAsJsonArray("streak_days_1_to_4"), streakRewardsTier1);
            if (rRoot.has("streak_days_5_to_6")) parseSimpleRewards(rRoot.getAsJsonArray("streak_days_5_to_6"), streakRewardsTier2);
            if (rRoot.has("streak_day_7")) parseSimpleRewards(rRoot.getAsJsonArray("streak_day_7"), streakRewardsTier3);

            if (rRoot.has("quest_completion")) parseSimpleRewards(rRoot.getAsJsonArray("quest_completion"), dailyQuestRewards);
            if (rRoot.has("milestones")) milestones = GSON.fromJson(rRoot.get("milestones"), MilestonesConfig.class);
            if (rRoot.has("bonuses")) bonuses = GSON.fromJson(rRoot.get("bonuses"), BonusesConfig.class);

            JsonObject mRoot = JsonParser.parseString(serverJson).getAsJsonObject();
            mechanics = GSON.fromJson(mRoot, MechanicsConfig.class);
            if (mechanics == null) mechanics = new MechanicsConfig();

            Constants.LOG.info("Successfully synced Daily Configs from Server RAM!");
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse synced config from server!", e);
        }
    }
}