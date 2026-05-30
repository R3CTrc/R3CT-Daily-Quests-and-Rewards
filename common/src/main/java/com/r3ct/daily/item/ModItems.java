package com.r3ct.daily.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import java.util.List;

public class ModItems {
    public static final ResourceKey<Item> QUEST_SHIELD_KEY = ResourceKey.create(Registries.ITEM, Identifier.parse("r3ct_daily:quest_shield"));
    public static final ResourceKey<Item> REWARD_SHIELD_KEY = ResourceKey.create(Registries.ITEM, Identifier.parse("r3ct_daily:reward_shield"));

    public static final Item QUEST_SHIELD = new StreakShieldItem(new Item.Properties()
            .setId(QUEST_SHIELD_KEY)
            .stacksTo(16)
            .rarity(Rarity.EPIC)
            .component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.r3ct_daily.quest_shield.description").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)),
                    Component.translatable("r3ct_daily.tooltip.shield_usage").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(false))
            ))), true);

    public static final Item REWARD_SHIELD = new StreakShieldItem(new Item.Properties()
            .setId(REWARD_SHIELD_KEY)
            .stacksTo(16)
            .rarity(Rarity.EPIC)
            .component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.r3ct_daily.reward_shield.description").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)),
                    Component.translatable("r3ct_daily.tooltip.shield_usage").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(false))
            ))), false);

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, Identifier.parse("r3ct_daily:quest_shield"), QUEST_SHIELD);
        Registry.register(BuiltInRegistries.ITEM, Identifier.parse("r3ct_daily:reward_shield"), REWARD_SHIELD);
    }
}