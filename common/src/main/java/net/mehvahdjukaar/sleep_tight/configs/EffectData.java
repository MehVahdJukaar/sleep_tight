package net.mehvahdjukaar.sleep_tight.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class EffectData {

    public static final Codec<EffectData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(e -> e.effect),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("base_intensity",0).forGetter(e -> e.baseIntensity),
            Codec.FLOAT.fieldOf("intensity_per_level").forGetter(e -> e.intensityPerLevel),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("base_duration").forGetter(e -> e.baseDuration),
            Codec.FLOAT.fieldOf("duration_per_level").forGetter(e -> e.durationPerLevel)
    ).apply(instance, EffectData::new));

    private final MobEffect effect;
    private final float intensityPerLevel;
    private final int baseDuration;
    private final float durationPerLevel;
    private final int baseIntensity;


    public EffectData(MobEffect effect, int baseIntensity, float intensityPerLevel, int baseDuration, float durationPerLevel) {
        this.effect = effect;
        this.baseIntensity = baseIntensity;
        this.intensityPerLevel = intensityPerLevel;
        this.baseDuration = baseDuration;
        this.durationPerLevel = durationPerLevel;
    }

    //bed level from 0 to whatever
    public MobEffectInstance createInstance(int bedLevel) {
        return new MobEffectInstance(effect, (int) (baseDuration + (bedLevel * durationPerLevel)),
                (int) (baseIntensity + (intensityPerLevel * bedLevel)), false, false);
    }
}
