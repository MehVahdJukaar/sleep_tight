package net.mehvahdjukaar.sleep_tight.test.controller;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

import java.util.Optional;

/**
 * Hands pitch over to {@link BirdMoveControl}. The default LookControl zeroes xRot every tick and
 * otherwise aims it at whatever a look goal picked, and it ticks after the move control in
 * {@code Mob.serverAiStep}, so without this the flight pitch is silently overwritten every tick.
 * Head yaw is left to vanilla, a bird looking around while flying straight is fine.
 */
public class BirdLookControl extends LookControl {

    public BirdLookControl(Mob mob) {
        super(mob);
    }

    @Override
    protected boolean resetXRotOnTick() {
        return false;
    }

    @Override
    protected Optional<Float> getXRotD() {
        return Optional.empty();
    }
}
