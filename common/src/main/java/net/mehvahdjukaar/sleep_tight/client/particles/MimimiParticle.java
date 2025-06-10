package net.mehvahdjukaar.sleep_tight.client.particles;

import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class MimimiParticle extends TextureSheetParticle {

    private static final int FADE_START = 40;

    private final float yaw;
    @Nullable
    private final Entity owner;
    private float oQuadSize;
    private float quadInc;

    private float rollOffset;

    public MimimiParticle(ClientLevel clientLevel, double x, double y, double z, double yaw, double entityId, double _b) {
        super(clientLevel, x, y, z, 0.0, 0.0, 0.0);
        this.friction = 1;
        this.gravity = 0;
        this.yaw = (float) yaw;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.quadSize = 0.01F;
        this.oQuadSize = this.quadSize;
        this.quadInc = 0.0019f;
        this.lifetime = 140;
        this.hasPhysics = true;
        this.roll = Mth.sin((this.age / (float) this.lifetime * 3.6f * Mth.PI + rollOffset)) * 0.1f;
        this.oRoll = roll;
        this.rollOffset = Mth.randomBetween(this.random, 0, Mth.PI * 2);
        this.owner = clientLevel.getEntity((int) entityId);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return Mth.lerp(partialTicks, this.oQuadSize, this.quadSize);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll = Mth.sin((this.age / (float) this.lifetime * 3.6f * Mth.PI + rollOffset)) * 0.1f;
        float wobble = Mth.cos((float) this.age / this.lifetime * 2.7f* Mth.PI) * 0.006f;
        Vector3f vv = new Vector3f(wobble, 0, 0.0045f);
        vv.rotateY((this.yaw * Mth.DEG_TO_RAD) - 45);
        this.xd = vv.x();
        this.yd = 0.004;
        this.zd = vv.z();

        this.oQuadSize = this.quadSize;
        this.quadSize += this.quadInc;
        this.quadInc *= 0.98f;

        int timeLeft = lifetime - age;
        if (timeLeft < FADE_START) {
            alpha = (float) (timeLeft) / FADE_START;
        }

        if (this.owner != null && (!(this.owner instanceof LivingEntity le) || !le.isSleeping())  && timeLeft > FADE_START) {
            this.age = this.lifetime - FADE_START; //make it disappear faster
        }

        super.tick();
    }

    @Override
    public void move(double dx, double dy, double dz) {
        var wantedX = this.x + dx;
        var wantedY = this.y + dy;
        var wantedZ = this.z + dz;
        super.move(dx, dy, dz);
        boolean collided = Math.abs(this.x - wantedX) > 0.001 || Math.abs(this.y - wantedY) > 0.001 || Math.abs(this.z - wantedZ) > 0.001;
        int timeLeft = lifetime - age;
        if (collided) {
            this.hasPhysics = false;
            this.x = wantedX;
            this.y = wantedY;
            this.z = wantedZ;
            if (timeLeft > FADE_START) {
                this.age = this.lifetime - FADE_START; //make it disappear faster
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Factory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            MimimiParticle particle = new MimimiParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}