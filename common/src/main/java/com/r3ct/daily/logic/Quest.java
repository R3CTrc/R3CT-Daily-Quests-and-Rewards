package com.r3ct.daily.logic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class Quest {
    public String id;
    public String name;
    public String description;
    public int requiredAmount;
    public int difficulty;
    public int points;
    public String requiredDimension;
    public String requiredLocation;

    public String actionType;
    public String target;
    public String rawRewardId;
    public int rewardAmount;

    public Quest(String id, String name, String description, int requiredAmount, int difficulty, int points, int rewardAmount, String requiredDimension, String requiredLocation, String actionType, String target, String rawRewardId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredAmount = requiredAmount;
        this.difficulty = difficulty;
        this.points = points;
        this.rewardAmount = rewardAmount;
        this.requiredDimension = requiredDimension;
        this.requiredLocation = requiredLocation;
        this.actionType = actionType;
        this.target = target;
        this.rawRewardId = rawRewardId;
    }

    public ItemStack getItemReward() {
        Item item;
        boolean isFallback = false;

        if (this.rawRewardId.startsWith("r3ct_daily:")) {
            item = Items.NETHER_STAR;
        } else {
            Identifier itemId = Identifier.parse(
                    (this.rawRewardId.contains(":") ? this.rawRewardId : "minecraft:" + this.rawRewardId).toLowerCase(Locale.ROOT)
            );

            var itemOpt = BuiltInRegistries.ITEM.getOptional(itemId);
            item = itemOpt.orElse(Items.PAPER);

            if (itemOpt.isEmpty()) {
                isFallback = true;
            }
        }

        ItemStack rewardStack = new ItemStack(item, this.rewardAmount);

        if (isFallback) {
            rewardStack.set(DataComponents.CUSTOM_NAME, Component.literal("Report this to admin!"));
        } else if (this.rawRewardId.startsWith("r3ct_daily:")) {
            rewardStack.set(DataComponents.CUSTOM_NAME, Component.translatable("r3ct_daily.item.special_reward"));
        }

        return rewardStack;
    }
}