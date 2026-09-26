package com.r3ct.daily.network;

import com.r3ct.daily.data.TopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record LeaderboardResponsePayload(int boardType, List<TopEntry> leftList, List<TopEntry> rightList) {
    public static final ResourceLocation ID = new ResourceLocation("r3ct_daily", "leaderboard_res");

    public LeaderboardResponsePayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readCollection(ArrayList::new, TopEntry::read),
                buf.readCollection(ArrayList::new, TopEntry::read)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.boardType);
        buf.writeCollection(this.leftList, TopEntry::write);
        buf.writeCollection(this.rightList, TopEntry::write);
    }
}