package net.mehvahdjukaar.sleep_tight.common.blocks;

import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.core.WakeReason;

public interface ISleepTightBed {

    default boolean st_canSpawnBedbugs() {
        return true;
    }

    default boolean st_canCauseNightmares(){
        return CommonConfigs.NIGHTMARES_BED.get();
    }

    default long st_getCooldown() {
        return CommonConfigs.BED_COOLDOWN.get();
    }

    default boolean st_hasPenalties(){
        return CommonConfigs.PENALTIES_BED.get();
    }

    default boolean st_hasRequirements(){
        return CommonConfigs.REQUIREMENT_BED.get();
    }

    default long st_modifyWakeUpTime(WakeReason reason, long newTime, long dayTime) {
        if (reason == WakeReason.ENCOUNTER || reason == WakeReason.NIGHTMARE) {
            double mult = reason == WakeReason.ENCOUNTER ? CommonConfigs.ENCOUNTER_SLEEP_TIME_MULTIPLIER.get() :
                    CommonConfigs.NIGHTMARE_SLEEP_TIME_MULTIPLIER.get();

            return (dayTime + (long) ((newTime - dayTime) * mult));
        }
        return newTime;
    }

}
