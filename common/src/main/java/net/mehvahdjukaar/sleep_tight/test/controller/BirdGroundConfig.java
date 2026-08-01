package net.mehvahdjukaar.sleep_tight.test.controller;

/**
 * Knobs for the ground half of locomotion, as opposed to {@link BirdFlightConfig} which tunes the
 * flying half. Same deal as the others: public static and mutable so they can be poked at runtime.
 */
public class BirdGroundConfig {

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
}
