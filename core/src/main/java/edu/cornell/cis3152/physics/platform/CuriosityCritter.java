package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
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
import edu.cornell.gdiac.physics2.*;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

public class CuriosityCritter extends Enemy {
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



    private int jumpCooldown;
    private boolean isJumping;
    private int shootCooldown;
    private boolean isGrounded;
    private boolean isShooting;

    // Sensor for ground detection
    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;

    // why use forces? why dynamic body? these guys shouldn't have any physical collisions
    // Caches for force calculation and affine transform for sprite flipping
    private final Vector2 velocityCache = new Vector2();
    private final Vector2 forceCache = new Vector2();
    private final Affine2 flipCache = new Affine2();

    private Fixture visionSensor;
    private Fixture followSensor;
    private float headOffset = 2.0f;
    private Fixture walkSensor;

    private PathFactory factory = new PathFactory();


    /** game logic stuff */

    // each npc ai character will have a unique one
    private int entityID;
    // indicates if this critter is interactable with or not. once harvested critteres are inactive
    private boolean active;
    // where the critter is looking, 0 is relative down of the critter's central location, iterates clockwise
    private float visionAngle;

    // should be seconds in how long it takes for the critter to reset from aware of player to idle
    private float awarenessCooldown;

    /** used to draw the harvest hitbox, which differs from the physical hitbox */
    private Path2 harvestOutline;

    // in radians
    private float followAngle;
    private Path2 visionTriggerOutline; //
    private Path2 visionFollowOutline;
    private Path2 walkSensorOutline;
    private Path2 wallSensorOutline;
    private Vector2 debugLookStart = new Vector2();
    private Vector2 debugLookEnd = new Vector2();

    private boolean hasShard;
    // where to move the shard(ideally) in world positions if this critter is carrying it
    // movement target in world coordinates
    private Vector2 worldTarget;

    public Shard heldShard;

    public boolean playerInFollowRange = false;
    private boolean safeToWalk;







    public float getVisionAngle() {
        return visionAngle;
    }

    public void setVisionAngle(float theta) {
        float desiredAngle = theta * MathUtils.degreesToRadians; // Convert to radians
        headBody.setTransform(headBody.getPosition(), desiredAngle);
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

    public boolean getSafeToWalk() {
        return safeToWalk;
    }


    public CuriosityCritter(float units, JsonValue data, float[] points, PlatformScene scene) {
        super();
        this.data = data;
        // Read initial position and overall size from JSON.
        float x = points[0];
        float y = points[1];
        float s = data.getFloat("size");

        this.scene = scene;

        float size = s * units;

        width  = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);

        float drawWidth  = size/2;
        float drawHeight = size;

        // may want to change what kind of physical obstacle this is
        obstacle = new BoxObstacle(x, y, width, height);
        // Optionally set a tolerance for collision detection (from JSON debug info)
        JsonValue debugInfo = data.get("debug");
        //((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );



        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setBodyType(BodyDef.BodyType.DynamicBody);
        obstacle.setName("critter");

        // Set debugging colors
        debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
        sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);

        max_speed   = data.getFloat("maxspeed", 0);
        damping    = data.getFloat("damping", 0);
        force      = data.getFloat("force", 0);
        jump_force = data.getFloat("jump_force", 0);

        isGrounded  = true;
        isShooting  = false;
        isJumping   = false;
        facingRight   = true;
        jumpCooldown = 0;
        shootCooldown = 0;
        //deg
        visionAngle = 0;

        stepRayLength = height;
        enemyVisionRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.STAIR_CHECK, stepRayLength);


        mesh.set(-drawWidth/1.5f, -drawHeight/1.3f, drawWidth*1.5f, drawHeight*1.5f);


    }



    public void setActiveTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "curiosity-critter-active", Texture.class );
        this.setTexture(texture);
    }

    public void setStunTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "curiosity-critter-inactive", Texture.class );
        this.setTexture(texture);
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

        sensorFixture.setUserData(sensorName);

        // Create a debug outline for the sensor so you can see it in debug mode
        float u = obstacle.getPhysicsUnits();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
        sensorShape.dispose();

        createHarvestSensor(width, height);
    }

    public void createHarvestSensor(float harvestWidth, float harvestHeight) {
        float u = obstacle.getPhysicsUnits();

        // Create the harvest outline (for visualization)

        harvestOutline = new Path2();

        factory.makeRoundedRect(-harvestWidth/2 * u, -harvestHeight/2 * u,
            harvestWidth * u, harvestHeight * u,
            (harvestWidth/2) * u, harvestOutline);

        // Create a sensor fixture for the harvest area
        // This will be used for collision detection without physical response
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

    // traversal sensors


    // vision following

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

    public void attachHead() {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.initialize(obstacle.getBody(), headBody, obstacle.getBody().getWorldCenter().add(0, height / 2));
        jointDef.enableMotor = true;
        jointDef.motorSpeed = 0;
        jointDef.maxMotorTorque = 100;
        headJoint = (RevoluteJoint) obstacle.getBody().getWorld().createJoint(jointDef);
    }

    public void createVisionSensor() {
        createHeadBody();
        attachHead();
        float u = obstacle.getPhysicsUnits();
        Vector2[] vertices = new Vector2[3];

        PolygonShape visionShape = new PolygonShape();

        FixtureDef visionDef = new FixtureDef();

        // follow sensor
        float coneWidth = 8.4f;
        float coneLength = 6f;

        vertices[0] = new Vector2((-coneWidth)/2, coneLength);
        vertices[1] = new Vector2((coneWidth)/2, coneLength);
        vertices[2] = new Vector2(0, -3f);

        visionShape.set(vertices);
        visionDef.shape = visionShape;
        visionDef.isSensor = true;

        followSensor = headBody.createFixture(visionDef);
        followSensor.setUserData("follow_sensor");
        visionFollowOutline = new Path2();
        visionFollowOutline = factory.makeTriangle(
            -coneWidth/2 * u, coneLength * u,
            coneWidth/2 * u, coneLength * u,
            0, -3f);

    }


    public void setHorizontalVelocity() {
        if (!obstacle.isActive()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        float vy = obstacle.getVY();
        Body body = obstacle.getBody();


        if (getMovement() == 0f) {
            velocityCache.set(0, vy);
            body.setLinearVelocity(velocityCache);
        }

        if (Math.abs(vx) >= getMaxSpeed()) {
            obstacle.setVX(Math.signum(vx) * getMaxSpeed());
        } else {
            velocityCache.set(getMovement(), vy);
            body.setLinearVelocity(velocityCache);
        }


    }

    /**
     * Applies forces to the physics body based on the current input.
     * This includes horizontal movement (with damping) and jumping impulses.
     */
    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        World world = getObstacle().getBody().getWorld();

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        Body body = obstacle.getBody();

        //horizontal input will be determined by velocity changes

        if (getMovement() == 0f) {
            // Force a full stop horizontally
            body.setLinearVelocity(0, body.getLinearVelocity().y);
        } else {
            // Otherwise, set the horizontal velocity toward getMovement()
            if (Math.abs(vx) >= getMaxSpeed()) {
                obstacle.setVX(Math.signum(vx) * getMaxSpeed());
            } else {
                setHorizontalVelocity();
            }
        }

        // jumping can be force

        if (isJumping()) {
            forceCache.set(0, jump_force);
            body.applyLinearImpulse(forceCache, pos, true);
        }
    }

    public boolean isPlatformStep(World world, float raylength) {
        Vector2 start = (isFacingRight()) ?
            obstacle.getBody().getPosition().cpy().add(width/2 + 0.1f, height/2) :
            obstacle.getBody().getPosition().cpy().add(-width/2 - 0.1f, height/2);
        Vector2 end = start.cpy().add(0, -raylength);


        debugRayStart = start;
        debugRayEnd = end;


        world.rayCast(enemyVisionRaycast, start, end);

        if (enemyVisionRaycast.getHitFixture() == null) {
            return false;
        } else if (!enemyVisionRaycast.fixtureIsStair) {
            return false;
        } else {
            Vector2 stairHit = new Vector2(enemyVisionRaycast.getHitPoint());

            if (isGrounded && Math.abs(movement) > 0) {
                float targetCenterY = stairHit.y + height/2;
                Body body = obstacle.getBody();
                Vector2 pos = body.getPosition();
                body.setTransform(stairHit.x, targetCenterY, body.getAngle());

                debugRayEnd = stairHit;
            }

        }

        enemyVisionRaycast.reset();

        return true;
    }

    public float getFollowAngle() {
        return followAngle;
    }

    public void updateFollowSensor(Player player) {
        Vector2 enemyPos = obstacle.getPosition();
        Vector2 playerPos = player.getObstacle().getPosition();
        Vector2 visionRef = new Vector2(enemyPos.x, enemyPos.y + 5.0f);
        visionRef.sub(enemyPos).nor();
        playerPos.sub(enemyPos).nor();
        followAngle = MathUtils.atan2(playerPos.y,playerPos.x) - MathUtils.atan2(visionRef.y,visionRef.x);
        headBody.setTransform(headBody.getPosition(), followAngle);
    }

    public void lookForPlayer() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();


        Vector2 pos = obstacle.getPosition();
        float rayLength = 4f;
        float followSensorAngle = MathUtils.atan2(player.getObstacle().getPosition().y - pos.y,
            player.getObstacle().getPosition().x - pos.x);

        Vector2 start = (facingRight) ? new Vector2(pos.x + width/2, pos.y + height/4) :
            new Vector2(pos.x - width/2, pos.y + height/4);
        //Vector2 end = (facingRight) ? new Vector2(pos.x + width + rayLength, pos.y + height/4) :
            new Vector2(pos.x - width - rayLength, pos.y + height/4);
        Vector2 end = new Vector2(
            pos.x + rayLength * MathUtils.cos(followSensorAngle),
            pos.y + rayLength * MathUtils.sin(followSensorAngle)
        );

        EnemyVisionRaycast playerRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.PLAYER_CHECK, 4f);
        world.rayCast(playerRaycast, start, end);

        debugLookStart.set(start);
        debugLookEnd.set(end);

        if (playerRaycast.getHitPlayer() != null) {
            setAwareOfPlayer(true);
            debugLookEnd.set(playerRaycast.getHitPoint());
            System.out.println("Seen the player");

        } else if (playerRaycast.getHitFixture() != null) {
            debugLookEnd.set(playerRaycast.getHitPoint());
        }

        playerRaycast.reset();
    }

    public boolean canContinue() {
        World world = obstacle.getBody().getWorld();
        Vector2 pos = obstacle.getPosition();

        float groundRayLength = stepRayLength * 4.5f;
        float wallRayLength = width * 0.5f;

        Vector2 groundStart = (facingRight) ? new Vector2(pos.x + width, pos.y) :
           new Vector2(pos.x - width, pos.y);
        Vector2 wallStart = (facingRight) ? new Vector2(pos.x + width/2, pos.y) :
            new Vector2(pos.x - width/2, pos.y);

        Vector2 groundEnd = groundStart.cpy().add(0, -groundRayLength);
        EnemyVisionRaycast wallVisionRaycast =
            new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.WALL_CHECK, stepRayLength);
        EnemyVisionRaycast groundVisionRaycast =
            new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.FALL_CHECK, stepRayLength);
        world.rayCast(groundVisionRaycast, groundStart, groundEnd);
        boolean groundExists = (groundVisionRaycast.getHitFixture() != null);
        groundVisionRaycast.reset();

        Vector2 wallEnd = new Vector2();
        if (isFacingRight()) {
            wallEnd.set(wallStart).add(wallRayLength, 0);
        } else {
            wallEnd.set(wallStart).sub(wallRayLength, 0);
        }
        world.rayCast(wallVisionRaycast, wallStart, wallEnd);
        boolean wallExists = (wallVisionRaycast.getHitFixture() != null);
        wallVisionRaycast.reset();

        return groundExists && !wallExists;
    }


    public void setTarget() {

    }

    public void giveShard(Shard shard) {
        if (heldShard != null) {
            dropShard();
        }
        heldShard = shard;
        hasShard = true;

        // compute target

    }

    public Shard dropShard() {
        Shard s = heldShard;
        heldShard = null;
        return s;
    }


    public void targetShard() {

    }





    @Override
    public void update(float dt) {
        lookForPlayer();
        if (isPlatformStep(scene.world, stepRayLength)) {
            System.out.println("Critter's seen a step");
        }

        if (!canContinue()) {
            safeToWalk = false;
            setMovement(0);
            applyForce();
        } else {
            safeToWalk = true;
        }
        if (isAwareOfPlayer()) {
            updateFollowSensor(scene.getAvatar());
        }
        if (isJumping()) {
            jumpCooldown = jumpLimit;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
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
        if (obstacle != null && mesh != null) {
            float scaleFactor = 1.4f; // Increase sprite size by 20%
            float x = obstacle.getX();
            float y = obstacle.getY();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preScale(scaleFactor, scaleFactor); // Scale up sprite only
            transform.preRotate((float) ((double) (a * 180.0F) / Math.PI));
            transform.preTranslate(x * u, y * u);

            batch.setTextureRegion(sprite);
            batch.drawMesh(mesh, transform, false);
            batch.setTexture((Texture) null);
        }
    }


    public void drawRayDebug(SpriteBatch batch) {
        if (debugRayStart != null && debugRayEnd != null) {
            float u = obstacle.getPhysicsUnits();
            Vector2 localStart = new Vector2(debugRayStart).sub(obstacle.getPosition());
            Vector2 localEnd = new Vector2(debugRayEnd).sub(obstacle.getPosition());

            PathFactory factory = new PathFactory();
            Path2 rayOutline = new Path2();
            factory.makeLine(localStart.x * u, localStart.y * u,
                localEnd.x * u, localEnd.y * u, rayOutline);

            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.PURPLE);
            transform.idt();
            float a = obstacle.getAngle();
            Vector2 p = obstacle.getPosition();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);
            batch.outline(rayOutline, transform);
        }
        if (debugLookEnd != null && debugLookStart != null) {
            float u = obstacle.getPhysicsUnits();
            Vector2 localStart = new Vector2(debugLookStart).sub(obstacle.getPosition());
            Vector2 localEnd = new Vector2(debugLookEnd).sub(obstacle.getPosition());

            PathFactory factory = new PathFactory();
            Path2 rayOutline = new Path2();
            factory.makeLine(localStart.x * u, localStart.y * u,
                localEnd.x * u, localEnd.y * u, rayOutline);

            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.PURPLE);
            transform.idt();
            float a = obstacle.getAngle();
            Vector2 p = obstacle.getPosition();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);
            batch.outline(rayOutline, transform);
        }
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
        if (visionTriggerOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.GREEN);

            Vector2 headPos = headBody.getPosition();
            float headAngleDeg = headBody.getAngle() * MathUtils.radiansToDegrees;
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate(headAngleDeg);
            transform.preTranslate(headPos.x * u, headPos.y * u);

            batch.outline(visionTriggerOutline, transform);
        }
        if (visionFollowOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.LIME);

            Vector2 headPos = headBody.getPosition();
            float headAngleDeg = headBody.getAngle() * MathUtils.radiansToDegrees;
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate(headAngleDeg);
            transform.preTranslate(headPos.x * u, headPos.y * u);

            batch.outline(visionFollowOutline, transform);
        }
        if (walkSensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.RED);

            Vector2 headPos = headBody.getPosition();
            float headAngleDeg = headBody.getAngle() * MathUtils.radiansToDegrees;
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate(headAngleDeg);
            transform.preTranslate(headPos.x * u, headPos.y * u);

            batch.outline(walkSensorOutline, transform);
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
        if (wallSensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.PINK);
            transform.idt();
            float u = obstacle.getPhysicsUnits();

            transform.preTranslate(obstacle.getX() * u, obstacle.getY() * u);
            batch.outline(wallSensorOutline, transform);
            batch.setColor(Color.WHITE);
        }
        drawRayDebug(batch);
        drawFallCheckDebug(batch);
    }

    public void drawFallCheckDebug(SpriteBatch batch) {
        // Get the world and critter position.
        World world = obstacle.getBody().getWorld();
        Vector2 pos = obstacle.getBody().getPosition();

        // Use the same ray length as in canContinue.
        float groundRayLength = stepRayLength * 4.5f;
        Vector2 groundStart = new Vector2();
        if (isFacingRight()) {
            groundStart.set(pos.x + width, pos.y);
        } else {
            groundStart.set(pos.x - width, pos.y);
        }
        // Ray goes downward from groundStart.
        Vector2 groundEnd = groundStart.cpy().add(0, -groundRayLength);

        // Convert world coordinates to screen units using your physics unit conversion.
        float u = obstacle.getPhysicsUnits();
        Path2 fallRayPath = new Path2();
        PathFactory factory = new PathFactory();
        factory.makeLine(groundStart.x * u, groundStart.y * u,
            groundEnd.x * u, groundEnd.y * u, fallRayPath);

        // Draw the fall check ray in a distinct color (yellow in this example)
        batch.setTexture(Texture2D.getBlank());
        batch.setColor(Color.PURPLE);
        // For simplicity, we use an identity transform here.
        Affine2 transform = new Affine2();
        transform.idt();
        batch.outline(fallRayPath, transform);
    }


    public Vector2 getWorldTarget() {
        return worldTarget;
    }

    public Body getHeadBody() {
        return headBody;
    }
}
