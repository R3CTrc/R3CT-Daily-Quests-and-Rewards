package com.r3ct.daily.platform;

import com.r3ct.daily.DailyNeoForgeClient;
import com.r3ct.daily.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

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
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    @Override
    public <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToPlayer(net.minecraft.server.level.ServerPlayer player, T payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToServer(T payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean isQuestKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return DailyNeoForgeClient.ClientModEvents.openQuestsKey != null &&
                    DailyNeoForgeClient.ClientModEvents.openQuestsKey.matches(keyEvent);
        }
        return false;
    }

    @Override
    public boolean isRewardKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return DailyNeoForgeClient.ClientModEvents.openRewardsKey != null &&
                    DailyNeoForgeClient.ClientModEvents.openRewardsKey.matches(keyEvent);
        }
        return false;
    }
}