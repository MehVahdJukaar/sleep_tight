package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;

public class DreamParticle extends TextureSheetParticle {

    private final float deltaRot;

    protected DreamParticle(ClientLevel clientLevel, double x, double y, double z, double vx, double vy, double vz) {
        super(clientLevel, x, y, z, vx, vy, vz );
        float g = this.random.nextFloat();
        this.rCol = Math.max(0.0F, Mth.sin((g + 0.0F) * 6.2831855F) * 0.65F + 0.35F);
        this.gCol = Math.max(0.0F, Mth.sin((g + 0.33333334F) * 6.2831855F) * 0.65F + 0.35F);
        this.bCol = Math.max(0.0F, Mth.sin((g + 0.6666667F) * 6.2831855F) * 0.65F + 0.35F);
        this.lifetime = (int) MthUtils.nextWeighted(this.random,90, 1, 70);
        this.alpha = 0.5f;
        this.deltaRot = MthUtils.nextWeighted(this.random, 0.03f, 500);
        this.quadSize = 0.05f + MthUtils.nextWeighted(this.random, 0.1f, 1);
        this.roll = (float) (Math.PI * this.random.nextFloat());
        this.friction = 0.95f;
        this.gravity = 0;
        this.xd*=0.15;
        this.zd*=0.15;
        this.yd = 0.005+this.random.nextFloat()*0.05;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.deltaRot;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 vec3 = renderInfo.getPosition();
        float f = (float)(Mth.lerp(partialTicks, this.xo, this.x) - vec3.x());
        float g = (float)(Mth.lerp(partialTicks, this.yo, this.y) - vec3.y());
        float h = (float)(Mth.lerp(partialTicks, this.zo, this.z) - vec3.z());
        Quaternion quaternion;
        if (this.roll == 0.0F) {
            quaternion = renderInfo.rotation();
        } else {
            quaternion = new Quaternion(renderInfo.rotation());
            float i = Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.mul(Vector3f.ZP.rotation(i));
        }

        Vector3f vector3f = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f.transform(quaternion);
        Vector3f[] vector3fs = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float j = this.getQuadSize(partialTicks);

        for(int k = 0; k < 4; ++k) {
            Vector3f vector3f2 = vector3fs[k];
            vector3f2.transform(quaternion);
            vector3f2.mul(j);
            vector3f2.add(f, g, h);
        }

        float l = this.getU0();
        float m = this.getU1();
        float n = this.getV0();
        float o = this.getV1();
        int p = this.getLightColor(partialTicks);
        buffer.vertex(vector3fs[0].x(), vector3fs[0].y(),vector3fs[0].z()).uv(m, o).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(p).endVertex();
        buffer.vertex(vector3fs[1].x(), vector3fs[1].y(),vector3fs[1].z()).uv(m, n).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(p).endVertex();
        buffer.vertex(vector3fs[2].x(), vector3fs[2].y(),vector3fs[2].z()).uv(l, n).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(p).endVertex();
        buffer.vertex(vector3fs[3].x(), vector3fs[3].y(),vector3fs[3].z()).uv(l, o).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(p).endVertex();

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

    @Deprecated
    public static final ParticleRenderType GLOW_LIGHT_PARTICLE_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        public String toString() {
            return "PARTICLE_SHEET_ADDITIVE_TRANSLUCENT";
        }
    };
}
