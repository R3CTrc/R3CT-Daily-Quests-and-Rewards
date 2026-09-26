package com.r3ct.daily.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {

    public static final Item QUEST_SHIELD = new StreakShieldItem(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.EPIC), true);

    public static final Item REWARD_SHIELD = new StreakShieldItem(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.EPIC), false);

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("r3ct_daily", "quest_shield"), QUEST_SHIELD);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("r3ct_daily", "reward_shield"), REWARD_SHIELD);
    }
}