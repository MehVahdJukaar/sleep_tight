package net.mehvahdjukaar.sleep_tight.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.moonlight.core.client.MLRenderTypes;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DreamParticle extends TextureSheetParticle {

    private static final int FADE_START = 40;

    private float maxAlpha;
    private final float deltaRot;

    private final ParticleRenderType renderType;

    protected DreamParticle(ClientLevel clientLevel, double x, double y, double z, double vx, double vy, double vz) {
        super(clientLevel, x, y, z, vx, vy, vz);
        //float g = 0.4f + ((System.currentTimeMillis()%1000))/1000f * 0.25f;
        float g = 0.4f + this.random.nextFloat() * 0.25f;
        //g = this.random.nextFloat();
        // g = System.currentTimeMillis()/10f;
        this.rCol = Math.max(0.0F, Mth.sin((g + 0.0F) * 6.2831855F) * 0.65F + 0.35F);
        this.gCol = Math.max(0.0F, Mth.sin((g + 0.33333334F) * 6.2831855F) * 0.65F + 0.35F);
        this.bCol = Math.max(0.0F, Mth.sin((g + 0.6666667F) * 6.2831855F) * 0.65F + 0.35F);

        int l = ClientConfigs.PARTICLE_LIFETIME.get();
        this.lifetime = l + (int) MthUtils.nextWeighted(this.random, l * 0.6f, 1);
        this.alpha = 0.01f;
        this.deltaRot = (0.002f + MthUtils.nextWeighted(this.random, 0.05f, 10)) * (this.random.nextBoolean() ? -1 : 1);
        this.quadSize = 0.04f + MthUtils.nextWeighted(this.random, 0.08f, 200);
        this.roll = (float) (Math.PI * this.random.nextFloat());
        this.friction = 0.995f;
        this.gravity = 0;
        this.xd *= 0.005;
        this.xd += vx;
        this.zd *= 0.005;
        this.zd += vz;

        this.yd = vy;

        this.setSize(0.1f, 0.1f);

        this.maxAlpha = (float) (double) ClientConfigs.PARTICLE_ALPHA.get();
        this.renderType = MLRenderTypes.PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return renderType;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.deltaRot;
        int timeLeft = lifetime - age;
        if (this.age < FADE_START) {
            this.alpha += maxAlpha / FADE_START;
            this.alpha = Math.min(this.alpha, this.maxAlpha);
        } else if (timeLeft < FADE_START) {
            alpha = (maxAlpha * timeLeft) / FADE_START;
        }
    }


    @Override
    protected int getLightColor(float partialTick) {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (!this.level.hasChunkAt(pos)) return 0;
        int i = level.getBrightness(LightLayer.SKY, pos);
        int j = level.getBrightness(LightLayer.BLOCK, pos);
        if (j < 10) j = 10;
        return i << 20 | j << 4;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprite;

        public Factory(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType pType, ClientLevel level, double pX, double pY, double pZ,
                                       double period, double unused, double mode) {

            DreamParticle p;
            //floaty particles
            if (mode == 1) {
                float h = (float) period;
                float ampl = 0.001f;
                float dx = (ampl * Mth.sin(6.2831855F * h));
                float dz = (ampl * Mth.cos(6.2831855F * h));
                float vy = 0.003f + MthUtils.nextWeighted(level.random, 0.004f, 10);
                p = new DreamParticle(level, pX, pY, pZ, dx, vy, dz);

            } else if (mode == 2) {
                float yaw = level.random.nextFloat() * 2 * Mth.PI;
                float pitch = Mth.randomBetween(level.random, -0.1f, 0.5f) * Mth.PI;
                float len = 0.4f + level.random.nextFloat() * 0.3f;
                Vec3 v = new Vec3(0, 0, len).xRot(pitch).yRot(yaw);
                p = new DreamParticle(level, pX + v.x * 0.5, pY + v.y * 0.5, pZ + v.z * 0.5, v.x, v.y * 0.75, v.z);
                p.friction = 0.78f;
                p.maxAlpha *= 2f;
                p.alpha = p.maxAlpha;
                p.setLifetime(60 + level.random.nextInt(180));
            } else {
                p = new DreamParticle(level, pX, pY, pZ, period, unused, mode);
            }

            p.setSpriteFromAge(sprite);
            return p;
        }
    }

}
