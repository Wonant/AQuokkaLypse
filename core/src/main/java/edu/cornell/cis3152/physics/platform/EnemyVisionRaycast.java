package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

/**
 * Helper class for ray casting operations that detects obstacles between two points.
 * This is used primarily for line-of-sight checks in enemy vision systems.
 */
public class EnemyVisionRaycast implements RayCastCallback {

    public enum VisionMode {STAIR_CHECK, WALL_CHECK, PLAYER_CHECK, FALL_CHECK}

    private VisionMode mode;

    private Fixture hitFixture = null;
    private Vector2 hitPoint = new Vector2();
    private float closestFraction = 1f;
    public boolean fixtureIsStair;

    private Player hitPlayer = null;
    private Vector2 hitPlayerPosition = null;

    private Vector2 posAboveStair = new Vector2();

    private float stepHeightThreshold;

    /** Whether the ray has hit a blocking object */
    private boolean blocked;

    /** The body that is casting the ray (to be ignored during checks) */
    private Body sourceBody;

    /** The target body we're checking visibility for (to be ignored during checks) */
    private Body targetBody;

    /** Additional bodies to ignore during ray casting */
    private Body[] ignoreBodies;

    /**
     * Creates a ray callback that checks for blocking objects between source and target.
     *
     * @param sourceBody The body casting the ray (will be ignored)
     * @param targetBody The body we're checking visibility for (will be ignored)
     * @param ignoreBodies Additional bodies to ignore during ray casting
     */
    public EnemyVisionRaycast(Body sourceBody, Body targetBody, Body... ignoreBodies) {
        this.blocked = false;
        this.sourceBody = sourceBody;
        this.targetBody = targetBody;
        this.ignoreBodies = ignoreBodies;
    }

    /**
     * Simplified constructor when there are no additional bodies to ignore.
     *
     * @param sourceBody The body casting the ray
     * @param targetBody The body we're checking visibility for
     */
    public EnemyVisionRaycast(Body sourceBody, Body targetBody) {
        this(sourceBody, targetBody, new Body[0]);
    }

    public EnemyVisionRaycast(VisionMode mode, float stepHeightThreshold) {
        this.mode = mode;
        this.stepHeightThreshold = stepHeightThreshold;
    }

    /**
     * Returns whether the ray hit a blocking object.
     *
     * @return true if the ray was blocked, false otherwise
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Resets the blocked status for reuse.
     */
    public void reset() {
        blocked = false;
        hitFixture = null;
        hitPoint.set(0, 0);
        closestFraction = 1f;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        if (mode == VisionMode.STAIR_CHECK) {
            Object userData = fixture.getBody().getUserData();
            if (!(userData instanceof Surface)) {
                return -1f;
            } else {
                Surface surface = (Surface) userData;
                String name = surface.getObstacle().getName();
                if (name.startsWith("stair")) {
                    posAboveStair.set(point).add(normal.cpy().nor());
                    hitFixture = fixture;
                    closestFraction = fraction;
                    fixtureIsStair = true;
                    hitPoint.set(point);
                    return 0;
                } else {
                    fixtureIsStair = false;
                    return 0;
                }
            }
        }

        else if (mode == VisionMode.FALL_CHECK) {
            if (fixture.isSensor()) return 1;
            if (fixture.getBody().getUserData() instanceof Surface) {
                hitFixture = fixture;
                hitPoint.set(point);
                return 0;
            }
            return 1;
        }

        else if (mode == VisionMode.WALL_CHECK) {
            if (fixture.isSensor()) return 1;
            Object userData = fixture.getBody().getUserData();
            if (userData instanceof Surface) {
                Surface surface = (Surface) userData;
                if (!surface.getObstacle().getName().startsWith("stair")) {
                    hitFixture = fixture;
                    hitPoint.set(point);
                    closestFraction = fraction;
                    return 0;
                }
            }
            return 1;
        }

        else if (mode == VisionMode.PLAYER_CHECK) {
            if (fixture.isSensor()) return 1;
            Object userData = fixture.getBody().getUserData();
            if (userData instanceof Surface) {
                hitFixture = fixture;
                hitPoint.set(point);
                return 0;
            } else if (userData instanceof Player) {
                Player player = (Player) userData;
                hitPlayer = player;
                hitPlayerPosition = player.getObstacle().getPosition();
                hitFixture = fixture;
                hitPoint.set(point);
                closestFraction = fraction;

                return fraction;
            }
        }

        else {
            if (fixture.isSensor()) return 1;
            if (fixture.getBody() == sourceBody || fixture.getBody() == targetBody) return 1;
            for (Body body : ignoreBodies) {
                if (fixture.getBody() == body) return 1;
            }
            if (fixture.getUserData() != null) {
                String userData = fixture.getUserData().toString();
                if (userData.contains("player") || userData.contains("ignore_vision")) {
                    return 1;
                }
            }
            blocked = true;
            return 0;
        }

        return 1f;
    }

    public Player getHitPlayer() {
        return hitPlayer;
    }

    public Vector2 getHitPlayerPosition() {
        return hitPlayerPosition;
    }

    public void setMode(EnemyVisionRaycast.VisionMode mode) {
        this.mode = mode;
    }

    public Fixture getHitFixture() {
        return hitFixture;
    }

    public Vector2 getHitPoint() {
        return hitPoint;
    }

    public float getClosestFraction() {
        return closestFraction;
    }

}

