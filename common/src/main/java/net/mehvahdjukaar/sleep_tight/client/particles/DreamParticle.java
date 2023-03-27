package net.mehvahdjukaar.sleep_tight.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.ParticleUtil;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DreamParticle extends TextureSheetParticle {

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

        if(PlatHelper.getPlatform().isFabric()){
            this.maxAlpha = (float) Math.max(0.2, ClientConfigs.PARTICLE_ALPHA.get());
            this.renderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }else {
            this.maxAlpha = (float) (double) ClientConfigs.PARTICLE_ALPHA.get();
            this.renderType = ParticleUtil.ADDITIVE_TRANSLUCENCY_RENDER_TYPE;
        }
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
        int alphaFadeTime = 40;
        if (this.age < alphaFadeTime) {
            this.alpha += maxAlpha / alphaFadeTime;
            this.alpha = Math.min(this.alpha, this.maxAlpha);
        }
        if (this.lifetime - this.age < alphaFadeTime) {
            this.alpha *= 0.95;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 pos = renderInfo.getPosition();

        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - pos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - pos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - pos.z());
        Quaternionf quaternion;
        if (this.roll == 0.0F) {
            quaternion = renderInfo.rotation();
        } else {
            quaternion = new Quaternionf(renderInfo.rotation());
            float i = Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.mul(Axis.ZP.rotation(i));
        }

        Vector3f[] vector3fs = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float size = this.getQuadSize(partialTicks);

        for (int k = 0; k < 4; ++k) {
            Vector3f vector3f2 = vector3fs[k];
            vector3f2.rotate(quaternion);
            vector3f2.mul(size);
            vector3f2.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        buffer.vertex(vector3fs[0].x(), vector3fs[0].y(), vector3fs[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(vector3fs[1].x(), vector3fs[1].y(), vector3fs[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(vector3fs[2].x(), vector3fs[2].y(), vector3fs[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(vector3fs[3].x(), vector3fs[3].y(), vector3fs[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
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
                p.maxAlpha*=2f;
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
