package net.mehvahdjukaar.sleep_tight.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class SimpleEffectData {

    public static final Codec<SimpleEffectData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Registry.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(e -> e.effect),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("level",0).forGetter(e -> e.level),
            ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(e -> e.duration)
    ).apply(instance, SimpleEffectData::new));

    private final MobEffect effect;
    private final int duration;
    private final int level;


    public SimpleEffectData(MobEffect effect, int baseDuration, int baseIntensity) {
        this.effect = effect;
        this.duration = baseDuration;
        this.level = baseIntensity;
    }

    //bed level from 0 to whatever
    public MobEffectInstance createInstance() {
        return new MobEffectInstance(effect, duration, level);
    }
}
