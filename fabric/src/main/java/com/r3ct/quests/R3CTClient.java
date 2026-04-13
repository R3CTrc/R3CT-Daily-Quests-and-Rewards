package com.r3ct.quests;

import com.r3ct.quests.data.PlayerData;
import com.r3ct.quests.logic.Quest;
import com.r3ct.quests.logic.QuestManager;
import com.r3ct.quests.network.LeaderboardResponsePayload;
import com.r3ct.quests.network.OpenQuestsPayload;
import com.r3ct.quests.network.OpenRewardsPayload;
import com.r3ct.quests.network.SyncQuestsPayload;
import com.r3ct.quests.screen.LeaderboardScreen;
import com.r3ct.quests.screen.QuestScreen;
import com.r3ct.quests.screen.RewardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class R3CTClient implements ClientModInitializer {
	public static KeyMapping openRewardsKey;
	public static KeyMapping openQuestsKey;
	private static KeyMapping toggleHudKey;

	private static final KeyMapping.Category R3CT_CATEGORY = KeyMapping.Category.register(Identifier.parse(R3CT.MOD_ID + ":main"));

	public static PlayerData clientQuestData = null;
	private static boolean minimizedHud = false;
	private static final long[] flashTimestamps = new long[10];
	private static final boolean[] flashIsGreen = new boolean[10];

	@Override
	public void onInitializeClient() {
		QuestManager.init();

		com.r3ct.quests.config.R3CTQuestsConfig.load();

		openRewardsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.r3ct.open_rewards",
				com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				R3CT_CATEGORY
		));

		openQuestsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.r3ct.open_quests",
				com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				R3CT_CATEGORY
		));

		toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.r3ct.toggle_hud",
				com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
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
					String stateText = minimizedHud ? "§4OFF" : "§aON";
					client.player.displayClientMessage(
							Component.translatable("r3ct.message.hud_toggle", stateText),
							false
					);
				}
			}
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
				context.client().setScreen(new RewardScreen(data));
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
				context.client().setScreen(new QuestScreen(data));
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

		HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.options.hideGui || client.getDebugOverlay().showDebugScreen() || client.player == null) return;

			if (client.screen != null && !(client.screen instanceof ChatScreen)) return;

			if (!com.r3ct.quests.config.R3CTQuestsConfig.getInstance().enableHud) return;

			int screenWidth = guiGraphics.guiWidth();
			int rawXOffset = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudXOffset;
			int rawYOffset = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudYOffset;

			double currentGuiScale = Math.max(1.0, client.getWindow().getGuiScale());
			float targetGuiScale = 2.0f;
			float configScale = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().hudScale;
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
					String loadingMsg = "§e" + Component.translatable("r3ct.hud.loading").getString();
					guiGraphics.drawString(client.font, loadingMsg, virtualWidth - client.font.width(loadingMsg) - xOffset, currentY, baseColor, true);
				}
				guiGraphics.pose().popMatrix();
				return;
			}

			if (!minimizedHud) {
				String title = "§e§l" + Component.translatable("r3ct.quests.header.daily_quests").getString();
				guiGraphics.drawString(client.font, title, virtualWidth - client.font.width(title) - xOffset, currentY, baseColor, true);
			}
			currentY += 12;

			for (int i = 0; i < clientQuestData.activeQuests.size(); i++) {
				Quest q = QuestManager.getQuestById(clientQuestData.activeQuests.get(i));
				if (q == null) continue;

				int progress = clientQuestData.questProgress.get(i);
				boolean done = progress >= q.requiredAmount;

				String mark = done ? "§a" + Component.translatable("r3ct.quests.status.claimed").getString() : "§c" + Component.translatable("r3ct.quests.status.incomplete").getString();

				if (minimizedHud) {
					guiGraphics.drawString(client.font, mark, virtualWidth - client.font.width(mark) - xOffset, currentY, baseColor, true);
				} else {
					String questName;
					if (q.name != null && !q.name.isEmpty()) {
						questName = Component.translatable(q.name).getString();
					} else {
						questName = Component.translatable(q.description).getString().split(" ")[0];
					}
					String diffIndicator = (q.difficulty == 0) ? "§2★ " : (q.difficulty == 1 ? "§6★ " : "§4★ ");

					String progressColor = "§f";
					if (System.currentTimeMillis() - flashTimestamps[i] < 600) {
						progressColor = flashIsGreen[i] ? "§a" : "§c";
					} else if (done) {
						progressColor = "§7";
					}

					String lineText = diffIndicator + progressColor + questName + " (" + progress + "/" + q.requiredAmount + ") " + mark;
					guiGraphics.drawString(client.font, lineText, virtualWidth - client.font.width(lineText) - xOffset, currentY, baseColor, true);
				}
				currentY += 10;
			}

			guiGraphics.pose().popMatrix();
		});

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(LeaderboardResponsePayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				context.client().setScreen(new LeaderboardScreen(payload.boardType(), payload.leftList(), payload.rightList()));
			});
		});
	}
}