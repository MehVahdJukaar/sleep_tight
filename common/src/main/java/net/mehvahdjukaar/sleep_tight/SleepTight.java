package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.client.PackProvider;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.InfestedBedBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.mehvahdjukaar.sleep_tight.common.entities.DreamerEssenceTargetEntity;
import net.mehvahdjukaar.sleep_tight.common.items.BedbugEggsItem;
import net.mehvahdjukaar.sleep_tight.common.items.NightBagItem;
import net.mehvahdjukaar.sleep_tight.common.network.ModCommands;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
import net.mehvahdjukaar.sleep_tight.common.tiles.InfestedBedTile;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
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
    public static final boolean HS = PlatformHelper.isModLoaded("heartstone");

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }


    public static void commonInit() {
        NetworkHandler.registerMessages();
        ClientConfigs.init();
        CommonConfigs.init();

        ModCommands.init();
        RegHelper.addAttributeRegistration(SleepTight::registerEntityAttributes);
        RegHelper.addSpawnPlacementsRegistration(SleepTight::registerSpawnPlacements);

        if(PlatformHelper.getEnv().isClient()) {
            PackProvider.INSTANCE.register();
        }

        //sleep next to eachother bonus bugged
        //TODO: bedbug spawn
        //naturalist terry bear
        //together nightmares
        //bedbug actail behavior
        //pushing players in hammocks
        //phantom spawn stuff
    }

    public static void commonSetup() {

    }


    private static void registerSpawnPlacements(RegHelper.SpawnPlacementEvent event) {
        event.register(BEDBUG_ENTITY.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BedbugEntity::checkMonsterSpawnRules);
    }

    private static void registerEntityAttributes(RegHelper.AttributeEvent event) {
        event.register(DREAMER_ESSENCE_ENTITY.get(), DreamerEssenceTargetEntity.makeAttributes());
        event.register(BEDBUG_ENTITY.get(), BedbugEntity.makeAttributes());
    }

    //sound events

    public static final Supplier<SoundEvent> NIGHTMARE_SOUND = RegHelper.registerSound(res("nightmare"));
    public static final Supplier<SoundEvent> BEDBUG_AMBIENT = RegHelper.registerSound(res("bedbug.ambient"));
    public static final Supplier<SoundEvent> BEDBUG_DEATH = RegHelper.registerSound(res("bedbug.death"));
    public static final Supplier<SoundEvent> BEDBUG_HURT = RegHelper.registerSound(res("bedbug.hurt"));

    //tags

    public static final TagKey<EntityType<?>> WAKE_UP_BLACKLIST = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, res("wake_up_blacklist"));
    public static final TagKey<Block> BEDBUG_WALK_THROUGH = TagKey.create(Registry.BLOCK_REGISTRY, res("bedbug_walk_through"));

    //particles

    public static final Supplier<SimpleParticleType> DREAM_PARTICLE = RegHelper.registerParticle(res("dream"));
    public static final Supplier<SimpleParticleType> BEDBUG_PARTICLE = RegHelper.registerParticle(res("bedbug"));

    //effects

    public static final Supplier<MobEffect> INVIGORATED = RegHelper.registerEffect(res("invigorated"), () ->
            new InvigoratedEffect(MobEffectCategory.BENEFICIAL, 0x11ff22));

    //entities

    public static final Supplier<EntityType<BedEntity>> BED_ENTITY = RegHelper.registerEntityType(res("bed_entity"),
            BedEntity::new, MobCategory.MISC, 0.5f, 0.5f, 4, Integer.MAX_VALUE);

    public static final Supplier<EntityType<BedbugEntity>> BEDBUG_ENTITY = RegHelper.registerEntityType(res("bedbug"),
            BedbugEntity::new, MobCategory.MONSTER, 11 / 16f, 6 / 16f, 7, 3);

    public static final Supplier<EntityType<DreamerEssenceTargetEntity>> DREAMER_ESSENCE_ENTITY = RegHelper.registerEntityType(res("dreamer_essence_dummy"),
            DreamerEssenceTargetEntity::new, MobCategory.MISC, 0.2f, 12 / 16f, 5, Integer.MAX_VALUE);

    //blocks

    public static final Supplier<DreamEssenceBlock> DREAMER_ESSENCE = regWithItem("dreamer_essence", () ->
                    new DreamEssenceBlock(BlockBehaviour.Properties.of(Material.AMETHYST, MaterialColor.COLOR_PURPLE)
                            .sound(SoundType.AMETHYST).strength(1)),
            CreativeModeTab.TAB_DECORATIONS
    );

    public static final Supplier<NightBagBlock> NIGHT_BAG = regBlock("night_bag", () ->
            new NightBagBlock(BlockBehaviour.Properties.of(Material.WOOL, MaterialColor.TERRACOTTA_BLUE)
                    .sound(SoundType.WOOL).strength(0.1F))
    );

    public static final Supplier<InfestedBedBlock> INFESTED_BED = regBlock("infested_bed", () ->
            new InfestedBedBlock(BlockBehaviour.Properties.copy(Blocks.BROWN_BED))
    );

    public static final Map<DyeColor, Supplier<Block>> HAMMOCKS = Util.make(() ->
            Arrays.stream(DyeColor.values()).collect(Collectors.toUnmodifiableMap(d -> d, d ->
                    regWithItem("hammock_" + d.getName(), () ->
                            new HammockBlock(d), CreativeModeTab.TAB_DECORATIONS)))
    );

    //tile

    public static final Supplier<BlockEntityType<HammockTile>> HAMMOCK_TILE = RegHelper.registerBlockEntityType(
            res("hammock"), () -> PlatformHelper.newBlockEntityType(HammockTile::new,
                    HAMMOCKS.values().stream().map(Supplier::get).toArray(Block[]::new))
    );

    public static final Supplier<BlockEntityType<InfestedBedTile>> INFESTED_BED_TILE = RegHelper.registerBlockEntityType(
            res("infested_bed"), () -> PlatformHelper.newBlockEntityType(InfestedBedTile::new, INFESTED_BED.get())
    );

    //items

    public static final Supplier<NightBagItem> NIGHT_BAG_ITEM = regItem("night_bag", () ->
            new NightBagItem(NIGHT_BAG.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS))
    );

    public static final Supplier<BedbugEggsItem> BED_BUG_EGGS = regItem("bedbug_eggs", () ->
            new BedbugEggsItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC))
    );

    public static final Supplier<Item> BEDBUG_SPAWN_EGG = regItem("bedbug_spawn_egg", () ->
            PlatformHelper.newSpawnEgg(BEDBUG_ENTITY, 0x4b1813, 0xce5438, new Item.Properties().tab(CreativeModeTab.TAB_MISC))
    );


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


}
