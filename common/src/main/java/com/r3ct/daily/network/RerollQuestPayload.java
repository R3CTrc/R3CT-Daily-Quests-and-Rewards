package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RerollQuestPayload(int questIndex) {
    public static final ResourceLocation ID = new ResourceLocation("r3ct_daily", "reroll_quest");

    public RerollQuestPayload(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.questIndex);
    }
}