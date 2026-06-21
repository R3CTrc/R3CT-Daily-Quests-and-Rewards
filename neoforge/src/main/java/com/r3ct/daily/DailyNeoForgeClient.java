package com.r3ct.daily;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.daily.client.screen.ConfigMainScreen;
import com.r3ct.daily.client.screen.LeaderboardScreen;
import com.r3ct.daily.client.screen.QuestScreen;
import com.r3ct.daily.client.screen.RewardScreen;
import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.network.LeaderboardResponsePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

public class DailyNeoForgeClient {

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (client, parent) -> new ConfigMainScreen(parent));
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        public static KeyMapping openRewardsKey;
        public static KeyMapping openQuestsKey;
        private static KeyMapping toggleHudKey;

        private static final KeyMapping.Category R3CT_CATEGORY = KeyMapping.Category.register(Identifier.parse(Constants.MOD_ID + ":main"));

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            QuestManager.init();
            DailyClientConfig.load();
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            openRewardsKey = new KeyMapping("key.r3ct_daily.open_rewards", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, R3CT_CATEGORY);
            openQuestsKey = new KeyMapping("key.r3ct_daily.open_quests", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, R3CT_CATEGORY);
            toggleHudKey = new KeyMapping("key.r3ct_daily.toggle_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, R3CT_CATEGORY);

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

            while (ClientModEvents.openRewardsKey.consumeClick()) client.player.connection.sendCommand("daily rewards");
            while (ClientModEvents.openQuestsKey.consumeClick()) client.player.connection.sendCommand("daily quests");

            if (ClientModEvents.toggleHudKey.consumeClick()) {
                minimizedHud = !minimizedHud;
                Component message = Component.translatable("r3ct_daily.message.hud_toggle", minimizedHud ? "§4OFF" : "§aON");
                client.gui.setOverlayMessage(message, false);
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui || client.getDebugOverlay().showDebugScreen() || client.player == null) return;
            if (client.screen != null && !(client.screen instanceof ChatScreen)) return;
            if (!DailyClientConfig.getInstance().enableHud) return;

            int screenWidth = event.getGuiGraphics().guiWidth();
            int rawXOffset = DailyClientConfig.getInstance().hudXOffset;
            int rawYOffset = DailyClientConfig.getInstance().hudYOffset;
            boolean isRight = DailyClientConfig.getInstance().hudAlignment.equals("right");

            double currentGuiScale = Math.max(1.0, client.getWindow().getGuiScale());
            float targetGuiScale = 2.0f;
            float configScale = DailyClientConfig.getInstance().hudScale;
            float scale = (float) (targetGuiScale / currentGuiScale) * configScale;

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
                    String loadingMsg = "§e" + Component.translatable("r3ct_daily.hud.loading").getString();
                    int xPos = isRight ? virtualWidth - client.font.width(loadingMsg) - xOffset : xOffset;
                    event.getGuiGraphics().text(client.font, loadingMsg, xPos, currentY, baseColor, true);
                }
                event.getGuiGraphics().pose().popMatrix();
                return;
            }

            if (!minimizedHud) {
                String title = "§e§l" + Component.translatable("r3ct_daily.quests.header.daily_quests").getString();
                int xPos = isRight ? virtualWidth - client.font.width(title) - xOffset : xOffset;
                event.getGuiGraphics().text(client.font, title, xPos, currentY, baseColor, true);
            }
            currentY += 12;

            for (int i = 0; i < clientQuestData.activeQuests.size(); i++) {
                Quest q = QuestManager.getQuestById(clientQuestData.activeQuests.get(i));
                if (q == null) continue;

                int progress = clientQuestData.questProgress.get(i);
                boolean done = progress >= q.requiredAmount;

                String mark = done ? "§a" + Component.translatable("r3ct_daily.quests.status.claimed").getString() : "§c" + Component.translatable("r3ct_daily.quests.status.incomplete").getString();

                if (minimizedHud) {
                    int xPos = isRight ? virtualWidth - client.font.width(mark) - xOffset : xOffset;
                    event.getGuiGraphics().text(client.font, mark, xPos, currentY, baseColor, true);
                } else {
                    String questName = (q.name != null && !q.name.isEmpty()) ? Component.translatable(q.name).getString() : Component.translatable(q.description).getString().split(" ")[0];
                    String diffIndicator = (q.difficulty == 0) ? "§2★ " : (q.difficulty == 1 ? "§6★ " : "§4★ ");
                    String progressColor = "§f";

                    if (System.currentTimeMillis() - flashTimestamps[i] < 600) {
                        progressColor = flashIsGreen[i] ? "§a" : "§c";
                    } else if (done) {
                        progressColor = "§7";
                    }

                    String lineText;
                    if (isRight) {
                        lineText = diffIndicator + progressColor + questName + " (" + progress + "/" + q.requiredAmount + ") " + mark;
                    } else {
                        lineText = mark + " " + diffIndicator + progressColor + questName + " (" + progress + "/" + q.requiredAmount + ")";
                    }

                    int xPos = isRight ? virtualWidth - client.font.width(lineText) - xOffset : xOffset;
                    event.getGuiGraphics().text(client.font, lineText, xPos, currentY, baseColor, true);
                }
                currentY += 10;
            }
            event.getGuiGraphics().pose().popMatrix();
        }
    }

    public static class ClientPayloadHandlers {
        public static void handleOpenRewards(com.r3ct.daily.network.OpenRewardsPayload payload) {
            PlayerData data = new PlayerData();
            data.rewardDay = payload.rewardDay();
            data.lastRewardDate = payload.lastRewardDate();
            data.streak = payload.streak();
            data.totalCollected = payload.totalCollected();
            data.claimedRewardHistory = payload.claimedRewardHistory();
            data.availableRewardFreezes = payload.availableRewardFreezes();
            data.claimedBonusRewards = payload.claimedBonusRewards();
            Minecraft.getInstance().setScreen(new RewardScreen(data, payload));
        }

        public static void handleOpenQuests(com.r3ct.daily.network.OpenQuestsPayload payload) {
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
            Minecraft.getInstance().setScreen(new QuestScreen(data, payload));
        }

        public static void handleSyncQuests(com.r3ct.daily.network.SyncQuestsPayload payload) {
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