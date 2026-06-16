package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A sleep-cooldown deadline expressed against two clocks at once, plus the day-clock rewind detection that
 * guards it.
 * <p>
 * {@code dayDeadline} is measured against {@link net.minecraft.world.level.Level#getDayTime()} and gives the
 * intended sleep-cycle behaviour and tuning. {@code gameDeadline} is measured against the monotonic
 * {@link net.minecraft.world.level.Level#getGameTime()} and acts as a backstop: day time can be frozen
 * (daylight cycle off) or rewound (/time set), but game time always advances while the level ticks, so the
 * cooldown is guaranteed to elapse. The cooldown is over as soon as EITHER clock passes its deadline.
 * <p>
 * {@code lastKnownDayTime} is the day time observed when the cooldown was last (re)started. It lets
 * {@link #tickRewind} tell a true day-time <em>rewind</em> (day time set to an earlier value) from a daylight
 * cycle <em>freeze</em>: a freeze leaves day time equal to what we last saw and is handled silently by the
 * game-time backstop, while a rewind drops below it and is surfaced to the player.
 */
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

    /** Ticks left before the cooldown elapses; <= 0 means it is over. */
    public long remaining(Player player) {
        long dayRemaining = dayDeadline - player.level().getDayTime();
        long gameRemaining = gameDeadline - player.level().getGameTime();
        return Math.min(dayRemaining, gameRemaining);
    }

    /** Deadline against the day clock; basis for the on-screen cooldown bar. */
    public long dayDeadline() {
        return dayDeadline;
    }

    /**
     * Detects a day-time rewind (world day time set to an earlier value than last observed) and, if so,
     * notifies the player and clears the cooldown. A daylight-cycle freeze leaves day time at the last seen
     * value and is NOT treated as a rewind — the game-time backstop ends such cooldowns silently.
     *
     * @return true if a rewind was detected and handled (the caller should reset dependent state and resync)
     */
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
