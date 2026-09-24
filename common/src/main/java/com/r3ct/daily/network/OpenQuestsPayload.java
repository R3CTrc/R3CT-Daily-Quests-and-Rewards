package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
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
        List<Integer> claimedPointRewards,
        boolean enableQuestRerolling,
        int rerollCostEasy,
        int rerollCostMedium,
        int rerollCostHard,
        int xpDailyReward,
        int xpPerQuestEasy,
        int xpPerQuestMedium,
        int xpPerQuestHard,
        int perfectDaysForShield,
        int maxStoredQuestShields,
        float questStreakXpMultiplier
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenQuestsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_ID + ":open_quests"));

    public static final StreamCodec<FriendlyByteBuf, OpenQuestsPayload> CODEC = StreamCodec.ofMember(
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
                readBoolList(buf),
                readIntList(buf),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readFloat()
        );
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf());
        return list;
    }

    private static List<Integer> readIntList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readInt());
        return list;
    }

    private static List<Boolean> readBoolList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Boolean> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readBoolean());
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(questStreak);
        buf.writeInt(totalQuestPoints);
        buf.writeInt(dailyQuestsCompletedToday);

        buf.writeVarInt(activeQuests.size());
        for (String s : activeQuests) buf.writeUtf(s);

        buf.writeVarInt(questProgress.size());
        for (int i : questProgress) buf.writeInt(i);

        buf.writeInt(streak);
        buf.writeInt(perfectDaysCount);
        buf.writeInt(availableFreezes);

        buf.writeVarInt(questRewardsClaimed.size());
        for (boolean b : questRewardsClaimed) buf.writeBoolean(b);

        buf.writeVarInt(claimedPointRewards.size());
        for (int i : claimedPointRewards) buf.writeInt(i);

        buf.writeBoolean(enableQuestRerolling);
        buf.writeInt(rerollCostEasy);
        buf.writeInt(rerollCostMedium);
        buf.writeInt(rerollCostHard);
        buf.writeInt(xpDailyReward);
        buf.writeInt(xpPerQuestEasy);
        buf.writeInt(xpPerQuestMedium);
        buf.writeInt(xpPerQuestHard);
        buf.writeInt(perfectDaysForShield);
        buf.writeInt(maxStoredQuestShields);
        buf.writeFloat(questStreakXpMultiplier);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}