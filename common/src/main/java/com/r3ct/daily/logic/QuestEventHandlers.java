package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class QuestEventHandlers {

    public static void onDimensionChange(ServerPlayer player, String dimId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();

        if (server == null) return;

        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (!data.unlockedDimensions.contains(dimId)) {
            data.unlockedDimensions.add(dimId);

            String translationKey = "r3ct.dimension." + dimId.replace(':', '.');
            net.minecraft.network.chat.Component dimComp = net.minecraft.network.chat.Component.translatable(translationKey).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

            player.sendSystemMessage(net.minecraft.network.chat.Component.empty().append(QuestManager.getPrefix()).append(
                    net.minecraft.network.chat.Component.translatable("r3ct.message.dimension_discovered", dimComp).withStyle(net.minecraft.ChatFormatting.GREEN)
            ));

            server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        }
    }

    public static void onBlockBreak(ServerPlayer serverPlayer, BlockState state, BlockPos pos, Level level) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (QuestManager.removePlacedBlock(pos, level)) {
            QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", blockId, -1);

            if (state.is(net.minecraft.tags.BlockTags.SAPLINGS)) {
                QuestManager.handleAction(serverPlayer, "PLACE_SAPLING", blockId, -1);
            }
            if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock) {
                QuestManager.handleAction(serverPlayer, "PLACE_SEED", blockId, -1);
            }

            return;
        }

        QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", blockId, 1);

        if (state.is(net.minecraft.tags.BlockTags.OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:oak_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.BIRCH_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:birch_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.SPRUCE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:spruce_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.JUNGLE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:jungle_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.ACACIA_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:acacia_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.DARK_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:dark_oak_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.MANGROVE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:mangrove_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.CHERRY_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:cherry_logs", 1);
        else if (state.is(net.minecraft.tags.BlockTags.PALE_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:pale_oak_logs", 1);

        if (state.is(net.minecraft.tags.BlockTags.COAL_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:coal_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.IRON_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:iron_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.GOLD_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:gold_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.COPPER_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:copper_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:diamond_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.LAPIS_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:lapis_ores", 1);
        if (state.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:redstone_ores", 1);

        if (blockId.equals("minecraft:nether_quartz_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct:nether_quartz_ores", 1);
    }

    public static void onEntityDeath(ServerPlayer serverPlayer, Entity victim) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();

        QuestManager.handleAction(serverPlayer, "KILL_MOB", mobId, 1);

        if (serverPlayer.getHealth() <= 4.0f) {
            QuestManager.handleAction(serverPlayer, "KILL_LOW_HP", mobId, 1);
        }

        if (mobId.equals("minecraft:skeleton") && serverPlayer.level().dimension().identifier().toString().equals("minecraft:the_nether")) {
            QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_NETHER", "minecraft:skeleton", 1);
        }

        if (victim instanceof net.minecraft.world.entity.monster.illager.Pillager pillager) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(pillager.blockPosition(), 9216);
                if (raid == null) QuestManager.handleAction(serverPlayer, "KILL_MOB_NO_RAID", "minecraft:pillager", 1);
            }
        } else if (victim instanceof net.minecraft.world.entity.monster.Ravager ravager) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(ravager.blockPosition(), 9216);
                if (raid != null) QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_RAID", "minecraft:ravager", 1);
            }
        }

        if (victim instanceof net.minecraft.world.entity.monster.Monster) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel && serverLevel.isVillage(victim.blockPosition())) {
                QuestManager.handleAction(serverPlayer, "KILL_MOB_VILLAGE", mobId, 1);
            }
        }
    }

    public static void onItemEnchanted(ServerPlayer player, net.minecraft.world.item.ItemStack item) {
        QuestManager.handleAction(player, "ENCHANT_ITEM", "any", 1);

        net.minecraft.world.item.enchantment.ItemEnchantments enchantments = item.getOrDefault(
                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
        );

        enchantments.keySet().forEach(holder -> {
            holder.unwrapKey().ifPresent(key -> {
                String enchantId = key.identifier().toString();
                QuestManager.handleAction(player, "ENCHANT_WITH", enchantId, 1);
            });
        });
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server == null) return;

            PlayerData data = ModState.getPlayerData(server, player.getUUID());
            boolean needsBiomeCheck = false;

            for (String qId : data.activeQuests) {
                Quest q = QuestManager.getQuestById(qId);
                if (q != null) {

                    if (q.actionType.equals("VISIT_BIOME") || q.actionType.equals("TIME_IN_BIOME")) {
                        needsBiomeCheck = true;
                    }

                    if (q.actionType.equals("HAS_ITEMS")) {
                        int count = 0;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack invStack = player.getInventory().getItem(i);
                            if (!invStack.isEmpty()) {
                                String invId = BuiltInRegistries.ITEM.getKey(invStack.getItem()).toString();
                                boolean matches = invId.equals(q.target) || q.target.equals("any");

                                if (!matches) {
                                    if (q.target.equals("r3ct:mushrooms") && (invId.equals("minecraft:red_mushroom") || invId.equals("minecraft:brown_mushroom"))) matches = true;
                                    else if (q.target.equals("r3ct:sniffer_seeds") && (invId.equals("minecraft:torchflower_seeds") || invId.equals("minecraft:pitcher_pod"))) matches = true;
                                    else if (q.target.equals("r3ct:flowers") && invStack.is(net.minecraft.tags.ItemTags.FLOWERS)) matches = true;
                                    else if (q.target.equals("r3ct:leaves") && invStack.is(net.minecraft.tags.ItemTags.LEAVES)) matches = true;
                                    else if (q.target.equals("r3ct:raw_fishes") && invStack.is(net.minecraft.tags.ItemTags.FISHES)) matches = true;
                                    else if (q.target.equals("r3ct:eggs") && (invId.equals("minecraft:egg") || invId.equals("minecraft:brown_egg") || invId.equals("minecraft:blue_egg"))) matches = true;
                                    else if (q.target.equals("r3ct:froglights") && (invId.equals("minecraft:ochre_froglight") || invId.equals("minecraft:verdant_froglight") || invId.equals("minecraft:pearlescent_froglight"))) matches = true;
                                }

                                if (q.target.equals("r3ct:full_beehive") && (invId.equals("minecraft:beehive") || invId.equals("minecraft:bee_nest"))) {
                                    var beesData = invStack.get(net.minecraft.core.component.DataComponents.BEES);
                                    if (beesData != null && beesData.bees().size() >= 3) {
                                        matches = true;
                                    }
                                }

                                if (matches) count += invStack.getCount();
                            }
                        }
                        QuestManager.handleAction(player, "HAS_ITEMS", q.target, count);
                    }

                    if (q.actionType.equals("HAS_EFFECTS")) {
                        try {
                            int requiredEffects = Integer.parseInt(q.target);
                            if (player.getActiveEffects().size() >= requiredEffects) {
                                QuestManager.handleAction(player, "HAS_EFFECTS", q.target, 1);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (needsBiomeCheck) {
                String biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");

                QuestManager.handleAction(player, "VISIT_BIOME", biomeId, 1);
                QuestManager.handleAction(player, "TIME_IN_BIOME", biomeId, 1);

                if (biomeId.contains("frozen") || biomeId.contains("snowy") || biomeId.contains("ice") || biomeId.contains("grove") || biomeId.contains("slopes") || biomeId.contains("peaks")) {
                    QuestManager.handleAction(player, "VISIT_BIOME", "r3ct:frozen_biomes", 1);
                    QuestManager.handleAction(player, "TIME_IN_BIOME", "r3ct:frozen_biomes", 1);
                }
                if (biomeId.contains("ocean")) {
                    QuestManager.handleAction(player, "TIME_IN_BIOME", "ocean", 1);
                }
            }

            if (player.blockPosition().getY() < 0) {
                QuestManager.handleAction(player, "TIME_BELOW_Y0", "any", 1);
            } else if (player.blockPosition().getY() > 200) {
                QuestManager.handleAction(player, "ALTITUDE_HIGH", "any", 1);
            }

            if (player.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE)) {
                QuestManager.handleAction(player, "HERO_OF_THE_VILLAGE", "any", 1);
            }

            if (player.isInLava() && player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) {
                QuestManager.handleAction(player, "SWIM_LAVA_FIRE_RES", "any", 1);
            }

            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();
            var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            var structuresAt = level.structureManager().getAllStructuresAt(pos);

            for (var structure : structuresAt.keySet()) {
                var start = level.structureManager().getStructureWithPieceAt(pos, structure);
                if (start != null && start.isValid()) {
                    String structId = registry.getKey(structure).toString();
                    QuestManager.handleAction(player, "ENTER_STRUCTURE", structId, 1);
                }
            }
        }
    }
}