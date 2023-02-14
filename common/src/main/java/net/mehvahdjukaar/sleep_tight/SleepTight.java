package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    }


    public static void commonSetup() {
    }




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
           BedEntity::new, MobCategory.MISC, 0.5f, 0.25f, 3, Integer.MAX_VALUE );


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

    public static <T extends Entity> Supplier<EntityType<T>> regEntity(
            String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height,
            int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        return RegHelper.registerEntityType(res(name), () ->
                PlatformHelper.newEntityType(name, factory, category, width, height,
                        clientTrackingRange, velocityUpdates, updateInterval));
    }


}
