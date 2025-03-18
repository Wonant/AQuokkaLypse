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
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        // Skip sensors as they don't block vision
        if (fixture.isSensor()) {
            return 1; // Continue checking
        }

        // Skip the source body (the one casting the ray)
        if (fixture.getBody() == sourceBody) {
            return 1; // Continue checking
        }

        // Skip the target body (the one we're checking visibility for)
        if (fixture.getBody() == targetBody) {
            return 1; // Continue checking
        }

        // Skip any additional bodies that should be ignored
        for (Body body : ignoreBodies) {
            if (fixture.getBody() == body) {
                return 1; // Continue checking
            }
        }

        // Skip fixtures with specific user data (e.g., "player" or other special tags)
        if (fixture.getUserData() != null) {
            String userData = fixture.getUserData().toString();
            if (userData.contains("player") || userData.contains("ignore_vision")) {
                return 1; // Continue checking
            }
        }

        // If we hit something else (like a wall), vision is blocked
        blocked = true;
        return 0; // Stop checking
    }
}

