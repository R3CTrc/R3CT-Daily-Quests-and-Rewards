package com.r3ct.daily.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

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
        if (this.rawRewardId.startsWith("r3ct_daily:")) {
            item = Items.NETHER_STAR;
        } else {
            Identifier itemId = Identifier.parse(
                    (this.rawRewardId.contains(":") ? this.rawRewardId : "minecraft:" + this.rawRewardId).toLowerCase(java.util.Locale.ROOT)
            );
            item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.DIRT);
        }

        ItemStack rewardStack = new ItemStack(item, this.rewardAmount);

        if (this.rawRewardId.startsWith("r3ct_daily:")) {
            rewardStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("r3ct_daily.item.special_reward"));
        }

        return rewardStack;
    }
}