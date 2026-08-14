package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

//sleep cooldown kept on two clocks. day time is the one we actually want to use, but it can be frozen or
//moved back with /time, so game time is there as a fallback since it always goes up. whichever runs out first ends it
public class InsomniaCooldown {

    public static final Codec<InsomniaCooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day_deadline").forGetter(c -> c.dayDeadline),
            Codec.LONG.fieldOf("game_deadline").forGetter(c -> c.gameDeadline),
            Codec.LONG.fieldOf("last_known_day_time").forGetter(c -> c.lastKnownDayTime)
    ).apply(instance, InsomniaCooldown::new));

    public static final StreamCodec<ByteBuf, InsomniaCooldown> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, c -> c.dayDeadline,
            ByteBufCodecs.VAR_LONG, c -> c.gameDeadline,
            ByteBufCodecs.VAR_LONG, c -> c.lastKnownDayTime,
            InsomniaCooldown::new
    );

    private long dayDeadline;
    private long gameDeadline;
    private long lastKnownDayTime; //day time last observed when the cooldown was set; used to detect rewinds

    public InsomniaCooldown() {
    }

    public InsomniaCooldown(long dayDeadline, long gameDeadline, long lastKnownDayTime) {
        this.dayDeadline = dayDeadline;
        this.gameDeadline = gameDeadline;
        this.lastKnownDayTime = lastKnownDayTime;
    }

    public void set(long dayTimeNow, long gameTimeNow, long duration) {
        this.dayDeadline = dayTimeNow + duration;
        this.gameDeadline = gameTimeNow + duration;
        this.lastKnownDayTime = dayTimeNow;
    }

    //ticks left, <= 0 means it's over
    public long remaining(Player player) {
        long dayRemaining = dayDeadline - player.level().getDayTime();
        long gameRemaining = gameDeadline - player.level().getGameTime();
        return Math.min(dayRemaining, gameRemaining);
    }

    public long dayDeadline() {
        return dayDeadline;
    }

    //clears the cooldown and tells the player when day time went backwards. returns true if that happened,
    //so callers know they have to resync. a frozen daylight cycle isn't a rewind, game time handles that one
    public boolean tickRewind(ServerPlayer player) {
        long dayTime = player.level().getDayTime();
        if (dayTime < lastKnownDayTime) {
            if (!player.getAbilities().instabuild && remaining(player) > 0) {
                player.displayClientMessage(Component.translatable("message.sleep_tight.time_skipped"), false);
            }
            //reset so the two clocks agree again
            this.set(dayTime, player.level().getGameTime(), 0);
            return true;
        }
        return false;
    }
}
