package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private boolean isGrounded;

    // Sensor for ground detection
    private Path2 sensorOutline;
    private Color sensorColor;

    // why use forces? why dynamic body? these guys shouldn't have any physical collisions
    // Caches for force calculation and affine transform for sprite flipping
    private final Vector2 velocityCache = new Vector2();
    private final Affine2 flipCache = new Affine2();

    private PathFactory factory = new PathFactory();


    /** game logic stuff */
    private Vector2 debugLookStart = new Vector2();
    private Vector2 debugLookEnd = new Vector2();
    private Vector2 debugFollowStart = new Vector2();
    private Vector2 debugFollowEnd = new Vector2();
    private Vector2 debugGroundStart = new Vector2();
    private Vector2 debugGroundEnd = new Vector2();

    private boolean hasShard;
    // where to move the shard(ideally) in world positions if this critter is carrying it
    // movement target in world coordinates
    private Vector2 worldTarget;
    public Shard heldShard;
    public boolean inMoveTask;

    public boolean playerInFollowRange = false;
    private boolean safeToWalk;

    private int climbCounter = 0;
    private final int CLIMB_DURATION = 11;

    private Animator walkSprite;
    private Animator turnSprite;
    private Animator alertSprite;
    private Animator attackSprite;
    private Animator shardWalkSprite;
    private Animator stunnedSprite;

    private AnimationState animationState;
    private boolean inAttackAnimation = false;
    private boolean inTurnAnimation = false;
    private boolean inStunAnimation = false;
    private boolean isChasing = false;
    private boolean lastFacingRight = true;

    private boolean turning = false;
    private float turnCooldown = 0f;
    private static final float TURN_COOLDOWN_TIME = 0.3f;


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

    public boolean isFacingRight() {
        return facingRight;
    }

    public boolean getSafeToWalk() {
        return safeToWalk;
    }

    private enum AnimationState {
        WALK,
        TURN,
        ALERT,
        ATTACK,
        SHARD_WALK,
        STUN
    }

    public void createAnimators(Texture texture) {
        walkSprite       = new Animator(texture, 8, 20, 0.06f, 154, 0, 14);       // Walk
        turnSprite       = new Animator(texture, 8, 20, 0.06f, 154, 15, 50);      // Turn
        alertSprite      = new Animator(texture, 8, 20, 0.06f, 154, 51, 82);      // Alert
        attackSprite     = new Animator(texture, 8, 20, 0.06f, 154, 83, 111);     // Attack Start
        shardWalkSprite  = new Animator(texture, 8, 20, 0.06f, 154, 112, 129);// Walk with shard
        stunnedSprite = new Animator(texture, 8, 20, 0.06f, 154, 130, 153);
    }

    public CuriosityCritter(float units, JsonValue data, float[] points, PlatformScene scene, MessageDispatcher dispatcher) {
        super(dispatcher);
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

        obstacle.setDensity(100000);
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

        isGrounded  = true;
        facingRight   = true;
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

        sensorFixture.setUserData("critter_body");

        // Create a debug outline for the sensor so you can see it in debug mode
        float u = obstacle.getPhysicsUnits();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
        sensorShape.dispose();

    }

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

    }

    public boolean isPlatformStep(World world, float raylength) {
        if (climbCounter < CLIMB_DURATION) {
            climbCounter++;
            return false;
        }
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
            debugRayEnd = stairHit;
            if (isGrounded && Math.abs(movement) > 0) {

            }
        }

        enemyVisionRaycast.reset();
        climbCounter = CLIMB_DURATION;
        return true;
    }

    /**
     * Performs a direct raycast to check if the player is visible from the enemy.
     * This is used when the player enters the follow sensor area.
     *
     *
     * @return true if player is directly visible, false if blocked by obstacles
     */
    public boolean checkFollowRaycast() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();
        Vector2 pos = obstacle.getPosition();

        float rayLength = 7f;
        float followSensorAngle = MathUtils.atan2(player.getObstacle().getPosition().y - pos.y,
            player.getObstacle().getPosition().x - pos.x);

        Vector2 start = (facingRight) ? new Vector2(pos.x + width/2, pos.y + height/4) :
            new Vector2(pos.x - width/2, pos.y + height/4);
        new Vector2(pos.x - width - rayLength, pos.y + height/4);
        Vector2 end = new Vector2(
            pos.x + rayLength * MathUtils.cos(followSensorAngle),
            pos.y + rayLength * MathUtils.sin(followSensorAngle)
        );

        EnemyVisionRaycast playerFollowRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.PLAYER_CHECK, 4f);
        world.rayCast(playerFollowRaycast, start, end);

        debugFollowStart.set(start);
        debugFollowEnd.set(end);

        boolean playerVisible = (playerFollowRaycast.getHitPlayer() != null);

        if (playerVisible)
        {
            debugFollowEnd.set(playerFollowRaycast.getHitPoint());
        }
        if (playerFollowRaycast.getHitFixture() != null) {
            debugFollowEnd.set(playerFollowRaycast.getHitPoint());
        }

        playerFollowRaycast.reset();
        return playerVisible;
    }

    public void lookForPlayer() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();
        Vector2 pos = obstacle.getPosition();
        float rayLength = 5.2f;
        Vector2 start, end;

        start = (facingRight) ? new Vector2(pos.x + width/2, pos.y + height/4) :
            new Vector2(pos.x - width/2, pos.y + height/4);

        float angleToPlayer = MathUtils.atan2(
            player.getObstacle().getPosition().y - pos.y,
            player.getObstacle().getPosition().x - pos.x
        );

        float forwardAngle = facingRight ? 0 : MathUtils.PI;

        if (isAwareOfPlayer()) {
            // If aware, track player
            end = new Vector2(
                pos.x + rayLength * MathUtils.cos(angleToPlayer),
                pos.y + rayLength * MathUtils.sin(angleToPlayer)
            );
            System.out.println("Track mode");
        } else {
            // If not aware, +-30 degree
            float maxAngleOffset = 30 * MathUtils.degreesToRadians; // 30 degrees in radians

            float angleDiff = angleToPlayer - forwardAngle;
            if (angleDiff > MathUtils.PI) angleDiff -= MathUtils.PI2;
            if (angleDiff < -MathUtils.PI) angleDiff += MathUtils.PI2;

            float clampedDiff = MathUtils.clamp(angleDiff, -maxAngleOffset, maxAngleOffset);

            float clampedAngle = forwardAngle + clampedDiff;

            end = new Vector2(
                pos.x + rayLength * MathUtils.cos(clampedAngle),
                pos.y + rayLength * MathUtils.sin(clampedAngle)
            );
        }

        EnemyVisionRaycast playerRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.PLAYER_CHECK, 4f);
        world.rayCast(playerRaycast, start, end);

        debugLookStart.set(start);
        debugLookEnd.set(end);

        boolean isAware = (playerRaycast.getHitPlayer() != null);
        if (isAware) {
            setAwareOfPlayer(true);
            debugLookEnd.set(playerRaycast.getHitPoint());

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

        debugGroundStart = (facingRight) ? new Vector2(pos.x + width * 1.5f, pos.y) :
            new Vector2(pos.x - width * 1.5f, pos.y);
        Vector2 wallStart = (facingRight) ? new Vector2(pos.x + width/2, pos.y) :
            new Vector2(pos.x - width/2, pos.y);

        debugGroundEnd = debugGroundStart.cpy().add(0, -groundRayLength);
        EnemyVisionRaycast wallVisionRaycast =
            new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.WALL_CHECK, stepRayLength);
        EnemyVisionRaycast groundVisionRaycast =
            new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.FALL_CHECK, stepRayLength);
        world.rayCast(groundVisionRaycast, debugGroundStart, debugGroundEnd);
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
        worldTarget = scene.getPossibleShardSpots().get(heldShard.id);
        System.out.println(worldTarget);
    }

    public void giveShard(Shard shard) {
        if (heldShard != null) {
            dropShard();
        }
        heldShard = shard;
        hasShard = true;

        // compute target
        setTarget();
    }

    public Shard dropShard() {
        Shard s = heldShard;
        heldShard = null;
        return s;
    }


    @Override
    public void update(float dt) {
        if (turnCooldown > 0) turnCooldown -= dt;
        boolean desiredFacing = (movement > 0) ? true : (movement < 0) ? false : facingRight;
        if (desiredFacing != facingRight && turnCooldown <= 0f) {
            inTurnAnimation = true;
            turnSprite.reset();
            turnCooldown = TURN_COOLDOWN_TIME;
        }
        facingRight = desiredFacing;

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
            scene.getAvatar().setTakingDamage(true);
            if (checkFollowRaycast()) {
                isFollowing = true;
                playerInFollowRange = true;
            } else {
                System.out.println("FOLLOW FALSE!");
                setAwareOfPlayer(false);
                playerInFollowRange = false;
            }
        }

        if (!isAwareOfPlayer() && wasAware) {
            dispatcher.dispatchMessage(null, scene, MessageType.CRITTER_LOST_PLAYER);
        } else if (isAwareOfPlayer() && !wasAware) {
            dispatcher.dispatchMessage(null, scene, MessageType.CRITTER_SEES_PLAYER);
        }

        wasAware = isAwareOfPlayer();

        if (inStunAnimation) {
            animationState = AnimationState.STUN;
        } else if (inTurnAnimation) {
            animationState = AnimationState.TURN;
            if (turnSprite.isAnimationFinished()) {
                inTurnAnimation = false;
            }
        }else if (hasShard) {
            animationState = AnimationState.SHARD_WALK;
        } else if (inAttackAnimation) {
            animationState = AnimationState.ATTACK;
        } else if (isChasing) {
            animationState = AnimationState.ALERT;
        } else {
            animationState = AnimationState.WALK;
            super.update(dt);
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
        TextureRegion frame;

        switch (animationState) {
            case WALK:
                frame = walkSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case TURN:
                frame = turnSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case ALERT:
                frame = alertSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case ATTACK:
                frame = attackSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case SHARD_WALK:
                frame = shardWalkSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case STUN:
                frame = stunnedSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            default:
                frame = walkSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
        }

        if (frame.isFlipX() != facingRight) {
            frame.flip(true, false);
        }

        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = width * u * 4f;
        float drawHeight = height * u * 1.15f;

        batch.draw(frame,
            posX - drawWidth / 2f, posY - drawHeight / 2f,
            drawWidth / 2f, drawHeight / 2f,
            drawWidth, drawHeight,
            1f, 1f, 0f);
    }


    public void drawRayDebug(SpriteBatch batch, Vector2 debugStart, Vector2 debugEnd) {
        if (debugStart != null && debugEnd != null) {
            float u = obstacle.getPhysicsUnits();
            Vector2 localStart = new Vector2(debugStart).sub(obstacle.getPosition());
            Vector2 localEnd = new Vector2(debugEnd).sub(obstacle.getPosition());

            PathFactory factory = new PathFactory();
            Path2 rayOutline = new Path2();
            factory.makeLine(localStart.x * u, localStart.y * u,
                localEnd.x * u, localEnd.y * u, rayOutline);

            batch.setTexture(Texture2D.getBlank());
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
        batch.setColor(Color.PURPLE);
        drawRayDebug(batch, debugRayStart, debugRayEnd);
        if (isAwareOfPlayer()) {
            batch.setColor(Color.GREEN);
            drawRayDebug(batch, debugFollowStart, debugFollowEnd);
        }
        batch.setColor(Color.PURPLE);
        drawRayDebug(batch, debugLookStart, debugLookEnd);
        batch.setColor(Color.WHITE);
        drawFallCheckDebug(batch);
    }

    public void drawFallCheckDebug(SpriteBatch batch) {
        // Convert world coordinates to screen units using your physics unit conversion.
        float u = obstacle.getPhysicsUnits();
        Path2 fallRayPath = new Path2();
        PathFactory factory = new PathFactory();
        factory.makeLine(debugGroundStart.x * u, debugGroundStart.y * u,
            debugGroundEnd.x * u, debugGroundEnd.y * u, fallRayPath);

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

    private float stunTimer;
    private float stunAnimTime = 0f;
    public Body getHeadBody() {
        return headBody;
    }
    public void getStunned() {
        if (!inStunAnimation) {
            inStunAnimation = true;
            setMovement(0);
            applyForce();
            setAwareOfPlayer(false);
            isChasing = false;
        }
    }

    public void setStunned(boolean stunned) {
        inStunAnimation = stunned;
    }

    public boolean isStunned() {
        return inStunAnimation;
    }

    public void setIsChasing(boolean val) {
        this.isChasing = val;
    }

    public void setInAttackAnimation(boolean val) {
        this.inAttackAnimation = val;
    }

}
