package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.common.*;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Author: MehVahdJukaar
 */
public class SleepTight {

    public static final String MOD_ID = "sleep_tight";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final boolean SUPP = PlatformHelper.isModLoaded("supplementaries");

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }


    public static void commonInit() {
        NetworkHandler.registerMessages();
        ClientConfigs.init();
        CommonConfigs.init();

        RegHelper.addAttributeRegistration(SleepTight::registerEntityAttributes);

    }


    public static void commonSetup() {


    }

    private static void registerEntityAttributes(RegHelper.AttributeEvent event) {
        event.register(DREAMER_ESSENCE_ENTITY.get(), DreamerEssenceTargetEntity.makeAttributes());
    }

    public static final TagKey<EntityType<?>> WAKE_UP_BLACKLIST = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, res("wake_up_blacklist"));

    public static final Supplier<SimpleParticleType> DREAM_PARTICLE = RegHelper.registerParticle(res("dream"));


    public static final Supplier<EntityType<DreamerEssenceTargetEntity>> DREAMER_ESSENCE_ENTITY = RegHelper.registerEntityType(
            res("dreamer_essence_dummy"), () -> (
                    EntityType.Builder.of(DreamerEssenceTargetEntity::new, MobCategory.MISC)
                            //.setTrackingRange(64)
                            //.setUpdateInterval(3)
                            .sized(0.2f, 12 / 16f))
                    .build("dreamer_essence_dummy"));


    public static final Supplier<DreamEssenceBlock> DREAMER_ESSENCE = regWithItem("dreamer_essence", () ->
                    new DreamEssenceBlock(BlockBehaviour.Properties.of(Material.AMETHYST, MaterialColor.COLOR_PURPLE)
                            .sound(SoundType.AMETHYST).strength(1)),
            CreativeModeTab.TAB_DECORATIONS
    );

    public static final Supplier<NightBagBlock> NIGHT_BAG = regBlock("night_bag", () ->
            new NightBagBlock(BlockBehaviour.Properties.of(Material.WOOL, MaterialColor.TERRACOTTA_BLUE)
                    .sound(SoundType.WOOL).strength(0.1F))
    );

    public static final Supplier<NightBagItem> NIGHT_BAG_ITEM = regItem("night_bag", () ->
            new NightBagItem(NIGHT_BAG.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS))
    );

    public static final Map<DyeColor, Supplier<Block>> HAMMOCKS = Util.make(() ->
            Arrays.stream(DyeColor.values()).collect(Collectors.toUnmodifiableMap(d -> d, d ->
                    regWithItem("hammock_" + d.getName(), () ->
                            new HammockBlock(d), CreativeModeTab.TAB_DECORATIONS)))
    );

    public static final Supplier<BlockEntityType<HammockBlockEntity>> HAMMOCK_TILE = RegHelper.registerBlockEntityType(
            res("hammock"), () -> PlatformHelper.newBlockEntityType(HammockBlockEntity::new,
                    HAMMOCKS.values().stream().map(Supplier::get).toArray(Block[]::new))
    );

    public static final Supplier<EntityType<BedEntity>> BED_ENTITY = RegHelper.registerEntityType(res("bed_entity"),
            BedEntity::new, MobCategory.MISC, 0.5f, 0.5f, 3, Integer.MAX_VALUE);


    public static <T extends Block> Supplier<T> regWithItem(String name, Supplier<T> blockFactory, CreativeModeTab tab) {
        return regWithItem(name, blockFactory, new Item.Properties().tab(tab), 0);
    }

    public static <T extends Block> Supplier<T> regWithItem(String name, Supplier<T> blockFactory, Item.Properties properties, int burnTime) {
        Supplier<T> block = regBlock(name, blockFactory);
        regBlockItem(name, block, properties, burnTime);
        return block;
    }

    public static Supplier<BlockItem> regBlockItem(String name, Supplier<? extends Block> blockSup, Item.Properties properties, int burnTime) {
        return RegHelper.registerItem(res(name), () -> new BlockItem(blockSup.get(), properties));
    }

    public static <T extends Block> Supplier<T> regBlock(String name, Supplier<T> sup) {
        return RegHelper.registerBlock(res(name), sup);
    }

    public static <T extends Item> Supplier<T> regItem(String name, Supplier<T> sup) {
        return RegHelper.registerItem(res(name), sup);
    }


    public static <T extends Entity> Supplier<EntityType<T>> regEntity(
            String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height,
            int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        return RegHelper.registerEntityType(res(name), () ->
                PlatformHelper.newEntityType(name, factory, category, width, height,
                        clientTrackingRange, velocityUpdates, updateInterval));
    }


}
