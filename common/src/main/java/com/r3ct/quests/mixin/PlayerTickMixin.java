package com.r3ct.quests.mixin;

import com.r3ct.quests.data.ModState;
import com.r3ct.quests.data.PlayerData;
import com.r3ct.quests.logic.Quest;
import com.r3ct.quests.logic.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTickMixin {

    @Unique private int questTickCounter = 0;
    @Unique private double levitationStartY = -1;
    @Unique private net.minecraft.world.phys.Vec3 lastElytraPos = null;
    @Unique private double continuousElytraFlight = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            questTickCounter++;
            if (player.isFallFlying()) {
                if (lastElytraPos != null) {
                    continuousElytraFlight += player.position().distanceTo(lastElytraPos);
                    QuestManager.handleAction(player, "ELYTRA_FLIGHT_NO_LAND", "any", (int)continuousElytraFlight);
                }
                lastElytraPos = player.position();
            } else {
                if (continuousElytraFlight > 0 && (player.onGround() || player.isInWater() || player.isInLava())) {
                    continuousElytraFlight = 0;
                    QuestManager.handleAction(player, "ELYTRA_FLIGHT_NO_LAND", "any", 0);
                }
                lastElytraPos = null;
            }
            if (player.fallDistance >= 30.0f) {
                QuestManager.handleAction(player, "FALL_FROM_HEIGHT", "30_blocks", 1);
            }

            if (questTickCounter >= 20) {
                questTickCounter = 0;

                net.minecraft.server.MinecraftServer server = player.level().getServer();
                if (server != null) {
                    PlayerData data = ModState.getPlayerData(server, player.getUUID());
                    for (String qId : data.activeQuests) {
                        Quest q = QuestManager.getQuestById(qId);
                        if (q != null && (q.actionType.equals("HAS_ITEMS") || q.actionType.equals("PICKUP_ITEM"))) {
                            int count = 0;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack invStack = player.getInventory().getItem(i);
                                if (!invStack.isEmpty()) {
                                    String invId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(invStack.getItem()).toString();
                                    if (invId.equals(q.target) || q.target.equals("any") ||
                                            (q.target.equals("r3ct:mushrooms") && invId.contains("_mushroom")) ||
                                            (q.target.equals("r3ct:sniffer_seeds") && (invId.equals("minecraft:torchflower_seeds") || invId.equals("minecraft:pitcher_pod"))) ||
                                            (q.target.equals("r3ct:flowers") && invStack.is(net.minecraft.tags.ItemTags.FLOWERS)) ||
                                            (q.target.equals("r3ct:leaves") && invStack.is(net.minecraft.tags.ItemTags.LEAVES)) ||
                                            (q.target.equals("r3ct:raw_fishes") && (invId.equals("minecraft:cod") || invId.equals("minecraft:salmon") || invId.equals("minecraft:tropical_fish") || invId.equals("minecraft:pufferfish")))) {
                                        count += invStack.getCount();
                                    }
                                }
                            }
                            QuestManager.handleAction(player, q.actionType, q.target, count);
                        }
                    }
                }

                String biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
                QuestManager.handleAction(player, "VISIT_BIOME", biomeId, 1);

                if (biomeId.contains("frozen") || biomeId.contains("snowy") || biomeId.contains("ice") || biomeId.contains("grove") || biomeId.contains("slopes") || biomeId.contains("peaks")) {
                    QuestManager.handleAction(player, "VISIT_BIOME", "r3ct:frozen_biomes", 1);
                }

                int y = player.getBlockY();
                if (y > 200) QuestManager.handleAction(player, "ALTITUDE_HIGH", "any", 1);
                else if (y < -50) QuestManager.handleAction(player, "ALTITUDE_LOW", "any", 1);

                int effectCount = player.getActiveEffects().size();
                if (effectCount >= 3) QuestManager.handleAction(player, "HAS_EFFECTS", "3_plus", 1);
                if (player.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE)) QuestManager.handleAction(player, "HERO_OF_THE_VILLAGE", "any", 1);

                int diamondPieces = 0;
                net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                        net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                        net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET
                };
                for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
                    ItemStack armorStack = player.getItemBySlot(slot);
                    if (!armorStack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(armorStack.getItem()).toString().contains("diamond_")) diamondPieces++;
                }
                if (diamondPieces == 4) QuestManager.handleAction(player, "EQUIP_ARMOR", "diamond", 1);
                if (player.getBlockY() < 0) QuestManager.handleAction(player, "TIME_BELOW_Y0", "any", 1);
                if (biomeId.contains("ocean")) QuestManager.handleAction(player, "TIME_IN_BIOME", "ocean", 1);
                if (player.isInLava() && player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) QuestManager.handleAction(player, "SWIM_LAVA_FIRE_RES", "any", 1);

                net.minecraft.server.level.ServerLevel sLevel = player.level();
                var structureRegistry = sLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                var endCityStructure = structureRegistry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY).value();
                if (sLevel.structureManager().getStructureAt(player.blockPosition(), endCityStructure).isValid()) {
                    QuestManager.handleAction(player, "ENTER_STRUCTURE", "minecraft:end_city", 1);
                    if (player.getBlockY() > 130) QuestManager.handleAction(player, "ENTER_STRUCTURE", "minecraft:end_city_ship", 1);
                }

                if (player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
                    if (levitationStartY == -1) levitationStartY = player.getY();
                    if (player.getY() - levitationStartY >= 20) QuestManager.handleAction(player, "LEVITATION_HEIGHT", "any", 1);
                } else { levitationStartY = -1; }
            }
        }
    }
}