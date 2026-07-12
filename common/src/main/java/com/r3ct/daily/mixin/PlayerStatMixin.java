package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

    @Unique private int r3ct_daily$walkCmBuffer = 0;
    @Unique private int r3ct_daily$sprintCmBuffer = 0;
    @Unique private int r3ct_daily$swimCmBuffer = 0;
    @Unique private int r3ct_daily$boatCmBuffer = 0;
    @Unique private int r3ct_daily$horseCmBuffer = 0;
    @Unique private int r3ct_daily$minecartCmBuffer = 0;
    @Unique private int r3ct_daily$striderCmBuffer = 0;
    @Unique private int r3ct_daily$pigCmBuffer = 0;

    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("HEAD"))
    private void r3ct_daily$onAwardStat(Stat<?> stat, int amount, CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;

        if (serverPlayer.connection == null) return;

        if (stat.getType() == Stats.ITEM_CRAFTED && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", itemId, amount);

            if (itemId.contains("_banner_pattern")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct_daily:banner_patterns", amount);
            }
            if (itemId.contains("_harness")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct_daily:harnesses", amount);
            }
            if (itemId.equals("minecraft:clock") || itemId.equals("minecraft:compass")) {
                QuestManager.handleAction(serverPlayer, "CRAFT_ITEM", "r3ct_daily:clock_or_compass", amount);
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
        }

        if (stat.getType() == Stats.CUSTOM && stat.getValue() instanceof Identifier statId) {
            String id = statId.toString();
            int blocks = 0;

            switch (id) {
                case "minecraft:jump":
                    QuestManager.handleAction(serverPlayer, "JUMP", "any", amount);
                    break;
                case "minecraft:walk_one_cm":
                case "minecraft:crouch_one_cm":
                    this.r3ct_daily$walkCmBuffer += amount;
                    if (this.r3ct_daily$walkCmBuffer >= 100) {
                        blocks = this.r3ct_daily$walkCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "WALK_DISTANCE", "any", blocks);
                        QuestManager.handleAction(serverPlayer, "WALK_OR_SPRINT_DISTANCE", "any", blocks);
                        this.r3ct_daily$walkCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:sprint_one_cm":
                    this.r3ct_daily$sprintCmBuffer += amount;
                    if (this.r3ct_daily$sprintCmBuffer >= 100) {
                        blocks = this.r3ct_daily$sprintCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "SPRINT_DISTANCE", "any", blocks);
                        QuestManager.handleAction(serverPlayer, "WALK_OR_SPRINT_DISTANCE", "any", blocks);
                        this.r3ct_daily$sprintCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:swim_one_cm":
                    this.r3ct_daily$swimCmBuffer += amount;
                    if (this.r3ct_daily$swimCmBuffer >= 100) {
                        blocks = this.r3ct_daily$swimCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "SWIM_DISTANCE", "any", blocks);
                        this.r3ct_daily$swimCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:boat_one_cm":
                    this.r3ct_daily$boatCmBuffer += amount;
                    if (this.r3ct_daily$boatCmBuffer >= 100) {
                        blocks = this.r3ct_daily$boatCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "BOAT_DISTANCE", "any", blocks);
                        this.r3ct_daily$boatCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:minecart_one_cm":
                    this.r3ct_daily$minecartCmBuffer += amount;
                    if (this.r3ct_daily$minecartCmBuffer >= 100) {
                        blocks = this.r3ct_daily$minecartCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "MINECART_DISTANCE", "any", blocks);
                        this.r3ct_daily$minecartCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:horse_one_cm":
                    this.r3ct_daily$horseCmBuffer += amount;
                    if (this.r3ct_daily$horseCmBuffer >= 100) {
                        blocks = this.r3ct_daily$horseCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "HORSE_DISTANCE", "any", blocks);
                        this.r3ct_daily$horseCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:strider_one_cm":
                    this.r3ct_daily$striderCmBuffer += amount;
                    if (this.r3ct_daily$striderCmBuffer >= 100) {
                        blocks = this.r3ct_daily$striderCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "STRIDER_DISTANCE", "any", blocks);
                        this.r3ct_daily$striderCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:pig_one_cm":
                    this.r3ct_daily$pigCmBuffer += amount;
                    if (this.r3ct_daily$pigCmBuffer >= 100) {
                        blocks = this.r3ct_daily$pigCmBuffer / 100;
                        QuestManager.handleAction(serverPlayer, "PIG_DISTANCE", "any", blocks);
                        this.r3ct_daily$pigCmBuffer -= (blocks * 100);
                    }
                    break;
                case "minecraft:bell_ring":
                    QuestManager.handleAction(serverPlayer, "RING_BELL", "any", amount);
                    break;
            }
        }

        if (stat.getType() == Stats.ITEM_USED && stat.getValue() instanceof Item item) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            if (itemId.equals("minecraft:goat_horn")) {
                QuestManager.handleAction(serverPlayer, "BLOW_HORN", "any", amount);
            }
        }
    }
}