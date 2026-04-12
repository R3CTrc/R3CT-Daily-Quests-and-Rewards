package com.r3ct.quests.network;

import com.r3ct.quests.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public record OpenQuestsPayload(
        int questStreak,
        int totalQuestPoints,
        int dailyQuestsCompletedToday,
        List<String> activeQuests,
        List<Integer> questProgress,
        int streak,
        int perfectDaysCount,
        int availableFreezes,
        List<Boolean> questRewardsClaimed,
        List<Integer> claimedPointRewards
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenQuestsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_ID + ":open_quests"));

    public static final StreamCodec<FriendlyByteBuf, OpenQuestsPayload> CODEC = CustomPacketPayload.codec(
            OpenQuestsPayload::write,
            OpenQuestsPayload::new
    );

    public OpenQuestsPayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                readStringList(buf),
                readIntList(buf),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readBoolean),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(questStreak);
        buf.writeInt(totalQuestPoints);
        buf.writeInt(dailyQuestsCompletedToday);

        buf.writeInt(activeQuests.size());
        for (String s : activeQuests) buf.writeUtf(s);

        buf.writeInt(questProgress.size());
        for (int i : questProgress) buf.writeInt(i);

        buf.writeInt(streak);

        buf.writeInt(perfectDaysCount);
        buf.writeInt(availableFreezes);

        buf.writeCollection(questRewardsClaimed, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(claimedPointRewards, FriendlyByteBuf::writeInt);
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(buf.readUtf());
        return list;
    }

    private static List<Integer> readIntList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(buf.readInt());
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}