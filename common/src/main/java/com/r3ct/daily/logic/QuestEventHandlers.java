package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class QuestEventHandlers {

    public static void onDimensionChange(ServerPlayer player, String dimId) {
        QuestManager.handleAction(player, "CHANGE_DIMENSION", dimId);
        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());

        if (!data.unlockedDimensions.contains(dimId)) {
            data.unlockedDimensions.add(dimId);
            String dimName = dimId.contains("nether") ? "Nether" : (dimId.contains("end") ? "End" : dimId);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("r3ct.message.dimension_discovered", "§d" + dimName));
            player.level().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(ModState.TYPE).setDirty();
        }

        if (data.unlockedDimensions.contains("minecraft:overworld") &&
                data.unlockedDimensions.contains("minecraft:the_nether") &&
                data.unlockedDimensions.contains("minecraft:the_end")) {
            QuestManager.grantAdvancement(player, "r3ct_daily:quests/dimension_master");
        }
    }

    public static void onBlockBreak(ServerPlayer serverPlayer, BlockState state, BlockPos pos, Level level) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (QuestManager.removePlacedBlock(pos, level)) {
            QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", blockId, -1);
            QuestManager.handleAction(serverPlayer, "PLACE_BLOCK", "any", -1);
            QuestManager.handleAction(serverPlayer, "PLACE_SAPLING", "any", -1);
            QuestManager.handleAction(serverPlayer, "PLACE_SEED", "any", -1);
            return;
        }

        QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", blockId);

        if (state.is(BlockTags.LEAVES)) QuestManager.handleAction(serverPlayer, "BREAK_LEAVES", "any", 1);
        if (state.is(BlockTags.JUNGLE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:jungle_logs", 1);
        if (state.is(BlockTags.PALE_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:pale_oak_logs", 1);
        if (blockId.contains("lapis_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:lapis_ores", 1);
        if (blockId.equals("minecraft:obsidian")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:obsidian", 1);
        if (blockId.equals("minecraft:nether_quartz_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:nether_quartz_ores", 1);
        if (blockId.equals("minecraft:cobweb")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "minecraft:cobweb", 1);
        if (blockId.contains("diamond_ore")) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:diamond_ores", 1);
        if (state.is(BlockTags.GOLD_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:gold_ores", 1);
        if (state.is(BlockTags.FLOWERS)) QuestManager.handleAction(serverPlayer, "BREAK_FLOWER", "any", 1);
        if (state.is(BlockTags.OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:oak_logs", 1);
        if (state.is(BlockTags.BIRCH_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:birch_logs", 1);
        if (state.is(BlockTags.IRON_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:iron_ores", 1);
        if (state.is(BlockTags.COPPER_ORES)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:copper_ores", 1);
        if (state.is(BlockTags.ACACIA_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:acacia_logs", 1);
        if (state.is(BlockTags.DARK_OAK_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:dark_oak_logs", 1);
        if (state.is(BlockTags.MANGROVE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:mangrove_logs", 1);
        if (state.is(BlockTags.SPRUCE_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:spruce_logs", 1);
        if (state.is(BlockTags.CHERRY_LOGS)) QuestManager.handleAction(serverPlayer, "BREAK_BLOCK", "r3ct_daily:cherry_logs", 1);

        if (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity beehive) {
                if (!beehive.isEmpty()) {
                    ItemEnchantments enchantments = serverPlayer.getMainHandItem().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    boolean hasSilkTouch = enchantments.keySet().stream().anyMatch(ench -> ench.is(Enchantments.SILK_TOUCH));
                    if (hasSilkTouch) {
                        QuestManager.handleAction(serverPlayer, "SILK_TOUCH_BEE_NEST", "any", 1);
                    }
                }
            }
        }
    }

    public static void onEntityDeath(ServerPlayer serverPlayer, Entity victim) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        QuestManager.handleAction(serverPlayer, "KILL_MOB", mobId);

        if (serverPlayer.getHealth() <= 4.0f) QuestManager.handleAction(serverPlayer, "KILL_LOW_HP", "any", 1);

        if (mobId.equals("minecraft:skeleton") && serverPlayer.level().dimension().identifier().toString().equals("minecraft:the_nether")) {
            QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_NETHER", "minecraft:skeleton", 1);
        }
        if (mobId.equals("minecraft:cave_spider")) QuestManager.handleAction(serverPlayer, "KILL_MOB", "minecraft:spider", 1);

        if (victim instanceof net.minecraft.world.entity.monster.illager.Pillager pillager) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(pillager.blockPosition(), 9216);
                if (raid == null) QuestManager.handleAction(serverPlayer, "KILL_MOB_NO_RAID", "minecraft:pillager", 1);
            }
        } else if (victim instanceof net.minecraft.world.entity.monster.Ravager ravagerEntity) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.raid.Raid raid = serverLevel.getRaids().getNearbyRaid(ravagerEntity.blockPosition(), 9216);
                if (raid != null) QuestManager.handleAction(serverPlayer, "KILL_MOB_IN_RAID", "minecraft:ravager", 1);
            }
        }
        if (victim instanceof net.minecraft.world.entity.monster.Monster) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                if (serverLevel.isVillage(victim.blockPosition())) {
                    QuestManager.handleAction(serverPlayer, "KILL_MOB_VILLAGE", "any", 1);
                }
            }
        }
    }

    public static void onPlayerWakeUp(ServerPlayer serverPlayer, BlockPos blockPos) {
        QuestManager.handleAction(serverPlayer, "SLEEP", "any", 1);
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.isVillage(blockPos != null ? blockPos : serverPlayer.blockPosition())) {
                QuestManager.handleAction(serverPlayer, "SLEEP_IN_VILLAGE", "any", 1);
            }
        }
    }

    public static void onEntityInteract(ServerPlayer serverPlayer, InteractionHand hand, Entity target) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack stack = serverPlayer.getItemInHand(hand);
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();

            if (mobId.equals("minecraft:cow") && stack.is(Items.BUCKET)) {
                if (target instanceof net.minecraft.world.entity.animal.cow.Cow cow && !cow.isBaby()) {
                    QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:cow_milk", 1);
                }
            }
            if (mobId.equals("minecraft:armadillo") && stack.is(Items.BRUSH)) {
                QuestManager.handleAction(serverPlayer, "BRUSH_ARMADILLO", "any", 1);
            }
            if (mobId.equals("minecraft:turtle") && stack.is(Items.SEAGRASS)) {
                QuestManager.handleAction(serverPlayer, "BREED_TURTLE", "any", 1);
            }
            if (mobId.equals("minecraft:sheep") && stack.is(Items.SHEARS)) {
                if (target instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) {
                    if (!sheep.isBaby() && !sheep.isSheared()) {
                        QuestManager.handleAction(serverPlayer, "INTERACT_ENTITY", "minecraft:sheep_shear", 1);
                    }
                }
            }
            if (mobId.equals("minecraft:tropical_fish") && stack.is(Items.WATER_BUCKET)) {
                if (target instanceof net.minecraft.world.entity.animal.fish.TropicalFish fish && !fish.fromBucket()) {
                    QuestManager.handleAction(serverPlayer, "CATCH_FISH_BUCKET", "minecraft:tropical_fish", 1);
                }
            }
        }
    }

    public static void onBlockInteract(ServerPlayer serverPlayer, InteractionHand hand, BlockPos pos, Direction face, BlockState targetState) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack stack = serverPlayer.getItemInHand(hand);
            Level level = serverPlayer.level();

            if (stack.is(Items.BONE_MEAL)) QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:bone_meal", 1);
            if (stack.is(Items.FLINT_AND_STEEL)) {
                BlockPos firePos = pos.relative(face);
                if (level.getBlockState(firePos).isAir() || targetState.is(Blocks.TNT) || targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
                    QuestManager.handleAction(serverPlayer, "USE_ITEM_ON_BLOCK", "minecraft:flint_and_steel", 1);
                }
            }
            if (targetState.is(Blocks.RESPAWN_ANCHOR) && stack.is(Items.GLOWSTONE)) {
                int charges = targetState.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE);
                if (charges < 4) QuestManager.handleAction(serverPlayer, "CHARGE_RESPAWN_ANCHOR", "any", 1);
            }
            if (targetState.is(Blocks.BELL)) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:bell", 1);
            if (targetState.is(Blocks.STONECUTTER)) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:stonecutter", 1);
            if (targetState.is(Blocks.CAMPFIRE) || targetState.is(Blocks.SOUL_CAMPFIRE)) {
                if (stack.get(DataComponents.FOOD) != null) {
                    if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
                        boolean hasSpace = false;
                        for (ItemStack item : campfire.getItems()) {
                            if (item.isEmpty()) { hasSpace = true; break; }
                        }
                        if (hasSpace) QuestManager.handleAction(serverPlayer, "USE_BLOCK", "minecraft:campfire", 1);
                    }
                }
            }
            if (targetState.is(Blocks.COMPOSTER)) {
                if (targetState.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) == 8) {
                    QuestManager.handleAction(serverPlayer, "EMPTY_COMPOSTER", "any", 1);
                }
            }
            if (targetState.is(Blocks.TNT) && stack.is(Items.FLINT_AND_STEEL)) QuestManager.handleAction(serverPlayer, "IGNITE_TNT", "any", 1);
            if ((targetState.is(Blocks.BEE_NEST) || targetState.is(Blocks.BEEHIVE)) && stack.is(Items.GLASS_BOTTLE)) {
                if (targetState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 5) {
                    QuestManager.handleAction(serverPlayer, "COLLECT_HONEY", "any", 1);
                }
            }
        }
    }

    public static void onEntityLoad(Entity entity, Level level) {
        if (entity.tickCount == 0) {
            if (entity instanceof net.minecraft.world.entity.animal.golem.CopperGolem) {
                Player nearest = level.getNearestPlayer(entity, 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "copper", 1);
            }
            if (entity instanceof net.minecraft.world.entity.animal.golem.IronGolem) {
                Player nearest = level.getNearestPlayer(entity, 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "iron", 1);
            }
            if (entity instanceof net.minecraft.world.entity.animal.golem.SnowGolem) {
                Player nearest = level.getNearestPlayer(entity, 10.0D);
                if (nearest instanceof ServerPlayer sp) QuestManager.handleAction(sp, "BUILD_GOLEM", "snow", 1);
            }
        }
    }
}