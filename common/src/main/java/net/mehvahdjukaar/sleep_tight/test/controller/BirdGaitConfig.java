package net.mehvahdjukaar.sleep_tight.test.controller;

/**
 * Knobs for the ground half of locomotion, as opposed to {@link BirdFlightConfig} which tunes the
 * flying half. Same deal as the others: public static and mutable so they can be poked at runtime.
 */
public class BirdGaitConfig {

    // how fast the bird swings round on the spot before taking off. Slower than the airborne rate on
    // purpose: this is feet and tail shuffling, not a banked turn, and it is the one moment the whole
    // launch reads as deliberate rather than as a mob teleporting its facing
    public static float launchTurnPerTick = 12.0F;

    // how close to the launch heading counts as lined up. Not zero: the flight follower's first
    // carrot is a couple of blocks out, so the last few degrees are cheaper to fly off than to
    // shuffle off, and holding out for exact alignment just adds a tick of standing still
    public static float launchYawTolerance = 8.0F;

    // how far below the mob a surface has to be for it to bother descending onto it after a flight.
    // Without this a path that ends in mid air drops gravity on a bird with nothing underneath and it
    // falls out of the sky; with it, arriving in open air simply leaves it hovering
    public static double perchProbeDepth = 2.0;

    // whether arriving anywhere with ground under it ends in a perch at all. Off means the bird
    // hovers wherever the path left it, which is the pre-gait behaviour and the thing to compare
    // against if landing starts misbehaving
    public static boolean perchOnArrival = true;

    // ---- walking ----
    // When a hop is short enough that flying it is silly. Both gates are checked before either search
    // runs, so a long trip never pays for a ground query it was always going to lose.

    // how far the destination may be, horizontally, for walking to be considered at all
    public static double walkMaxDistance = 5.0;

    // and how far up or down. Separate from the distance above because this is about what a walker
    // can step over, not about how far it is: a target two blocks away and five up is a flight
    public static double walkMaxRise = 2.0;

    // the debuff, and the whole reason this is a comparison rather than a rule. It multiplies the
    // estimated walking time only; the mob still walks at its real MOVEMENT_SPEED, which for these
    // attributes is actually faster than cruise, so without a thumb on the scale a bird would walk
    // every hop that happened to be short. Raising it makes walking the exception again
    public static double walkCostPenalty = 2.0;

    // what flying a path costs before any of its length is flown: the launch pivot, the wings
    // spooling up, the descent and the perch at the far end. Roughly a second, and it is the term
    // that actually decides short hops, since no length-based comparison can see it
    public static double takeoffCostTicks = 20.0;

    // speed modifier handed to the walking navigation. One means the mob's own MOVEMENT_SPEED; the
    // debuff lives in the comparison above rather than here, so what gets chosen and how fast it
    // then happens stay separate knobs
    public static double walkSpeedModifier = 1.0;
}
