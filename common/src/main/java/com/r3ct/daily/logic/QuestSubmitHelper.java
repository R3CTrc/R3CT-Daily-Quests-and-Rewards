package com.r3ct.daily.logic;

import com.r3ct.daily.client.screen.ConfirmSubmitScreen;
import com.r3ct.daily.client.screen.ItemSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QuestSubmitHelper {

    public static void handleQuestSubmitClick(Screen parentScreen, Quest q, int questIndex, int currentProgress) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<Integer> matchingSlots = new ArrayList<>();
        int totalAvailable = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (QuestManager.isItemMatchingTarget(stack, q.target)) {
                matchingSlots.add(i);
                totalAvailable += stack.getCount();
            }
        }

        if (matchingSlots.isEmpty()) return;

        boolean allSame = true;
        ItemStack firstStack = player.getInventory().getItem(matchingSlots.get(0));
        for (int slot : matchingSlots) {
            if (!ItemStack.isSameItemSameComponents(firstStack, player.getInventory().getItem(slot))) {
                allSame = false;
                break;
            }
        }

        int amountToTake = Math.min(q.requiredAmount - currentProgress, totalAvailable);

        if (allSame) {
            Minecraft.getInstance().gui.setScreen(new ConfirmSubmitScreen(parentScreen, q, questIndex, -1, amountToTake));
        } else {
            Minecraft.getInstance().gui.setScreen(new ItemSelectionScreen(parentScreen, q, questIndex, matchingSlots, amountToTake));
        }
    }

    public static int countItems(String target) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;

        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (QuestManager.isItemMatchingTarget(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}