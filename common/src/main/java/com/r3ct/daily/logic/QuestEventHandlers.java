package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

public class QuestEventHandlers {

    public static void onDimensionChange(ServerPlayer player, String dimId) {
        if (player.connection == null) return;

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        PlayerData data = ModState.getPlayerData(server, player.getUUID());

        if (!data.unlockedDimensions.contains(dimId)) {
            data.unlockedDimensions.add(dimId);

            String translationKey = "r3ct_daily.dimension." + dimId.replace(':', '.');
            Component dimComp = Component.translatable(translationKey).withStyle(ChatFormatting.LIGHT_PURPLE);

            player.sendSystemMessage(Component.empty().append(QuestManager.getPrefix()).append(
                    Component.translatable("r3ct_daily.message.dimension_discovered", dimComp).withStyle(ChatFormatting.GREEN)
            ));

            server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        }
    }

    public static void onBlockBreak(ServerPlayer serverPlayer, BlockState state, BlockPos pos, Level level) {
        if (serverPlayer.connection == null) return;

        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (QuestManager.removePlacedBlock(pos, level)) {
            boolean isFullyGrownCrop = (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) ||
                    (state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3);

            if (!isFullyGrownCrop) {
                QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", blockId, -1);

                if (state.is(BlockItemTags.SAPLINGS.block())) {
                    QuestManager.handleAction(serverPlayer, "PLACE_SAPLING", blockId, -1);
                }
                if (state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock || state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof PitcherCropBlock) {
                    QuestManager.handleAction(serverPlayer, "PLACE_SEED", blockId, -1);
                }

                if (state.is(BlockTags.BEDS))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:beds", -1);
                if (state.is(BlockTags.WOOL))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:wool", -1);

                if (state.is(BlockItemTags.OAK_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:oak_logs", -1);
                else if (state.is(BlockItemTags.BIRCH_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:birch_logs", -1);
                else if (state.is(BlockItemTags.SPRUCE_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:spruce_logs", -1);
                else if (state.is(BlockItemTags.JUNGLE_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:jungle_logs", -1);
                else if (state.is(BlockItemTags.ACACIA_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:acacia_logs", -1);
                else if (state.is(BlockItemTags.DARK_OAK_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:dark_oak_logs", -1);
                else if (state.is(BlockItemTags.MANGROVE_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:mangrove_logs", -1);
                else if (state.is(BlockItemTags.CHERRY_LOGS.block()))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:cherry_logs", -1);
                else if (state.is(BlockTags.PALE_OAK_LOGS))
                    QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "r3ct_daily:pale_oak_logs", -1);

                return;
            }
        }

        QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", blockId, 1);

        if (state.is(BlockTags.BEDS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:beds", 1);
        if (state.is(BlockTags.WOOL)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:wool", 1);

        if (state.is(BlockItemTags.OAK_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:oak_logs", 1);
        else if (state.is(BlockItemTags.BIRCH_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:birch_logs", 1);
        else if (state.is(BlockItemTags.SPRUCE_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:spruce_logs", 1);
        else if (state.is(BlockTags.JUNGLE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:jungle_logs", 1);
        else if (state.is(BlockItemTags.ACACIA_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:acacia_logs", 1);
        else if (state.is(BlockItemTags.DARK_OAK_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:dark_oak_logs", 1);
        else if (state.is(BlockItemTags.MANGROVE_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:mangrove_logs", 1);
        else if (state.is(BlockItemTags.CHERRY_LOGS.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:cherry_logs", 1);
        else if (state.is(BlockTags.PALE_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:pale_oak_logs", 1);

        if (state.is(BlockItemTags.COAL_ORES.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:coal_ores", 1);
        if (state.is(BlockTags.IRON_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:iron_ores", 1);
        if (state.is(BlockTags.GOLD_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:gold_ores", 1);
        if (state.is(BlockTags.COPPER_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:copper_ores", 1);
        if (state.is(BlockItemTags.DIAMOND_ORES.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:diamond_ores", 1);
        if (state.is(BlockItemTags.LAPIS_ORES.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:lapis_ores", 1);
        if (state.is(BlockItemTags.REDSTONE_ORES.block())) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:redstone_ores", 1);
        if (blockId.equals("minecraft:nether_quartz_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:nether_quartz_ores", 1);
    }

    public static void onEntityDeath(ServerPlayer serverPlayer, Entity victim) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();

        QuestManager.handleAction(serverPlayer, "KILL_MOB", mobId, 1);

        if (serverPlayer.getHealth() <= 4.0f) {
            QuestManager.handleAction(serverPlayer, "KILL_LOW_HP", mobId, 1);
        }

        if (victim instanceof Pillager pillager) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                Raid raid = serverLevel.getRaids().getNearbyRaid(pillager.blockPosition(), 9216);
                if (raid == null) QuestManager.handleAction(serverPlayer, "KILL_MOB_NO_RAID", "minecraft:pillager", 1);
            }
        } else if (victim instanceof Ravager ravager) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                Raid raid = serverLevel.getRaids().getNearbyRaid(ravager.blockPosition(), 9216);
                if (raid != null) QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_RAID", "minecraft:ravager", 1);
            }
        }

        if (victim instanceof Monster) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel && serverLevel.isVillage(victim.blockPosition())) {
                QuestManager.handleAction(serverPlayer, "KILL_MOB_VILLAGE", mobId, 1);
            }
        }
    }

    public static void onItemEnchanted(ServerPlayer player, ItemStack item) {
        QuestManager.handleAction(player, "ENCHANT_ITEM", "any", 1);

        ItemEnchantments enchantments = item.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        enchantments.keySet().forEach(holder -> {
            holder.unwrapKey().ifPresent(key -> {
                String enchantId = key.identifier().toString();
                QuestManager.handleAction(player, "ENCHANT_WITH", enchantId, 1);
            });
        });
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player.connection == null) return;

        if (player.tickCount % 20 == 0) {
            MinecraftServer server = player.level().getServer();
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
                            ItemStack invStack = player.getInventory().getItem(i);
                            if (QuestManager.isItemMatchingTarget(invStack, q.target)) {
                                count += invStack.getCount();
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
                    QuestManager.handleAction(player, "VISIT_BIOME", "r3ct_daily:frozen_biomes", 1);
                    QuestManager.handleAction(player, "TIME_IN_BIOME", "r3ct_daily:frozen_biomes", 1);
                }
            }

            if (player.blockPosition().getY() < 0) {
                QuestManager.handleAction(player, "TIME_BELOW_Y0", "any", 1);
            } else if (player.blockPosition().getY() > 200) {
                QuestManager.handleAction(player, "ALTITUDE_HIGH", "any", 1);
            }

            if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                QuestManager.handleAction(player, "HERO_OF_THE_VILLAGE", "any", 1);
            }

            if (player.isInLava() && player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                QuestManager.handleAction(player, "SWIM_LAVA_FIRE_RES", "any", 1);
            }

            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
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