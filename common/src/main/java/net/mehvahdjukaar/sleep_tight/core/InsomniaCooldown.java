package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Sleep-cooldown deadline tracked against two clocks: {@code dayDeadline} on day time (the intended
 * behaviour) and {@code gameDeadline} on the monotonic game time as a backstop, since day time can be
 * frozen or rewound while game time always advances. The cooldown ends when either clock passes its
 * deadline. {@code lastKnownDayTime} lets {@link #tickRewind} tell a real rewind (surfaced to the player)
 * from a daylight-cycle freeze (ended silently by the backstop).
 */
public class InsomniaCooldown {

    public static final Codec<InsomniaCooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day_deadline").forGetter(c -> c.dayDeadline),
            Codec.LONG.fieldOf("game_deadline").forGetter(c -> c.gameDeadline),
            Codec.LONG.fieldOf("last_known_day_time").forGetter(c -> c.lastKnownDayTime)
    ).apply(instance, InsomniaCooldown::new));

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

    public long dayDeadline() {
        return dayDeadline;
    }

    public long gameDeadline() {
        return gameDeadline;
    }

    public long lastKnownDayTime() {
        return lastKnownDayTime;
    }

    /**
     * Detects a day-time rewind (day time set to an earlier value than last observed); notifies the player
     * and clears the cooldown. A daylight-cycle freeze is not a rewind: the game-time backstop ends those
     * cooldowns silently.
     *
     * @return true if a rewind was handled (caller should reset dependent state and resync)
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
