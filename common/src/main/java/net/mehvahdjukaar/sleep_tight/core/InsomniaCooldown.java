package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

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

    //someone set the time backwards. clears the cooldown and tells the player. returns true if that happened
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
