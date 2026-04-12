package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
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
    @Unique private int elytraCmBuffer = 0;
    @Unique private int horseCmBuffer = 0;
    @Unique private int minecartCmBuffer = 0;
    @Unique private int striderCmBuffer = 0;
    @Unique private int pearlCmBuffer = 0;

    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("HEAD"))
    private void onAwardStat(Stat<?> stat, int amount, CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;

        if (stat.getType() == Stats.ITEM_CRAFTED && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", itemId, amount);

            if (itemId.contains("_fence") || itemId.contains("_fence_gate")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_FENCE_GATE", "any", amount);
            }
            if (itemId.contains("chest_boat")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_BOAT_WITH_CHEST", "any", amount);
            }
            if (itemId.equals("minecraft:spyglass") || itemId.equals("minecraft:filled_map")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:spyglass_or_map", amount);
            }
            if (itemId.equals("minecraft:clock") || itemId.equals("minecraft:compass")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:clock_or_compass", amount);
            }
            if (itemId.contains("_banner_pattern")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:banner_patterns", amount);
            }
            if (itemId.contains("_harness")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct:harnesses", amount);
            }
        }

        else if (stat.getType() == Stats.ITEM_BROKEN && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            QuestManager.handleAction(serverPlayer, "BREAK_ITEM", itemId, amount);
            QuestManager.handleAction(serverPlayer, "BREAK_ITEM", "any", amount);

            if (itemId.contains("_boots")) {
                QuestManager.handleAction(serverPlayer, "BREAK_ITEM", "r3ct:boots", amount);
            }
        }

        else if (stat.getType() == Stats.CUSTOM && stat.getValue() instanceof net.minecraft.resources.Identifier rl) {
            String statName = rl.toString();

            if (statName.equals("minecraft:walk_one_cm")) {
                walkCmBuffer += amount;
                if (walkCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "WALK_DISTANCE", "any", walkCmBuffer / 100); walkCmBuffer %= 100; }
            } else if (statName.equals("minecraft:sprint_one_cm")) {
                sprintCmBuffer += amount;
                if (sprintCmBuffer >= 100) {
                    int blocks = sprintCmBuffer / 100;
                    QuestManager.handleAction(serverPlayer, "SPRINT_DISTANCE", "any", blocks);
                    QuestManager.handleAction(serverPlayer, "WALK_DISTANCE", "any", blocks);

                    sprintCmBuffer %= 100;
                }
            } else if (statName.equals("minecraft:boat_one_cm")) {
                boatCmBuffer += amount;
                if (boatCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "BOAT_DISTANCE", "any", boatCmBuffer / 100); boatCmBuffer %= 100; }
            } else if (statName.equals("minecraft:minecart_one_cm")) {
                minecartCmBuffer += amount;
                if (minecartCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "MINECART_DISTANCE", "any", minecartCmBuffer / 100); minecartCmBuffer %= 100; }
            } else if (statName.equals("minecraft:strider_one_cm")) {
                striderCmBuffer += amount;
                if (striderCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "STRIDER_DISTANCE", "any", striderCmBuffer / 100); striderCmBuffer %= 100; }
            } else if (statName.equals("minecraft:aviate_one_cm")) {
                elytraCmBuffer += amount;
                if (elytraCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "ELYTRA_DISTANCE", "any", elytraCmBuffer / 100); elytraCmBuffer %= 100; }
            } else if (statName.equals("minecraft:swim_one_cm")) {
                swimCmBuffer += amount;
                if (swimCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "SWIM_DISTANCE", "any", swimCmBuffer / 100); swimCmBuffer %= 100; }
            } else if (statName.equals("minecraft:horse_one_cm")) {
                horseCmBuffer += amount;
                if (horseCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "HORSE_DISTANCE", "any", horseCmBuffer / 100); horseCmBuffer %= 100; }
            } else if (statName.equals("minecraft:ender_pearl_one_cm")) {
                pearlCmBuffer += amount;
                if (pearlCmBuffer >= 100) { QuestManager.handleAction(serverPlayer, "PEARL_DISTANCE", "any", pearlCmBuffer / 100); pearlCmBuffer %= 100; }
            }
            else if (statName.equals("minecraft:fall_one_cm")) {
                QuestManager.handleAction(serverPlayer, "FALL_DISTANCE", "any", amount);
            } else if (statName.equals("minecraft:jump")) {
                QuestManager.handleAction(serverPlayer, "JUMP", "any", amount);
            } else if (statName.equals("minecraft:damage_blocked_by_shield")) {
                QuestManager.handleAction(serverPlayer, "BLOCK_DAMAGE", "any", 1);
            }
        }
        else if (stat.getType() == Stats.ITEM_USED && stat.getValue() instanceof net.minecraft.world.item.Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            QuestManager.handleAction(serverPlayer, "USE_ITEM", itemId, amount);
            QuestManager.handleAction(serverPlayer, "USE_ITEM", "any", amount);
        }
    }
}