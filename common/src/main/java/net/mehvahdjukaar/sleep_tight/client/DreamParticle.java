package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.moonlight.api.client.util.ParticleUtil;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DreamParticle extends TextureSheetParticle {

    private final double maxAlpha;
    private final float deltaRot;

    protected DreamParticle(ClientLevel clientLevel, double x, double y, double z, double vx, double vy, double vz) {
        super(clientLevel, x, y, z, vx, vy, vz);
        float g = 0.3f+this.random.nextFloat()*0.35f;
        this.rCol = Math.max(0.0F, Mth.sin((g + 0.0F) * 6.2831855F) * 0.65F + 0.35F);
        this.gCol = Math.max(0.0F, Mth.sin((g + 0.33333334F) * 6.2831855F) * 0.65F + 0.35F);
        this.bCol = Math.max(0.0F, Mth.sin((g + 0.6666667F) * 6.2831855F) * 0.65F + 0.35F);

        int l = ClientConfigs.PARTICLE_LIFETIME.get();
        this.lifetime = l + (int) MthUtils.nextWeighted(this.random, l*0.6f, 1);
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

        this.maxAlpha = ClientConfigs.PARTICLE_ALPHA.get()*7;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleUtil.ADDITIVE_TRANSLUCENCY_RENDER_TYPE;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.deltaRot;
        int alphaFadeTime = 40;
        if (this.age < alphaFadeTime) {
            this.alpha += maxAlpha / alphaFadeTime;
            this.alpha = Math.min(this.alpha, 1);
        } else if (this.lifetime - this.age < alphaFadeTime) {
            this.alpha *=0.95;
        }

    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 pos = renderInfo.getPosition();

        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - pos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - pos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - pos.z());
        Quaternion quaternion;
        if (this.roll == 0.0F) {
            quaternion = renderInfo.rotation();
        } else {
            quaternion = new Quaternion(renderInfo.rotation());
            float i = Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.mul(Vector3f.ZP.rotation(i));
        }

        Vector3f[] vector3fs = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float size = this.getQuadSize(partialTicks);

        for (int k = 0; k < 4; ++k) {
            Vector3f vector3f2 = vector3fs[k];
            vector3f2.transform(quaternion);
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
        buffer.vertex(vector3fs[3].x(), vector3fs[3].y(), vector3fs[3].z()).uv(u0, v1).color(1, 1, 1, this.alpha).uv2(light).endVertex();

    }

    @Override
    protected int getLightColor(float partialTick) {
        BlockPos pos = new BlockPos(this.x, this.y, this.z);
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
        public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ,
                                       double pXSpeed, double pYSpeed, double pZSpeed) {
            var p = new DreamParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            p.setSpriteFromAge(sprite);

            return p;
        }
    }

}
