package com.r3ct.quests.platform;

import com.r3ct.quests.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

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
    public java.nio.file.Path getConfigDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToPlayer(net.minecraft.server.level.ServerPlayer player, T payload) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }

    @Override
    public <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToServer(T payload) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
    }

    @Override
    public boolean isQuestKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return com.r3ct.quests.R3CTClient.openQuestsKey != null && com.r3ct.quests.R3CTClient.openQuestsKey.matches(keyEvent);
        }
        return false;
    }

    @Override
    public boolean isRewardKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return com.r3ct.quests.R3CTClient.openRewardsKey != null && com.r3ct.quests.R3CTClient.openRewardsKey.matches(keyEvent);
        }
        return false;
    }
}
