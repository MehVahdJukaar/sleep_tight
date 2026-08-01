package net.mehvahdjukaar.sleep_tight.test.controller;

/**
 * A flier that can put its feet down, and the seam between its two halves of locomotion.
 * <p>
 * Implemented by the mob so that neither the search nor the flight follower has to know the mob
 * class. It is deliberately the whole contract between the flying side and the ground side: when
 * a second, walking navigation arrives, it is this interface it has to satisfy, and nothing above
 * it changes.
 */
public interface PerchingFlier {

    /**
     * Feet planted on a surface. Not the same as being low, slow or touching the ground mid-flight:
     * a bird hovering a hand's width above a roof is still flying and still carries its heading.
     * <p>
     * What reads it: the pathfinder, which lets a perched bird's search leave in any direction
     * because there is no airspeed to conserve (see {@code BirdNodeEvaluator#getStart}).
     */
    boolean isPerched();

    /**
     * True while the ground half is swinging the mob round to face where the path leaves from.
     * Flight is suspended for the duration: the navigation does not advance the path and the move
     * control does not thrust.
     */
    boolean isHoldingForLaunch();

    /**
     * Told by the navigation when a path has been accepted, so the ground half can line the mob up
     * with it before it leaves the ground. Ignored unless the mob is actually perched.
     *
     * @param launchYaw the yaw the first stretch of the path leaves along
     */
    void requestLaunch(float launchYaw);

    /** The path went away before the mob left the ground, so there is nothing to line up with. */
    void cancelLaunch();
}
