package com.r3ct.quests;

import net.minecraft.world.item.ItemStack;

public class Quest {
    public String id;
    public String name;
    public String description;
    public int requiredAmount;
    public int difficulty;
    public int points;
    public ItemStack itemReward;
    public String requiredDimension;

    public String actionType;
    public String target;
    public String rawRewardId;

    public Quest(String id, String name, String description, int requiredAmount, int difficulty, int points, ItemStack itemReward, String requiredDimension, String actionType, String target, String rawRewardId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredAmount = requiredAmount;
        this.difficulty = difficulty;
        this.points = points;
        this.itemReward = itemReward;
        this.requiredDimension = requiredDimension;
        this.actionType = actionType;
        this.target = target;
        this.rawRewardId = rawRewardId;
    }
}