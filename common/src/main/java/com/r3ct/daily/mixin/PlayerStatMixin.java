package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerStatMixin {

    @Unique private int walkCmBuffer = 0;
    @Unique private int sprintCmBuffer = 0;
    @Unique private int swimCmBuffer = 0;
    @Unique private int boatCmBuffer = 0;
    @Unique private int horseCmBuffer = 0;
    @Unique private int minecartCmBuffer = 0;
    @Unique private int striderCmBuffer = 0;
    @Unique private int pigCmBuffer = 0;

    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("HEAD"))
    private void onAwardStat(Stat<?> stat, int amount, CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;

        if (stat.getType() == Stats.ITEM_CRAFTED && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", itemId, amount);

            if (itemId.contains("_banner_pattern")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:banner_patterns", amount);
            }
            if (itemId.contains("_harness")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:harnesses", amount);
            }
            if (itemId.equals("minecraft:clock") || itemId.equals("minecraft:compass")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:clock_or_compass", amount);
            }

            if (itemId.contains("_fence") || itemId.contains("_fence_gate")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_FENCE_GATE", "any", amount);
            }
            if (itemId.contains("chest_boat")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_BOAT_WITH_CHEST", "any", amount);
            }
        }

        if (stat.getType() == Stats.ITEM_BROKEN && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            QuestManager.handleAction(serverPlayer, "BREAK_ITEM", itemId, amount);

            if (itemId.endsWith("_boots")) {
                QuestManager.handleAction(serverPlayer, "BREAK_ITEM", "r3ct:boots", amount);
            }
        }

        if (stat.getType() == Stats.CUSTOM && stat.getValue() instanceof net.minecraft.resources.Identifier statId) {
            String id = statId.toString();
            int blocks = 0;

            switch (id) {
                case "minecraft:jump":
                    QuestManager.handleAction(serverPlayer, "JUMP", "any", amount);
                    break;
                case "minecraft:walk_one_cm":
                case "minecraft:crouch_one_cm":
                    walkCmBuffer += amount;
                    blocks = walkCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "WALK_DISTANCE", "any", blocks);
                        QuestManager.handleAction(serverPlayer, "WALK_OR_SPRINT_DISTANCE", "any", blocks);
                        walkCmBuffer %= 100;
                    }
                    break;
                case "minecraft:sprint_one_cm":
                    sprintCmBuffer += amount;
                    blocks = sprintCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "SPRINT_DISTANCE", "any", blocks);
                        QuestManager.handleAction(serverPlayer, "WALK_OR_SPRINT_DISTANCE", "any", blocks);
                        sprintCmBuffer %= 100;
                    }
                    break;
                case "minecraft:swim_one_cm":
                    swimCmBuffer += amount;
                    blocks = swimCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "SWIM_DISTANCE", "any", blocks);
                        swimCmBuffer %= 100;
                    }
                    break;
                case "minecraft:boat_one_cm":
                    boatCmBuffer += amount;
                    blocks = boatCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "BOAT_DISTANCE", "any", blocks);
                        boatCmBuffer %= 100;
                    }
                    break;
                case "minecraft:minecart_one_cm":
                    minecartCmBuffer += amount;
                    blocks = minecartCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "MINECART_DISTANCE", "any", blocks);
                        minecartCmBuffer %= 100;
                    }
                    break;
                case "minecraft:horse_one_cm":
                    horseCmBuffer += amount;
                    blocks = horseCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "HORSE_DISTANCE", "any", blocks);
                        horseCmBuffer %= 100;
                    }
                    break;
                case "minecraft:strider_one_cm":
                    striderCmBuffer += amount;
                    blocks = striderCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "STRIDER_DISTANCE", "any", blocks);
                        striderCmBuffer %= 100;
                    }
                    break;
                case "minecraft:pig_one_cm":
                    pigCmBuffer += amount;
                    blocks = pigCmBuffer / 100;
                    if (blocks > 0) {
                        QuestManager.handleAction(serverPlayer, "RIDE_PIG_DISTANCE", "any", blocks);
                        pigCmBuffer %= 100;
                    }
                    break;
                case "minecraft:bell_ring":
                    QuestManager.handleAction(serverPlayer, "RING_BELL", "any", amount);
                    break;
            }
        }

        if (stat.getType() == Stats.ITEM_USED && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            QuestManager.handleAction(serverPlayer, "USE_ITEM", itemId, amount);

            if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg") || itemId.equals("minecraft:blue_egg")) {
                QuestManager.handleAction(serverPlayer, "THROW_EGG", "any", amount);
            }
        }
    }
}