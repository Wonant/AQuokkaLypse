package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;


import com.badlogic.gdx.graphics.g2d.Sprite;
import edu.cornell.cis3152.physics.AIControllerManager;
import edu.cornell.cis3152.physics.ObstacleGroup;

import java.util.HashMap;
import java.util.Iterator;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.cis3152.physics.InputController;
//import edu.cornell.cis3152.physics.rocket.Box;
import edu.cornell.gdiac.assets.AssetDirectory;
//import edu.cornell.cis3152.physics.PhysicsScene;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.physics2.*;

public class RaycastVision {
    private static final int RAY_COUNT = 10; // Number of rays in the cone
    private static final float VISION_ANGLE = 60f; // Field of view in degrees
    private static final float MAX_VISION_DISTANCE = 4.5f; // Max distance the vision can reach
    private static final float DEFAULT_DIRECTION_ANGLE = 0f;

    private Body sourceBody; // Body the vision cone originates from (head)
    private World world;
    private Array<Vector2> rayHits; // Store ray hit points for drawing
    private float physicsUnits; // Scaling factor
    private Player player; // Reference to player for detection
    private boolean playerDetected;

    // Custom raycast callback to find closest hit point
    private class RaycastCallback implements RayCastCallback {
        private Vector2 hitPoint = new Vector2();
        private boolean hasHit = false;
        private float closestFraction = 1f;
        private Object hitObject = null;

        @Override
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            // Ignore sensors
            if (fixture.isSensor()) {
                return -1;
            }

            // Check if the ray hit a player or a surface
            Object userData = fixture.getBody().getUserData();
            if (userData instanceof Surface || userData instanceof Player) {
                if (fraction < closestFraction) {
                    closestFraction = fraction;
                    hitPoint.set(point);
                    hasHit = true;
                    hitObject = userData;
                }
            }

            // Continue to find closer objects
            return 1;
        }

        public boolean hasHit() {
            return hasHit;
        }

        public Vector2 getHitPoint() {
            return hitPoint;
        }

        public float getClosestFraction() {
            return closestFraction;
        }

        public Object getHitObject() {
            return hitObject;
        }

        public void reset() {
            hasHit = false;
            closestFraction = 1f;
            hitObject = null;
        }
    }

    public RaycastVision(Body sourceBody, World world, float physicsUnits, Player player) {
        this.sourceBody = sourceBody;
        this.world = world;
        this.physicsUnits = physicsUnits;
        this.player = player;
        this.rayHits = new Array<>(RAY_COUNT);

        // Initialize hit points array
        for (int i = 0; i < RAY_COUNT; i++) {
            rayHits.add(new Vector2());
        }
    }

    public boolean update() {
        Vector2 sourcePosition = sourceBody.getPosition();
        float sourceAngle = sourceBody.getAngle(); // In radians
        playerDetected = false;

        // Cast rays from the head position in a cone
        float halfVisionAngle = VISION_ANGLE * 0.5f * MathUtils.degreesToRadians;

        RaycastCallback callback = new RaycastCallback();

        // Cast rays across the vision cone
        for (int i = 0; i < RAY_COUNT; i++) {
            callback.reset();

            // Calculate angle for this ray, distributed evenly across the cone
            // For an N-ray cone, we need N-1 segments, hence RAY_COUNT - 1
            float angleOffset = -halfVisionAngle + (i / (float)(RAY_COUNT - 1)) * (VISION_ANGLE * MathUtils.degreesToRadians);
            float rayAngle = sourceAngle + angleOffset;

            // Calculate ray end point using the angle
            Vector2 rayDirection = new Vector2(
                MathUtils.cos(rayAngle),
                MathUtils.sin(rayAngle)
            );

            Vector2 rayEnd = new Vector2(
                sourcePosition.x + rayDirection.x * MAX_VISION_DISTANCE,
                sourcePosition.y + rayDirection.y * MAX_VISION_DISTANCE
            );

            // Perform the raycast
            world.rayCast(callback, sourcePosition, rayEnd);

            // Store hit point (either obstacle hit or maximum distance)
            if (callback.hasHit()) {
                rayHits.set(i, callback.getHitPoint().cpy());

                // Check if the hit object is the player
                if (callback.getHitObject() instanceof Player) {
                    playerDetected = true;
                }
            } else {
                // No hit, use the end point
                rayHits.set(i, rayEnd.cpy());
            }
        }

        return playerDetected;
    }

    public void draw(SpriteBatch batch, Color color) {
        // Draw each ray as a line
        batch.setTexture(Texture2D.getBlank());
        batch.setColor(color);

        Vector2 sourcePosition = sourceBody.getPosition();
        float srcX = sourcePosition.x * physicsUnits;
        float srcY = sourcePosition.y * physicsUnits;

        // First, draw a filled polygon for the vision cone
        // We'll use triangles from the source to adjacent hit points
        Color fillColor = new Color(color);
        fillColor.a = 0.15f; // Semi-transparent
        batch.setColor(fillColor);

        PathFactory factory = new PathFactory();

        // Draw triangles between source and consecutive hit points
        for (int i = 0; i < rayHits.size - 1; i++) {
            Vector2 hit1 = rayHits.get(i);
            Vector2 hit2 = rayHits.get(i + 1);

            float hit1X = hit1.x * physicsUnits;
            float hit1Y = hit1.y * physicsUnits;
            float hit2X = hit2.x * physicsUnits;
            float hit2Y = hit2.y * physicsUnits;

            Path2 triangle = factory.makeTriangle(srcX, srcY, hit1X, hit1Y, hit2X, hit2Y);
            batch.outline(triangle);

        }

        // Then draw the ray lines
        batch.setColor(color);
        for (int i = 0; i < rayHits.size; i++) {
            Vector2 hit = rayHits.get(i);
            float hitX = hit.x * physicsUnits;
            float hitY = hit.y * physicsUnits;

            Path2 rayPath = factory.makeLine(srcX, srcY, hitX, hitY);
            batch.outline(rayPath);
        }
    }

    public boolean isPlayerDetected() {
        return playerDetected;
    }
}
