package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

public class PlayerVisionRaycast implements RayCastCallback {
    public enum VisionMode {STAIR_CHECK}

    private Vector2 hitNormal = new Vector2();
    private VisionMode mode;
    private Fixture hitFixture = null;
    private Vector2 hitPoint = new Vector2();
    private float closestFraction = 1f;
    public boolean fixtureIsStair;

    private Vector2 posAboveStair = new Vector2();

    private float stepHeightThreshold;

    public PlayerVisionRaycast(VisionMode mode, float stepHeightThreshold) {
        this.mode = mode;
        this.stepHeightThreshold = stepHeightThreshold;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        Object userData = fixture.getBody().getUserData();

        if (mode == VisionMode.STAIR_CHECK) {
            if (userData instanceof Surface) {
                Surface surface = (Surface) userData;
                String name = surface.getObstacle().getName();
                if (point.y - surface.getObstacle().getY() <= stepHeightThreshold && name.startsWith("stair")) {
                    hitNormal.set(normal).nor();
                    posAboveStair.set(point).add(normal.cpy().nor());
                    hitFixture = fixture;
                    closestFraction = fraction;
                    fixtureIsStair = true;
                    hitPoint.set(point);
                    return 0;
                }
            }
        }
        return 1f;
    }

    public void setMode(VisionMode mode) {
        this.mode = mode;
    }

    public void reset() {
        hitFixture = null;
        hitPoint.set(0, 0);
        closestFraction = 1f;
        hitNormal.set(0,0);
    }

    public Vector2 getHitNormal() {
        return hitNormal;
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
