package com.r3ct.daily;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.network.*;
import com.r3ct.daily.client.screen.LeaderboardScreen;
import com.r3ct.daily.client.screen.QuestScreen;
import com.r3ct.daily.client.screen.RewardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DailyFabricClient implements ClientModInitializer {
	public static KeyMapping openRewardsKey;
	public static KeyMapping openQuestsKey;
	private static KeyMapping toggleHudKey;

	private static final KeyMapping.Category R3CT_CATEGORY = KeyMapping.Category.register(Identifier.parse(DailyFabric.MOD_ID + ":main"));

	public static PlayerData clientQuestData = null;
	private static boolean minimizedHud = false;
	private static final long[] flashTimestamps = new long[10];
	private static final boolean[] flashIsGreen = new boolean[10];

	@Override
	public void onInitializeClient() {
		QuestManager.init();

		DailyClientConfig.load();

		openRewardsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.r3ct_daily.open_rewards",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				R3CT_CATEGORY
		));

		openQuestsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.r3ct_daily.open_quests",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				R3CT_CATEGORY
		));

		toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.r3ct_daily.toggle_hud",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_PERIOD,
				R3CT_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openRewardsKey.consumeClick()) {
				if (client.player != null) client.player.connection.sendCommand("daily rewards");
			}
			while (openQuestsKey.consumeClick()) {
				if (client.player != null) client.player.connection.sendCommand("daily quests");
			}
			while (toggleHudKey.consumeClick()) {
				minimizedHud = !minimizedHud;

				if (client.player != null) {
					Component message = Component.translatable("r3ct_daily.message.hud_toggle", minimizedHud ? "§4OFF" : "§aON");
					client.gui.hud.setOverlayMessage(message, false);
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				DailyServerConfig.syncFromServer(payload.questsJson(), payload.rewardsJson(), payload.serverJson());
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			DailyServerConfig.loadAll();
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenRewardsPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				PlayerData data = new PlayerData();
				data.rewardDay = payload.rewardDay();
				data.lastRewardDate = payload.lastRewardDate();
				data.streak = payload.streak();
				data.totalCollected = payload.totalCollected();
				data.claimedRewardHistory = payload.claimedRewardHistory();
				data.availableRewardFreezes = payload.availableRewardFreezes();
				data.claimedBonusRewards = payload.claimedBonusRewards();
				context.client().gui.setScreen(new RewardScreen(data, payload));
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenQuestsPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
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

				clientQuestData = data;
				context.client().gui.setScreen(new QuestScreen(data, payload));
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncQuestsPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				if (clientQuestData != null && clientQuestData.questProgress != null) {
					for (int i = 0; i < payload.questProgress().size() && i < clientQuestData.questProgress.size(); i++) {
						int oldVal = clientQuestData.questProgress.get(i);
						int newVal = payload.questProgress().get(i);

						if (newVal > oldVal) {
							flashTimestamps[i] = System.currentTimeMillis();
							flashIsGreen[i] = true;
						} else if (newVal < oldVal) {
							flashTimestamps[i] = System.currentTimeMillis();
							flashIsGreen[i] = false;
						}
					}
				}

				if (clientQuestData == null) clientQuestData = new PlayerData();
				clientQuestData.questStreak = payload.questStreak();
				clientQuestData.totalQuestPoints = payload.totalQuestPoints();
				clientQuestData.dailyQuestsCompletedToday = payload.dailyQuestsCompletedToday();
				clientQuestData.activeQuests = payload.activeQuests();
				clientQuestData.questProgress = payload.questProgress();
				clientQuestData.streak = payload.streak();

				clientQuestData.perfectDaysCount = payload.perfectDaysCount();
				clientQuestData.availableFreezes = payload.availableFreezes();
				clientQuestData.availableRewardFreezes = payload.availableRewardFreezes();

				clientQuestData.questRewardsClaimed = payload.questRewardsClaimed();
				clientQuestData.claimedPointRewards = payload.claimedPointRewards();
			});
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DailyFabric.MOD_ID, "quest_hud"), (guiGraphics, deltaTracker) -> {
			Minecraft client = Minecraft.getInstance();

			if (client.gui.hud.isHidden() || client.getDebugOverlay().showDebugScreen() || client.player == null) return;

			if (client.gui.screen() != null && !(client.gui.screen() instanceof ChatScreen)) return;

			if (!DailyClientConfig.getInstance().enableHud) return;

			int screenWidth = guiGraphics.guiWidth();
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

			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().scale(scale, scale);

			int virtualWidth = (int) (screenWidth / scale);
			int xOffset = (int) (rawXOffset / scale);
			int currentY = (int) (rawYOffset / scale);

			if (clientQuestData == null || clientQuestData.activeQuests.isEmpty()) {
				if (!minimizedHud) {
					String loadingMsg = "§e" + Component.translatable("r3ct_daily.hud.loading").getString();
					int xPos = isRight ? virtualWidth - client.font.width(loadingMsg) - xOffset : xOffset;
					guiGraphics.text(client.font, loadingMsg, xPos, currentY, baseColor, true);
				}
				guiGraphics.pose().popMatrix();
				return;
			}

			if (!minimizedHud) {
				String title = "§e§l" + Component.translatable("r3ct_daily.quests.header.daily_quests").getString();
				int xPos = isRight ? virtualWidth - client.font.width(title) - xOffset : xOffset;
				guiGraphics.text(client.font, title, xPos, currentY, baseColor, true);
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
					guiGraphics.text(client.font, mark, xPos, currentY, baseColor, true);
				} else {
					String questName;
					if (q.name != null && !q.name.isEmpty()) {
						questName = Component.translatable(q.name).getString();
					} else {
						questName = Component.translatable(q.description).getString().split(" ")[0];
					}

					int maxNameLength = 25;
					if (questName.length() > maxNameLength) {
						questName = questName.substring(0, maxNameLength) + "...";
					}

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
					guiGraphics.text(client.font, lineText, xPos, currentY, baseColor, true);
				}
				currentY += 10;
			}

			guiGraphics.pose().popMatrix();
		});

		ClientPlayNetworking.registerGlobalReceiver(LeaderboardResponsePayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				context.client().gui.setScreen(new LeaderboardScreen(payload.boardType(), payload.leftList(), payload.rightList()));
			});
		});
	}
}