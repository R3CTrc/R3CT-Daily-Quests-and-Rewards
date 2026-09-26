package com.r3ct.daily.platform;

import com.r3ct.daily.DailyFabricClient;
import com.r3ct.daily.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public <T extends CustomPacketPayload> void sendToServer(T payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public boolean isQuestKey(Object event) {
        if (event instanceof Integer keyCode) {
            return DailyFabricClient.openQuestsKey != null && DailyFabricClient.openQuestsKey.matches(keyCode, -1);
        }
        return false;
    }

    @Override
    public boolean isRewardKey(Object event) {
        if (event instanceof Integer keyCode) {
            return DailyFabricClient.openRewardsKey != null && DailyFabricClient.openRewardsKey.matches(keyCode, -1);
        }
        return false;
    }
}