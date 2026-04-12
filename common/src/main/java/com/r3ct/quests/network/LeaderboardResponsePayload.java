package com.r3ct.quests.network;

import com.r3ct.quests.data.TopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public record LeaderboardResponsePayload(int boardType, List<TopEntry> leftList, List<TopEntry> rightList) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeaderboardResponsePayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("r3ct:leaderboard_res"));

    public static final StreamCodec<FriendlyByteBuf, LeaderboardResponsePayload> CODEC = CustomPacketPayload.codec(
            LeaderboardResponsePayload::write,
            LeaderboardResponsePayload::new
    );

    public LeaderboardResponsePayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readCollection(ArrayList::new, TopEntry::read), buf.readCollection(ArrayList::new, TopEntry::read));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(boardType);
        buf.writeCollection(leftList, TopEntry::write);
        buf.writeCollection(rightList, TopEntry::write);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}