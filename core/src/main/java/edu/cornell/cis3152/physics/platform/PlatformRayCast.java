package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import edu.cornell.cis3152.physics.ObstacleGroup;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class PlatformRayCast implements RayCastCallback {
    private Fixture platformFixture = null;
    private float closestFraction = 1f;
    private Vector2 hitPoint = new Vector2();

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        Object userData = fixture.getBody().getUserData();
        System.out.println(fixture.getBody().getUserData());
        if (userData instanceof Surface) {
            Surface surface = (Surface)userData;
            String obstacleName = surface.getObstacle().getName();
            if (obstacleName != null && obstacleName.startsWith("platform")) {
                if (surface.isShadowed()) {
                    platformFixture = fixture;
                    closestFraction = fraction;
                    hitPoint.set(point);
                }
                return 0; // Return 0 to stop the raycast immediately at the first platform
            }
        }
        return 1; // Continue searching
    }

    public Fixture getPlatformFixture() {
        return platformFixture;
    }

    public float getClosestFraction() {
        return closestFraction;
    }

    public Vector2 getHitPoint() {
        return hitPoint;
    }
}
