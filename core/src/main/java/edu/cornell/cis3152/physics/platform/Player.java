package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;

public class Player extends ObstacleSprite {
    /** Json constants for parameters */
    private final JsonValue data;

    /** physics stuff */
    private float width;
    private float height;

    private float force;
    private float damping;
    private float max_speed;
    private float jump_force;
    private int jumpLimit;
    private int shotLimit;

    private float movement;
    private boolean facingRight;
    private int jumpCooldown;
    private boolean isJumping;
    private int shootCooldown;
    private boolean isGrounded;
    private boolean isShooting;

    // Sensor for ground detection
    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;

    // Caches for force calculation and affine transform for sprite flipping
    private final Vector2 forceCache = new Vector2();
    private final Affine2 flipCache = new Affine2();

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

    public boolean isShooting() {
        return isShooting && shootCooldown <= 0;
    }

    public void setShooting(boolean value) {
        isShooting = value;
    }

    public boolean isJumping() {
        return isJumping && isGrounded && jumpCooldown <= 0;
    }

    public void setJumping(boolean value) {
        isJumping = value;
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


    public Player(float units, JsonValue data) {
        this.data = data;

        // Read initial position and overall size from JSON.
        float x = data.get("pos").getFloat(0);
        float y = data.get("pos").getFloat(1);
        float s = data.getFloat("size");

        float size = s * units;

        width  = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);

        float drawWidth  = size * 2.2f;
        float drawHeight = size * 2.0f;

        // may want to change what kind of physical obstacle this is
        obstacle = new CapsuleObstacle(x, y, width, height);
        // Optionally set a tolerance for collision detection (from JSON debug info)
        JsonValue debugInfo = data.get("debug");
        ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("player");

        // Set debugging colors
        debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
        sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);

        max_speed   = data.getFloat("maxspeed", 0);
        damping    = data.getFloat("damping", 0);
        force      = data.getFloat("force", 0);
        jump_force = data.getFloat("jump_force", 0);

        isGrounded  = false;
        isShooting  = false;
        isJumping   = false;
        facingRight   = true;
        jumpCooldown = 0;
        shootCooldown = 0;


        mesh.set(-drawWidth/2.0f, -drawHeight/1.8f, drawWidth, drawHeight);
    }

    /**
     * Creates a sensor fixture to detect ground contact.
     * This sensor prevents double-jumping by detecting when the player is on the ground.
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
        sensorName = "player_sensor";
        sensorFixture.setUserData(sensorName);

        // Create a debug outline for the sensor so you can see it in debug mode
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }

    /**
     * Applies forces to the physics body based on the current input.
     * This includes horizontal movement (with damping) and jumping impulses.
     */
    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        Body body = obstacle.getBody();

        // Apply damping when no horizontal input is provided
        if (getMovement() == 0f) {
            forceCache.set(-getDamping() * vx, 0);
            body.applyForce(forceCache, pos, true);
        }

        // Clamp horizontal velocity to the maximum speed
        if (Math.abs(vx) >= getMaxSpeed()) {
            obstacle.setVX(Math.signum(vx) * getMaxSpeed());
        } else {
            forceCache.set(getMovement(), 0);
            body.applyForce(forceCache, pos, true);
        }

        // Apply a vertical impulse if a jump is initiated
        if (isJumping()) {
            forceCache.set(0, jump_force);
            body.applyLinearImpulse(forceCache, pos, true);
        }
    }

    /**
     * Updates the player state each frame.
     * This includes handling cooldowns for jumping and shooting.
     *
     * @param dt  The time in seconds since the last frame.
     */
    @Override
    public void update(float dt) {
        // Update jump cooldown
        if (isJumping()) {
            jumpCooldown = jumpLimit;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
        }

        // Update shooting cooldown
        if (isShooting()) {
            shootCooldown = shotLimit;
        } else {
            shootCooldown = Math.max(0, shootCooldown - 1);
        }

        super.update(dt);
    }

    /**
     * Draws the player sprite.
     * The sprite is flipped horizontally if the player is facing left.
     *
     * @param batch  The sprite batch used for drawing.
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (facingRight) {
            flipCache.setToScaling(1, 1);
        } else {
            flipCache.setToScaling(-1, 1);
        }
        super.draw(batch, flipCache);
    }

    /**
     * Draws the debug outlines for the physics body and sensor.
     *
     * @param batch  The sprite batch used for drawing.
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
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            batch.outline(sensorOutline, transform);
        }
    }
}
