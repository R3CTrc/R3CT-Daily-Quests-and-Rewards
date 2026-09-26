package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigSyncPayload(String questsJson, String rewardsJson, String serverJson) {
    public static final ResourceLocation ID = new ResourceLocation("r3ct_daily", "config_sync");

    public ConfigSyncPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(262144),
                buf.readUtf(262144),
                buf.readUtf(262144)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.questsJson, 262144);
        buf.writeUtf(this.rewardsJson, 262144);
        buf.writeUtf(this.serverJson, 262144);
    }
}