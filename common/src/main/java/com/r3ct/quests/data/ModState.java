package com.r3ct.quests.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModState extends SavedData {
    public final Map<UUID, PlayerData> players = new HashMap<>();

    public static ModState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PlayerData getPlayerData(MinecraftServer server, UUID uuid) {
        return get(server).players.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            playersNbt.put(uuid.toString(), data.toNbt());
        });
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static ModState load(CompoundTag nbt, HolderLookup.Provider registries) {
        ModState state = new ModState();

        nbt.getCompound("players").ifPresent(playersNbt -> {

            for (String key : playersNbt.keySet()) {

                playersNbt.getCompound(key).ifPresent(playerDataNbt -> {
                    try {
                        state.players.put(UUID.fromString(key), PlayerData.fromNbt(playerDataNbt));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        });

        return state;
    }

    public static final Codec<ModState> CODEC = CompoundTag.CODEC.xmap(
            nbt -> load(nbt, null),
            state -> state.save(new CompoundTag(), null)
    );

    public static final SavedDataType<ModState> TYPE = new SavedDataType<>(
            Identifier.parse("r3ct_data"),
            ModState::new,
            CODEC,
            DataFixTypes.LEVEL
    );
}