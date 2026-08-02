package net.mehvahdjukaar.sleep_tight.test.navigator;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightControl;
import net.mehvahdjukaar.sleep_tight.test.controller.PerchingFlier;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNodeEvaluator;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathFinder;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottlePlanner;
import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottleProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Drop-in flying navigation using the bird lattice pathfinder. Hook it to a mob by
 * returning this from {@code Mob.createNavigation}, and pair it with {@link BirdFlightControl};
 * vanilla's FlyingMoveControl will not fly these paths as planned.
 * <p>
 * Besides swapping in the finder this replaces the whole waypoint rule with a {@link PathRuler}.
 * Vanilla's is built for mobs that can stop on a dime and turn instantly, neither of which is true
 * here. This class only wires the ruler up; the geometry lives in the ruler and the steering in
 * {@link BirdFlightControl}.
 */
public class BirdFlightNavigation extends FlyingPathNavigation {

    /** Below this much horizontal spread the path leaves vertically and there is nothing to face. */
    private static final double MIN_LAUNCH_SPREAD = 1.0E-4;

    // vanilla keeps its PathFinder private, and createPathFinder runs from the super constructor,
    // so this deliberately has no initializer: one here would run afterwards and wipe it
    @Nullable
    private BirdPathFinder finder;

    @Nullable
    private PathRuler ruler;
    @Nullable
    private Path ruledPath;
    @Nullable
    private ThrottleProfile throttle;
    @Nullable
    private FlightEnvelope envelope;

    // last node index handed to the path, so a change can restart vanilla's node timeout. Now that
    // the cursor may lose ground this also fires on the way back, which is the right call per node
    // but means the node timeout cannot catch a mob oscillating between two of them. The 100 tick
    // distance-based stuck check is what covers that case
    private int lastNodeIndex = -1;
    // this tick's answers, worked out in followThePath and read by the move control right after
    private double speedLimit = Double.MAX_VALUE;
    private double profiledSpeedLimit = Double.MAX_VALUE;

    public BirdFlightNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    /**
     * The {@code maxVisitedNodes} vanilla hands in is {@code floor(FOLLOW_RANGE * 16)}, sized for a
     * search with one node per cell. This lattice has one per heading bin, so that budget would
     * cover an eighth as much ground and hand back partial paths from searches that were nowhere
     * near exhausted. Recomputed here from the two factors that actually determine it rather than
     * scaled by a magic number.
     */
    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new BirdNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.finder = new BirdPathFinder((BirdNodeEvaluator) this.nodeEvaluator, latticeNodeBudget(this.mob));
        return this.finder;
    }

    /** What the search turned down at each node of the current path, for the debug renderer. */
    public List<BirdPathFinder.ConsideredMove> getConsideredMoves() {
        return this.finder != null ? this.finder.getConsideredMoves() : List.of();
    }

    /** {@code followRange * nodesPerBlockOfRange * statesPerCell}, at defaults 64 * 16 * 24 = 24576. */
    private static int latticeNodeBudget(Mob mob) {
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return Mth.floor(followRange * BirdPathfindingConfig.nodesPerBlockOfRange
                * BirdNodeEvaluator.HEADING_BINS * BirdNodeEvaluator.CLIMB_STATES);
    }

    /**
     * Every "go there" overload hands the destination to the mob, which owns both halves of the
     * locomotion and so is the only thing that can weigh walking against flying it. What comes back
     * here is a flight, through {@link #moveTo(Path, double)}, or nothing because the mob is already
     * walking there. {@link BirdWalkNavigation} has the mirror image of this.
     * <p>
     * The alternative shape, one navigation wrapping the two and delegating, was rejected: it would
     * have to forward around thirty public methods, implement {@code createPathFinder} with a third
     * pathfinder it never uses, shadow a dozen protected fields that would then always read empty,
     * and be paired with a second wrapper around the move control. Worse, it would make
     * {@code getNavigation()} return the same type in both modes, and the follower, the throttle
     * layer and the debug packets all discover which half is live by {@code instanceof} on exactly
     * that.
     */
    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(BlockPos.containing(x, y, z), 1, speed)
                : super.moveTo(x, y, z, speed);
    }

    @Override
    public boolean moveTo(double x, double y, double z, int accuracy, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(BlockPos.containing(x, y, z), accuracy, speed)
                : super.moveTo(x, y, z, accuracy, speed);
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(entity.blockPosition(), 1, speed)
                : super.moveTo(entity, speed);
    }

    /**
     * Profiling happens here rather than in {@code tick} because it needs the final geometry:
     * {@code super.moveTo} runs {@code trimPath} first, which can move nodes around.
     */
    @Override
    public boolean moveTo(@Nullable Path path, double speed) {
        boolean accepted = super.moveTo(path, speed);
        this.lastNodeIndex = -1;
        if (accepted && this.path != null) {
            // snapshotted here so the path is flown against the numbers it was planned with, even
            // if the config is poked mid-flight
            this.envelope = FlightEnvelope.forMob(this.mob);
            this.throttle = ThrottlePlanner.fromPath(this.path, this.mob, this.envelope);
            this.requestLaunch();
        } else {
            this.envelope = null;
            this.throttle = null;
        }
        return accepted;
    }

    /**
     * Hands the ground layer the heading this path leaves along, so a perched bird can turn to face
     * it before it flies. Asked for on every path and ignored unless the mob actually has its feet
     * down, because whether it does is not this layer's business to decide.
     * <p>
     * The heading is taken from the follower's own first aiming point rather than from the first
     * node, so the turn ends exactly where pure pursuit is about to start and there is no leftover
     * correction on the first tick of flight. A path that leaves straight up has no heading to turn
     * to, and asking for the current one lets the launch complete immediately rather than special
     * casing it.
     */
    private void requestLaunch() {
        if (!(this.mob instanceof PerchingFlier flier)) {
            return;
        }
        PathRuler ruler = this.ruler();
        Vec3 away = this.carrotFor(ruler).subtract(this.getTempMobPos());
        float launchYaw = away.horizontalDistanceSqr() < MIN_LAUNCH_SPREAD
                ? this.mob.getYRot() : BirdFlightControl.yawTowards(away.x, away.z);
        flier.requestLaunch(launchYaw);
    }

    /** True while the ground layer is still turning the mob to face the path it was just given. */
    public boolean isHeldOnGround() {
        return this.mob instanceof PerchingFlier flier && flier.isHoldingForLaunch();
    }

    /** How fast the mob is allowed to be along the current path. Null when there is nothing to fly. */
    @Nullable
    public ThrottleProfile getThrottleProfile() {
        return this.throttle;
    }

    /** The envelope the current path was planned against, for the move control to steer by. */
    @Nullable
    public FlightEnvelope getFlightEnvelope() {
        return this.envelope;
    }

    /**
     * Vanilla aims the move control at the next node. Aim it at the ruler's lookahead point instead,
     * which is the whole reason the path gets flown as a curve rather than as a sequence of headings.
     * Navigation ticks before the move control, so this is what the steering ends up seeing.
     * <p>
     * How far ahead is the control's policy, not ours: it is the one that knows how much of the line
     * it is willing to round off. Turning that distance into a point is route geometry, which is why
     * the resolving happens here, and why the corridor check on top of it lives here too.
     */
    @Override
    public void tick() {
        // the mob is on its feet lining up with the path. Nothing about following it applies yet:
        // not the cursor, not the ruler, and above all not the timeouts, which would otherwise spend
        // the whole turn counting the mob as failing to reach its first node.
        // The carrot is parked on the mob rather than left alone, because the hold lifts from the
        // ground control, which runs after this method: on the tick it does, the move control would
        // otherwise find whatever the last path left in there and fly a tick at it, in a direction
        // the turn was there to get away from. Aimed at itself, that tick coasts instead
        if (this.isHeldOnGround()) {
            this.mob.getMoveControl().setWantedPosition(
                    this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.speedModifier);
            return;
        }
        super.tick();
        if (!this.isDone() && this.ruler != null) {
            Vec3 carrot = this.carrotFor(this.ruler);
            // deliberately not getGroundY: that snaps the y to the top of whatever is under the
            // carrot's block, which is right for a walker and drags a flier down into the terrain
            // any time it flies within a block of a surface. FlyingPathNavigation never overrode it
            this.mob.getMoveControl().setWantedPosition(carrot.x, carrot.y, carrot.z, this.speedModifier);
        }
    }

    /**
     * Where the follower should aim this tick, which is not simply a point {@code lookahead} blocks
     * further along the line.
     * <p>
     * Pure pursuit flies the chord to its carrot, so wherever the line bends between here and there
     * the mob cuts the bend, and the cut is a direction and not just a size: over a staircase it is
     * downward, into the steps. And the room to cut into is nothing like the same on all sides,
     * because the line is drawn along the bird's feet. So the bend is measured, the aim point is
     * pushed back along it by as much as the corridor will take, and only if the leftover still does
     * not fit is the lookahead pulled in until it does. Biasing is the useful half and shortening is
     * the blunt one, which is why it is the fallback: a shorter carrot stops smoothing and goes back
     * to tracking the polyline, corners and all.
     * <p>
     * This is the geometric version of what {@code enclosure} was doing by proxy. Measuring the cut
     * that is about to happen beats inferring it from how walled in the cell is, which cannot say
     * which side the walls are on and, normalised over 26 neighbours, barely moves for the one
     * blocked face under a bird skimming a surface.
     */
    private Vec3 carrotFor(PathRuler ruler) {
        double cursor = ruler.cursor();
        double lookahead = this.lookahead(ruler);
        FlightEnvelope corridor = this.envelope != null ? this.envelope : FlightEnvelope.forMob(this.mob);
        Vec3 cut = ruler.chordDeviation(cursor, cursor + lookahead);
        for (int trim = 0; trim < BirdFlightConfig.maxLookaheadTrims && lookahead > BirdFlightConfig.enclosedLookahead
                && !fitsCorridor(corridor, cut); trim++) {
            lookahead = Math.max(BirdFlightConfig.enclosedLookahead, lookahead * BirdFlightConfig.lookaheadTrim);
            cut = ruler.chordDeviation(cursor, cursor + lookahead);
        }
        // towards the bulge, since that is the side the chord falls short on
        return ruler.lookaheadPoint(lookahead).add(corridor.clampToCorridor(cut));
    }

    private static boolean fitsCorridor(FlightEnvelope corridor, Vec3 cut) {
        return corridor.clampToCorridor(cut).distanceToSqr(cut) < 1.0E-6;
    }

    private double lookahead(PathRuler ruler) {
        return this.mob.getMoveControl() instanceof BirdFlightControl bird
                ? bird.lookahead(ruler.enclosureAt(ruler.cursor()), ruler.offRoute())
                : BirdFlightConfig.openAirLookahead;
    }

    /**
     * No acceptance test along the path at all: the cursor is wherever the mob projects onto it, and
     * the path's node index is dragged along behind. Missing a node is no longer an event, so
     * nothing can send the mob back around for one.
     * <p>
     * The end is the exception, and needs a radius rather than a projection. Drag is the only brake,
     * so the profile's arrival ramp approaches the last node asymptotically and the cursor never
     * actually reaches it.
     * <p>
     * Vanilla's corner cutting (canCutCorner plus shouldTargetNextNodeInDirection) is deliberately
     * not carried over: it skips a node whenever the one after it is closer, which on a banked arc
     * is most of them, and straightens exactly the curve the lattice was built to produce.
     */
    @Override
    protected void followThePath() {
        Vec3 pos = this.getTempMobPos();
        PathRuler ruler = this.ruler();
        ruler.advanceCursorTo(pos, BirdFlightConfig.projectionWindow);
        this.speedLimit = this.speedLimitFor(ruler);
        if (ruler.remaining() <= BirdFlightConfig.arrivalRadius) {
            // dropping the path here is what makes arrival absorbing. Left running, the follower
            // would keep steering at a final node it is already on top of, overshoot it, turn back
            // and oscillate, which is the second class of 180 in believable_bird_flight.md section 6
            this.stop();
            return;
        }
        int nextNode = ruler.nextNodeIndex();
        if (nextNode != this.lastNodeIndex) {
            this.lastNodeIndex = nextNode;
            this.restartNodeTimeout();
        }
        this.path.setNextNodeIndex(nextNode);
        // acceptance spheres are gone, so this now only sizes the markers in the debug path renderer
        this.maxDistanceToWaypoint = (float) BirdFlightConfig.openAirLookahead;
        this.doStuckDetection(pos);
    }

    /**
     * The line vanilla forgets. Its node timeout is meant to ask "am I taking too long to reach the
     * node I am heading for", and on a node change it does recompute {@code timeoutLimit} as that
     * node's budget, but it never zeroes {@code timeoutTimer} to match. Only {@code timeoutPath()}
     * firing does that. So the clock runs from the start of the path while the budget stays at one
     * node's worth, and every mob is on a fixed fuse of roughly 200 ticks however well it is going.
     * <p>
     * Vanilla survives its own bug two ways: its paths are usually short enough to finish inside the
     * fuse, and when they are not, {@code timeoutPath} stops the path having just zeroed the timer,
     * the goal sees {@code isDone()} and repaths on the spot, and the recycle is invisible. Neither
     * applies here. A lattice path is up to 64 blocks, this mob covers 11 in 200 ticks, and nothing
     * repaths it, so the fuse burned down around the twelfth node and the flight simply ended.
     */
    private void restartNodeTimeout() {
        this.timeoutTimer = 0L;
    }

    /**
     * The speed cap for this tick: the limit underfoot, tightened by whatever the mob cannot brake
     * down to in time and floored while it is nowhere near the line.
     * <p>
     * Underfoot alone is right for a mob tracing the line exactly. The planner's backwards pass
     * guarantees {@code limit[i] <= limit[j] + (arc[j] - arc[i]) * (1 - drag)} for every later j, so
     * a node's limit is by construction one that coasting can still satisfy every downstream node
     * from. Reading a window ahead on top of that applies the same braking distance twice, which is
     * what {@code speedLimitOver} does and why a follower using it crawls into corners.
     * <p>
     * A control that rounds corners off breaks that guarantee, and not in an obvious way. It covers
     * <i>arc</i> faster than it covers ground: the cursor sweeps along the line while the mob takes
     * the short way across the corner. Drag only bleeds speed per block actually flown, so over the
     * same stretch of route the mob sheds less than the profile assumed and reaches the corner above
     * its limit. So rather than guess a lookahead, apply the condition the braking pass itself is
     * built on with the mob's real braking authority substituted in. At {@code groundPerArc} of one
     * this reduces to exactly the limit underfoot, so a mob that follows the line pays nothing.
     */
    private double speedLimitFor(PathRuler ruler) {
        ThrottleProfile profile = this.throttle;
        FlightEnvelope flightEnvelope = this.envelope;
        if (profile == null || flightEnvelope == null) {
            this.profiledSpeedLimit = Double.MAX_VALUE;
            return Double.MAX_VALUE;
        }
        double cursor = ruler.cursor();
        this.profiledSpeedLimit = profile.speedLimitAt(cursor);
        double bleedPerArc = ruler.groundPerArc() * (1.0 - flightEnvelope.brakingDrag());
        double window = cursor + flightEnvelope.stoppingDistance(this.mob.getDeltaMovement().length());

        double tightest = this.profiledSpeedLimit;
        for (int i = ruler.nextNodeIndex(); i < profile.nodeCount() && profile.arcAtNode(i) <= window; i++) {
            double ahead = profile.arcAtNode(i) - cursor;
            tightest = Math.min(tightest, profile.limitAtNode(i) + ahead * bleedPerArc);
        }
        return Math.max(tightest, this.rejoinSpeed(flightEnvelope, ruler.offRoute()));
    }

    /**
     * A floor on the speed while the mob is nowhere near the line, ramping in from
     * {@link BirdFlightConfig#rejoinFrom} to {@link BirdFlightConfig#rejoinBy} blocks off it.
     * <p>
     * The profile describes speeds for a mob <i>on</i> the path, and every limit in it is about
     * geometry the mob is only subject to while tracing it. A mob that has been knocked well clear
     * has to fly back first, and that leg is not the profile's business. Without this it obeys
     * whatever limit it happens to be level with, and level with is decided by the foot of a
     * perpendicular that a sideways excursion slides a long way up the line: a shove near the end of
     * a path leaves the mob reading arrival speed from a dozen blocks out, crawling home at a
     * hundredth of a block per tick until a watchdog puts it down.
     * <p>
     * Ramped rather than switched, and starting outside the range normal flying reaches, so a mob
     * that is merely rounding a corner off gets nothing from it and every profiled corner limit
     * stands exactly as planned.
     */
    private double rejoinSpeed(FlightEnvelope flightEnvelope, double offRoute) {
        double lost = Mth.clamp((offRoute - BirdFlightConfig.rejoinFrom)
                / (BirdFlightConfig.rejoinBy - BirdFlightConfig.rejoinFrom), 0.0, 1.0);
        return lost * flightEnvelope.cruiseSpeed();
    }

    /** What the move control is allowed to fly at this tick. Unbounded when there is no profile. */
    public double getSpeedLimit() {
        return this.speedLimit;
    }

    /**
     * The profile's limit exactly underfoot, which is what the mob should be judged against. Not
     * always what the control was told to do, see {@link #speedLimitFor}, so measuring compliance
     * against the value that was fed would be marking its own homework.
     */
    public double getProfiledSpeedLimit() {
        return this.profiledSpeedLimit;
    }

    /** How far the mob is from the drawn line. 0 if there is no ruler yet. */
    public double getOffRoute() {
        return this.ruler != null ? this.ruler.offRoute() : 0.0;
    }

    private PathRuler ruler() {
        if (this.ruler == null || this.ruledPath != this.path) {
            this.ruledPath = this.path;
            this.ruler = new PathRuler(this.path, this.mob);
        }
        return this.ruler;
    }

    /** How far along the current path the mob's projected position sits. 0 if there is no ruler yet. */
    public double getRulerCursor() {
        return this.ruler != null ? this.ruler.cursor() : 0.0;
    }

    /** Total arc length of the current path. 0 if there is no ruler yet. */
    public double getRulerLength() {
        return this.ruler != null ? this.ruler.length() : 0.0;
    }

    /**
     * Ticks the vanilla node-timeout watchdog has spent stalled on the current node, and the budget
     * it gets before {@code timeoutPath()} kills the path. {@code doStuckDetection} still keys this
     * off {@code path.getNextNodePos()}, the raw vanilla node, even though the ruler is what actually
     * drives {@code nextNodeIndex} here - worth watching separately from {@link #isStuck()} since
     * {@code timeoutPath()} clears the stuck flag right before it calls {@code stop()}.
     */
    public long getTimeoutTimer() {
        return this.timeoutTimer;
    }

    public double getTimeoutLimit() {
        return this.timeoutLimit;
    }

    /** Ticks since the last 100-tick distance-based stuck check, for watching that countdown too. */
    public int getTicksSinceStuckCheck() {
        return this.tick - this.lastStuckCheck;
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob instanceof PerchingFlier flier) {
            // there is no longer a path to line up with, so a launch part way through is off
            flier.cancelLaunch();
        }
        this.ruler = null;
        this.ruledPath = null;
        this.throttle = null;
        this.envelope = null;
        this.speedLimit = Double.MAX_VALUE;
        this.profiledSpeedLimit = Double.MAX_VALUE;
    }
}
