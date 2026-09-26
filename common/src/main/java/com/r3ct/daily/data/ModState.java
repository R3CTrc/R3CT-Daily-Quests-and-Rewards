package com.r3ct.daily.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModState extends SavedData {
    public final Map<UUID, PlayerData> players = new HashMap<>();

    public static ModState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ModState::load, ModState::new, "r3ct_data");
    }

    public static PlayerData getPlayerData(MinecraftServer server, UUID uuid) {
        return get(server).players.computeIfAbsent(uuid, k -> new PlayerData());
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            playersNbt.put(uuid.toString(), data.toNbt());
        });
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static ModState load(CompoundTag nbt) {
        ModState state = new ModState();

        if (nbt.contains("players", 10)) {
            CompoundTag playersNbt = nbt.getCompound("players");

            for (String key : playersNbt.getAllKeys()) {
                if (playersNbt.contains(key, 10)) {
                    CompoundTag playerDataNbt = playersNbt.getCompound(key);
                    try {
                        state.players.put(UUID.fromString(key), PlayerData.fromNbt(playerDataNbt));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        return state;
    }
}