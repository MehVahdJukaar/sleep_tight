package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.sleep_tight.client.particles.BedbugParticle;
import net.mehvahdjukaar.sleep_tight.client.particles.DreamParticle;
import net.mehvahdjukaar.sleep_tight.client.particles.MimimiParticle;
import net.mehvahdjukaar.sleep_tight.client.renderers.BedbugEntityRenderer;
import net.mehvahdjukaar.sleep_tight.client.renderers.HammockBlockTileRenderer;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

public class SleepTightClient {

    public static final ModelLayerLocation NIGHT_BAG = loc("night_bag");
    public static final ModelLayerLocation HAMMOCK = loc("hammock");
    public static final ModelLayerLocation BEDBUG = loc("bedbug");
    public static final ResourceLocation BED_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/beds.png");
    public static final ResourceLocation ICONS = SleepTight.res("textures/gui/sleep_icons.png");
    public static final ResourceLocation BEDBUG_TEXTURE = SleepTight.res("textures/entity/bedbug.png");
    public static final ResourceLocation SLEEPING_VILLAGER_TEXTURE = SleepTight.res("textures/entity/villager_sleeping.png");

    public static final Material[] HAMMOCK_TEXTURES = Arrays.stream(DyeColor.values())
            .sorted(Comparator.comparingInt(DyeColor::getId))
            .map(dyeColor -> new Material(BED_SHEET, SleepTight.res("entity/bed/hammock_" + dyeColor.getName())))
            .toArray(Material[]::new);

    public static boolean HAS_SNORE = Calendar.getInstance().get(Calendar.MONTH) == Calendar.APRIL
            && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1; //april fools

    public static void init() {
        ClientHelper.addModelLayerRegistration(SleepTightClient::registerLayers);
        ClientHelper.addEntityRenderersRegistration(SleepTightClient::registerEntityRenderers);
        ClientHelper.addBlockEntityRenderersRegistration(SleepTightClient::registerBlockEntityRenderers);
        ClientHelper.addParticleRegistration(SleepTightClient::registerParticles);
    }


    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(SleepTight.res(name), name);
    }

    private static void registerParticles(ClientHelper.ParticleEvent event) {
        event.register(SleepTight.DREAM_PARTICLE.get(), DreamParticle.Factory::new);
        event.register(SleepTight.BEDBUG_PARTICLE.get(), BedbugParticle.Factory::new);
        event.register(SleepTight.ZZZ_PARTICLE.get(), MimimiParticle.Factory::new);
    }

    private static void registerLayers(ClientHelper.ModelLayerEvent event) {
        event.register(HAMMOCK, HammockBlockTileRenderer::createLayer);
        event.register(BEDBUG, BedbugEntityRenderer::createLayer);
    }

    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        event.register(SleepTight.BED_ENTITY.get(), NoopRenderer::new);
        event.register(SleepTight.DREAMER_ESSENCE_ENTITY.get(), NoopRenderer::new);
        event.register(SleepTight.BEDBUG_ENTITY.get(), BedbugEntityRenderer::new);
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(SleepTight.HAMMOCK_TILE.get(), HammockBlockTileRenderer::new);
    }

    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    @EventCalled
    public static void onEntityTick(LivingEntity entity) {
        if (entity.isSleeping() && ClientConfigs.ZZZ_PARTICLES.get()
                && !entity.getType().is(SleepTight.NO_SLEEP_PARTICLES)
                && entity.tickCount % 35 == 0) {
            Vec3 pos = entity.position().add(0, entity.getEyeHeight() + 1 / 16f, 0);
            Level level = entity.level();
            float yawDeg = 180 - entity.getViewYRot(1);
            var bedPos = entity.getSleepingPos();
            if (bedPos.isPresent()) {
                BlockState bedState = level.getBlockState(bedPos.get());
                if (bedState.hasProperty(BedBlock.FACING)) {
                    yawDeg = -bedState.getValue(BedBlock.FACING).toYRot();
                }
            }

            level.addParticle(SleepTight.ZZZ_PARTICLE.get(),
                    pos.x, pos.y, pos.z,
                    yawDeg, entity.getId(), 0);
        }
    }

    @Nullable
    public static BedData getLayingBedData() {
        Player player = getPlayer();
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof BedEntity be) {
            BlockPos pos = be.blockPosition();
            return STPlatStuff.getBedDataIfPresent(player.level(), pos);
        }
        return null;
    }

    private static long lastTick = 0;
    private static boolean hasDreamEssenceInRange = false;

    public static boolean getCachedDreamEssenceInRange(BlockPos pos, Level level) {
        if (level.getGameTime() != lastTick) {
            hasDreamEssenceInRange = DreamEssenceBlock.isInRangeInternal(pos, level);
            lastTick = level.getGameTime();
        }
        return hasDreamEssenceInRange;
    }
}
