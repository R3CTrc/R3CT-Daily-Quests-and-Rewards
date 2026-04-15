package com.r3ct.quests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class R3CTQuestsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = com.r3ct.quests.platform.Services.PLATFORM.getConfigDir().resolve("r3ct_quests/quests_client.json").toFile();

    public boolean enableHud = true;
    public int hudXOffset = 10;
    public int hudYOffset = 70;
    public float hudScale = 1.0f;
    public float questScreenScale = 1.0f;
    public float rewardScreenScale = 1.0f;
    public float leaderboardScreenScale = 1.0f;

    private static R3CTQuestsConfig instance = new R3CTQuestsConfig();

    public static R3CTQuestsConfig getInstance() {
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                R3CTQuestsConfig loaded = GSON.fromJson(reader, R3CTQuestsConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                com.r3ct.quests.Constants.LOG.error("Error loading quests_client.json!", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            com.r3ct.quests.Constants.LOG.error("Error saving quests_client.json!", e);
        }
    }
}