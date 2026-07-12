package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigSyncPayload(String questsJson, String rewardsJson, String serverJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_daily:config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(262144), ConfigSyncPayload::questsJson,
            ByteBufCodecs.stringUtf8(262144), ConfigSyncPayload::rewardsJson,
            ByteBufCodecs.stringUtf8(262144), ConfigSyncPayload::serverJson,
            ConfigSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}