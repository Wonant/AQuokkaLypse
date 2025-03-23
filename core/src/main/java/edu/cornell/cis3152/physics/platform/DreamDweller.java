package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.CapsuleObstacle;
import edu.cornell.gdiac.physics2.Obstacle;

import java.util.HashMap;

public class DreamDweller extends Enemy {
    private final JsonValue data;

    /**
     * physics stuff
     */
    private float width;
    private float height;

    private float force;
    private float damping;
    private float max_speed;
    private float damage;
    private float alertRadius;
    private float stunResistance;
    private float fearEnergyAmount;

    private float movement;
    private boolean facingRight;
    private boolean isGrounded;
    private boolean stunned;
    private float stunDuration;
    private float currentStunTime;

    // Vision related properties
    private float visionAngle;
    private float visionSwingSpeed;
    private float visionSwingRange;
    private float currentVisionSwingTime;
    private boolean visionDirectionClockwise;

    // Sensor for ground detection
    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;

    // Caches for force calculation and affine transform for sprite flipping
    private final Vector2 forceCache = new Vector2();
    private final Affine2 flipCache = new Affine2();

    // Head and vision
    private Fixture visionSensor;
    private Fixture alertSensor;
    private float headOffset = 2.0f;

    /**
     * game logic stuff
     */
    private int entityID;
    private boolean active;
    private boolean awareOfPlayer;
    private float awarenessCooldown;
    private float currentAwarenessCooldown;

    private Path2 visionSensorOutline;
    private Path2 alertSensorOutline;

    // Texture for the DreamDweller
    private Texture2D dwellerTexture;
    private HashMap<DreamDweller, Sprite> visionCones3;
    private Path2 harvestOutline;

    public float getMovement() {
        return movement;
    }

    public void setMovement(float value) {
        movement = value;
        // Change facing direction based on input
        if (movement < 0) {
            facingRight = false;
        } else if (movement > 0) {
            facingRight = true;
        }
    }

    public float getVisionAngle() {
        return visionAngle;
    }

    public void setVisionAngle(float theta) {
        visionAngle = theta;
        float desiredAngle = theta * MathUtils.degreesToRadians; // Convert to radians
        headBody.setTransform(headBody.getPosition(), desiredAngle);
    }

    public void setStunned(boolean value) {
        stunned = value;
        if (stunned) {
            currentStunTime = stunDuration;
        }
    }

    public boolean isStunned() {
        return stunned;
    }

    public float getStunResistance() {
        return stunResistance;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean value) {
        isGrounded = value;
    }

    public float getForce() {
        return force;
    }

    public float getDamping() {
        return damping;
    }

    public float getMaxSpeed() {
        return max_speed;
    }

    public String getSensorName() {
        return sensorName;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void setAwareOfPlayer(boolean awareOfPlayer) {
        this.awareOfPlayer = awareOfPlayer;
        if (awareOfPlayer) {
            currentAwarenessCooldown = awarenessCooldown;
        }
    }

    public boolean isAwareOfPlayer() {
        return awareOfPlayer;
    }

    public float getDamage() {
        return damage;
    }

    public float getFearEnergyAmount() {
        return fearEnergyAmount;
    }

    public float getAlertRadius() {
        return alertRadius;
    }

    public Body getHeadBody() {
        return headBody;
    }

    /**
     * Alerts other enemies in the vicinity
     * Should be called by the GameMode or other manager when this enemy detects the player
     */
    public void alertOtherEnemies() {
        // This will be implemented in the game mode to find and alert other enemies
        // This method serves as a hook for that functionality
    }

    /**
     * Constructor for DreamDweller
     *
     * @param units  The ratio of pixels to Box2D units
     * @param data   The JSON value containing enemy properties
     * @param points The initial position of the enemy
     */
    public DreamDweller(float units, JsonValue data, float[] points) {
        this.data = data;
        // Read initial position and overall size from JSON.
        float x = points[0];
        float y = points[1];
        float s = data.getFloat("size");

        float size = s * units;

        width = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);

        float drawWidth = size / 2;
        float drawHeight = size;

        // Create the physics body as a capsule

        obstacle = new CapsuleObstacle(x, y, width, height);
        // Optionally set a tolerance for collision detection (from JSON debug info)
        JsonValue debugInfo = data.get("debug");
        ((CapsuleObstacle) obstacle).setTolerance(debugInfo.getFloat("tolerance", 0.5f));

        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("dweller");


        // Set debugging colors
        debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
        sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);

        // Initialize physics properties
        max_speed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        damage = data.getFloat("damage", 1.0f);
        alertRadius = data.getFloat("alertRadius", 100.0f);
        stunResistance = data.getFloat("stunResistance", 2.0f);
        fearEnergyAmount = data.getFloat("fearEnergy", 20.0f);
        stunDuration = data.getFloat("stunDuration", 3.0f);

        // Initialize vision properties
        visionSwingSpeed = data.getFloat("visionSwingSpeed", 0.5f);
        visionSwingRange = data.getFloat("visionSwingRange", 90.0f);
        currentVisionSwingTime = 0;
        visionDirectionClockwise = true;

        // Initialize state
        isGrounded = false;
        facingRight = true;
        stunned = false;
        currentStunTime = 0;
        visionAngle = 0;
        awarenessCooldown = data.getFloat("awarenessCooldown", 5.0f);
        currentAwarenessCooldown = 0;

        mesh.set(-drawWidth / 1.5f, -drawHeight / 1.6f, drawWidth * 1.5f, drawHeight * 1.5f);
    }
    public void setActiveTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "dream-dweller-active", Texture.class );
        this.setTexture(texture);
    }

    public void setStunTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "dream-dweller-inactive", Texture.class );
        this.setTexture(texture);
    }
    /**
     * Loads the texture for the DreamDweller
     */
    public void setTexture(Texture texture) {
        super.setTexture(texture);
    }

    /**
     * Creates a sensor fixture to detect ground contact.
     */
    public void createSensor() {
        // Position the sensor just below the physics body.
        Vector2 sensorCenter = new Vector2(0, -height / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density", 0);
        sensorDef.isSensor = true;

        // Get sensor configuration from JSON (e.g., size adjustments)
        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("shrink", 0) * width / 2.0f;
        float h = sensorjv.getFloat("height", 0);
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Create the sensor fixture on the physics body
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorName = "dweller_sensor";
        sensorFixture.setUserData(sensorName);

        // Create a debug outline for the sensor
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
        sensorShape.dispose();

        createHarvestSensor(width, height);
    }

    // hitbox
    public void createHarvestSensor(float harvestWidth, float harvestHeight) {
        float u = obstacle.getPhysicsUnits();

        // Create the harvest outline (for visualization)
        PathFactory factory = new PathFactory();
        harvestOutline = new Path2();

        float minDim = Math.min(harvestWidth, harvestHeight);
        float cornerRadius = (minDim / 2.0f) * u;

        factory.makeRoundedRect(
            -harvestWidth / 2 * u,
            -harvestHeight / 2 * u,
            harvestWidth * u,
            harvestHeight * u,
            cornerRadius,
            harvestOutline
        );

        // hitbox!!!
        PolygonShape harvestShape = new PolygonShape();
        harvestShape.setAsBox(harvestWidth/2, harvestHeight/2);

        FixtureDef harvestDef = new FixtureDef();
        harvestDef.shape = harvestShape;
        harvestDef.isSensor = true;

        Body body = obstacle.getBody();
        Fixture harvestFixture = body.createFixture(harvestDef);

        harvestFixture.setUserData("harvest_sensor");

        harvestShape.dispose();
    }

    /**
     * Creates a separate body for the head
     */
    public void createHeadBody() {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.DynamicBody;
        Vector2 pos = obstacle.getPosition().cpy().add(0, height / 2); // Head above body
        bdef.position.set(pos);

        headBody = obstacle.getBody().getWorld().createBody(bdef);
        CircleShape headShape = new CircleShape();
        headShape.setRadius(width / 3);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = headShape;
        fdef.density = 0.1f;
        fdef.isSensor = true; // Prevent physical collisions
        headBody.createFixture(fdef);
        headBody.setUserData(this);

        headShape.dispose();
    }

    /**
     * Attaches the head to the main body with a revolute joint
     */
    public void attachHead() {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.initialize(obstacle.getBody(), headBody, obstacle.getBody().getWorldCenter().add(0, height / 2));
        jointDef.enableMotor = true;
        jointDef.motorSpeed = 0;
        jointDef.maxMotorTorque = 100;
        headJoint = (RevoluteJoint) obstacle.getBody().getWorld().createJoint(jointDef);
    }

    /**
     * Creates vision sensors for the DreamDweller
     * - A wider vision cone than other enemies
     * - An alert radius to notify other enemies
     */
    public void createVisionSensor() {
        createHeadBody();
        attachHead();

        // Vision cone - wider than CuriosityCritter
        float coneWidth = 3.9f;  // Wider than CuriosityCritter
        float coneLength = 4.8f; // Longer than CuriosityCritter
        Vector2[] vertices = new Vector2[3];
        vertices[0] = new Vector2(-coneWidth / 2, coneLength);
        vertices[1] = new Vector2(coneWidth / 2, coneLength);
        vertices[2] = new Vector2(0, 0);
        PolygonShape visionShape = new PolygonShape();
        visionShape.set(vertices);

        FixtureDef visionDef = new FixtureDef();
        visionDef.shape = visionShape;
        visionDef.isSensor = true;

        visionSensor = headBody.createFixture(visionDef);
        visionSensor.setUserData("vision_sensor");

        PathFactory factory = new PathFactory();
        visionSensorOutline = new Path2();
        float u = obstacle.getPhysicsUnits();
        visionSensorOutline = factory.makeTriangle(
            -coneWidth / 2 * u, coneLength * u,
            coneWidth / 2 * u, coneLength * u,
            0, 0);


        // Create outline for alert radius
        alertSensorOutline = new Path2();
        factory.makeCircle(0, 0, alertRadius * u);

        visionShape.dispose();
    }

    /**
     * Applies forces to the physics body based on the current input.
     * This includes horizontal movement (with damping) and jumping impulses.
     */
    public void applyForce() {
        setMovement(0);

        if (obstacle.isActive()) {
            obstacle.setVX(0);
            obstacle.setVY(0);
        }

        Body body = obstacle.getBody();
        if (body != null) {
            body.setLinearVelocity(0, 0);
            body.setAngularVelocity(0);
        }
    }

    /**
     * Updates the DreamDweller's state
     *
     * @param dt The time elapsed since the last update
     */
    @Override
    public void update(float dt) {
        // Update stun state
        if (stunned) {
            currentStunTime -= dt;
            if (currentStunTime <= 0) {
                stunned = false;
            }
        }

        // Update awareness cooldown
        if (awareOfPlayer && !stunned) {
            currentAwarenessCooldown -= dt;
            if (currentAwarenessCooldown <= 0) {
                awareOfPlayer = false;
            }
        }

        // Update vision swing
        if (!awareOfPlayer && !stunned) {
            // Update the vision swing time
            currentVisionSwingTime += dt;

            // Calculate the new vision angle based on a sine wave
            float swingProgress = (float) Math.sin(currentVisionSwingTime * visionSwingSpeed);
            float newAngle = swingProgress * visionSwingRange;

            // Set the new vision angle
            setVisionAngle(newAngle);
        } else if (awareOfPlayer && !stunned) {
            // When aware of player, vision tracks the player's position
            // This would be implemented in the game mode where it has access to player position
        }

        applyForce();
        super.update(dt);
    }

    /**
     * Draws the DreamDweller sprite
     *
     * @param batch The sprite batch used for drawing
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (facingRight) {
            flipCache.setToScaling(1, 1);
        } else {
            flipCache.setToScaling(-1, 1);
        }

        if (obstacle != null && mesh != null && sprite != null) {
            float scaleFactor = 1.5f; // Increase sprite size
            float x = obstacle.getX();
            float y = obstacle.getY();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preScale(scaleFactor, scaleFactor);
            transform.preRotate((float) ((double) (a * 180.0F) / Math.PI));
            transform.preTranslate(x * u, y * u);

            batch.setTextureRegion(sprite);
            batch.drawMesh(mesh, transform, false);
            batch.setTexture((Texture) null);
        }
    }

    /**
     * Draws the debug outlines for the physics body and sensors
     *
     * @param batch The sprite batch used for drawing
     */
    @Override
    public void drawDebug(SpriteBatch batch) {
        super.drawDebug(batch);

        if (sensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(sensorColor);

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            // Prepare a transformation matrix for proper sensor positioning
            transform.idt();
            transform.preRotate((float) (a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            batch.outline(sensorOutline, transform);
        }

        if (visionSensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.PURPLE); // Different color for DreamDweller vision

            Vector2 headPos = headBody.getPosition();
            float headAngleDeg = headBody.getAngle() * MathUtils.radiansToDegrees;
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate(headAngleDeg);
            transform.preTranslate(headPos.x * u, headPos.y * u);

            batch.outline(visionSensorOutline, transform);
            batch.setColor(Color.WHITE);
        }

        if (alertSensorOutline != null && awareOfPlayer) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.RED); // Alert radius when active

            Vector2 headPos = headBody.getPosition();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preTranslate(headPos.x * u, headPos.y * u);

            batch.outline(alertSensorOutline, transform);
            batch.setColor(Color.WHITE);
        }
        if (harvestOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.BLUE);

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            batch.outline(harvestOutline, transform);

            batch.setColor(Color.WHITE);
        }
    }
}


