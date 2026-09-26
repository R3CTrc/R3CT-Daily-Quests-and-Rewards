package com.r3ct.daily.platform;

import com.r3ct.daily.DailyNeoForgeClient;
import com.r3ct.daily.platform.services.IPlatformHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public <T extends CustomPacketPayload> void sendToServer(T payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean isQuestKey(Object event) {
        if (event instanceof Integer keyCode) {
            return DailyNeoForgeClient.ClientModEvents.openQuestsKey != null &&
                    DailyNeoForgeClient.ClientModEvents.openQuestsKey.matches(keyCode, -1);
        }
        return false;
    }

    @Override
    public boolean isRewardKey(Object event) {
        if (event instanceof Integer keyCode) {
            return DailyNeoForgeClient.ClientModEvents.openRewardsKey != null &&
                    DailyNeoForgeClient.ClientModEvents.openRewardsKey.matches(keyCode, -1);
        }
        return false;
    }
}