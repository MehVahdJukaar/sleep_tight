package net.mehvahdjukaar.sleep_tight.client.particles;

import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class BedbugParticle extends TextureSheetParticle {
    public BedbugParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
        super(clientLevel, d, e, f, 0.0, 0.0, 0.0);
        this.friction = 0.7F;
        this.gravity = 0.5F;
        this.xd *= 0.10000000149011612;
        this.yd = MthUtils.nextWeighted(this.random, 0.15f, 1, 0.02f);
        this.zd *= 0.10000000149011612;
        this.xd += g * 0.4;
        this.zd += i * 0.4;
        this.quadSize *= 0.5F;
        this.lifetime = (int) MthUtils.nextWeighted(this.random, 20, 0.5f, 10);
        this.hasPhysics = true;
        this.tick();
        this.roll = MthUtils.nextWeighted(this.random, 0.3f, 80) * Mth.PI * (this.random.nextBoolean() ? -1 : 1);
        this.oRoll = roll;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize;// * Mth.clamp(( this.age + scaleFactor) /  this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.roll += Mth.randomBetween(this.random, -0.2f, 0.2f);
        boolean wasOnGround = this.onGround;
        super.tick();
        if (!wasOnGround && this.onGround) {
            this.lifetime = this.age + 3;
            this.hasPhysics = false;
            this.x = xo;
            this.y = yo;
            this.z = zo;
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }


    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Factory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BedbugParticle particle = new BedbugParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}