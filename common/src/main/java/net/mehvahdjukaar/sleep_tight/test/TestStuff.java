package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Registration for the throwaway test entities. Everything here is dev only - keep the mod's real
 * content out of it so the whole package can be deleted without touching anything else.
 */
public class TestStuff {

    public static final RegSupplier<EntityType<BirdTestMob>> TEST_MOB = RegHelper.registerEntityType(
            SleepTight.res("test_mob"),
            EntityType.Builder.of(BirdTestMob::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(3));

    public static final Supplier<Item> TEST_MOB_SPAWN_EGG = RegHelper.registerItem(
            SleepTight.res("test_mob_spawn_egg"),
            () -> PlatHelper.newSpawnEgg(TEST_MOB, 0x3f8f3f, 0xd0e060, new Item.Properties()));

    // touching the class is enough to run the registrations above
    public static void init() {
    }
}
