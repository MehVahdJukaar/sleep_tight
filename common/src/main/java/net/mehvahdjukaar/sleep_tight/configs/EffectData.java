package net.mehvahdjukaar.sleep_tight.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

public class EffectData {

    // by id since configs get built before data registries exist
    public static final Codec<EffectData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(e -> e.effectId),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("base_intensity",0).forGetter(e -> e.baseIntensity),
            Codec.FLOAT.fieldOf("intensity_per_level").forGetter(e -> e.intensityPerLevel),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("base_duration").forGetter(e -> e.baseDuration),
            Codec.FLOAT.fieldOf("duration_per_level").forGetter(e -> e.durationPerLevel)
    ).apply(instance, EffectData::new));

    private final ResourceLocation effectId;
    private final float intensityPerLevel;
    private final int baseDuration;
    private final float durationPerLevel;
    private final int baseIntensity;


    public EffectData(Holder<MobEffect> effect, int baseIntensity, float intensityPerLevel, int baseDuration, float durationPerLevel) {
        this(effect.unwrapKey().orElseThrow().location(), baseIntensity, intensityPerLevel, baseDuration, durationPerLevel);
    }

    private EffectData(ResourceLocation effectId, int baseIntensity, float intensityPerLevel, int baseDuration, float durationPerLevel) {
        this.effectId = effectId;
        this.baseIntensity = baseIntensity;
        this.intensityPerLevel = intensityPerLevel;
        this.baseDuration = baseDuration;
        this.durationPerLevel = durationPerLevel;
    }

    //bed level from 0 to whatever
    @Nullable
    public MobEffectInstance createInstance(int bedLevel) {
        var effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
        if (effect == null) return null;
        return new MobEffectInstance(effect, (int) (baseDuration + (bedLevel * durationPerLevel)),
                (int) (baseIntensity + (intensityPerLevel * bedLevel)), false, false);
    }
}
