package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;

import javax.swing.*;

public class MindMaintenance extends Enemy {
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
    private int shootLimit = 60;
    private boolean isGrounded;
    private boolean isShooting;

    // Sensor for ground detection
    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;

    // Caches for force calculation and affine transform for sprite flipping
    private final Vector2 forceCache = new Vector2();
    private final Vector2 velocityCache = new Vector2();
    private final Affine2 flipCache = new Affine2();

    private Vector2 debugFollowStart;
    private Vector2 debugFollowEnd;
    private Vector2 debugGroundStart;
    private Vector2 debugGroundEnd;

    private Fixture visionSensor;
    private float followAngle;
    private Fixture followSensor;
    private float headOffset = 2.0f;
    private Fixture walkSensor;
    private Vector2 debugLookStart = new Vector2();
    private Vector2 debugLookEnd = new Vector2();

    // time before enemy resumes normal behavior after detecting player
    private float susCooldown = 100;
    private float susCountdown = susCooldown;

    /** game logic stuff */

    // where the maintenance is looking, 0 is relative down of the critter's central location, iterates clockwise
    private float visionAngle;

    // should be seconds in how long it takes for the critter to reset from aware of player to idle
    private float awarenessCooldown;

    private Path2 visionSensorOutline;
    private Path2 visionFollowOutline;
    private Path2 walkSensorOutline;
    private Path2 harvestOutline;

    private boolean safeToWalk;

    /** animation */
    private Animator idleSprite;
    private Animator walkingSprite;
    private Animator turnSprite;
    private Animator alertSprite;
    private Animator alertWalk;
    private Animator attackSprite;
    private Animator stunnedSprite;
    private MindMaintenance.AnimationState animationState;

    private final int ATTACK_FRAME_DURATION = 29;
    private final int STUN_FRAME_DURATION   = 20;
    private boolean inAttackAnimation = false;
    private int     attackFrameCounter = 0;
    private boolean inStunAnimation   = false;
    private int     stunFrameCounter  = 0;;


    private enum AnimationState {
        WALK,
        IDLE,
        TURN,
        STAIR,
        ALERT,
        ATTACK,
        STUN
    }

    public void createAnimators(Texture mindmaintenance) {
        //idleSprite = new Animator();
        walkingSprite = new Animator(mindmaintenance, 10, 13, 0.08f, 130, 0,15);
        turnSprite = new Animator(mindmaintenance, 10, 13, 0.08f, 130, 17,27);
        alertSprite = new Animator(mindmaintenance, 10, 13, 0.08f, 130, 42,67);
        attackSprite = new Animator(mindmaintenance, 10, 13, 0.08f, 130, 87,115, false);
        stunnedSprite = new Animator(mindmaintenance, 10, 13, 0.08f, 130, 116, 135, false);
    }

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
        float desiredAngle = theta * MathUtils.degreesToRadians; // Convert to radians
        headBody.setTransform(headBody.getPosition(), desiredAngle);
    }

    public boolean isSafeToWalk(){return safeToWalk;}

    public boolean isSus(){return susCountdown > 0;}

    public boolean isShooting() {
        return isShooting && shootCooldown <= 0 && !isStunned();
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


    public MindMaintenance(float units, JsonValue data, float[] points, PlatformScene scene, MessageDispatcher dispatcher) {
        super(dispatcher);
        this.data = data;
        this.scene = scene;

        // Read initial position and overall size from JSON.
        float x = points[0];
        float y = points[1];
        float s = data.getFloat("size");

        float size = s * units;

        width  = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);

        float drawWidth  = size;
        float drawHeight = size*2;

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
        obstacle.setName("maintenance");

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
        //deg
        visionAngle = 0;

        stepRayLength = height;
        enemyVisionRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.STAIR_CHECK, stepRayLength);

        mesh.set(-drawWidth/1.5f, -drawHeight/1.6f, drawWidth*1.5f, drawHeight*1.5f);
    }

    public void setActiveTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "mind-maintenance-active", Texture.class );
        this.setTexture(texture);
    }

    public void setStunTexture(AssetDirectory directory){
        Texture texture = directory.getEntry( "mind-maintenance-inactive", Texture.class );
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
        sensorName = "maintenance_sensor";
        sensorFixture.setUserData(sensorName);

        // Create a debug outline for the sensor so you can see it in debug mode
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
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

    public void createVisionSensor() {
        createHeadBody();
        attachHead();
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
        if (!groundExists)
        {
            System.out.println("Don't see the GROUND RUN!!");
        }
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

    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        Body body = obstacle.getBody();

        // Apply damping when no horizontal input is provided
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

        // Clamp horizontal velocity to the maximum speed
        if (Math.abs(vx) >= getMaxSpeed()) {
            obstacle.setVX(Math.signum(vx) * getMaxSpeed());
        }

        // Apply a vertical impulse if a jump is initiated
        if (isJumping()) {
            forceCache.set(0, jump_force);
            body.applyLinearImpulse(forceCache, pos, true);
        }
    }

    public void resetShootCooldown(){
        shootCooldown = shootLimit;
    }

    @Override
    public void update(float dt) {
        lookForPlayer();
        if (isStunned()) {
            animationState = AnimationState.STUN;
            super.update(dt);
            return;
        }

        if (isPlatformStep(scene.world, stepRayLength)) {
            System.out.println("MM's seen a step");
        }
        if (inAttackAnimation) {
            animationState = AnimationState.ATTACK;
            attackFrameCounter++;
            if (attackFrameCounter >= ATTACK_FRAME_DURATION) {
                inAttackAnimation = false;
                resetShootCooldown();
            }
            super.update(dt);
            return;
        }

        if (isShooting()) {
            if (!inAttackAnimation) {
                inAttackAnimation    = true;
                attackFrameCounter   = 0;
                attackSprite.reset();
            }
        }

        if (isAwareOfPlayer()) {
            animationState = AnimationState.ALERT;
        }
        else if (movement == 0) {
            animationState = MindMaintenance.AnimationState.TURN;
        }
        else{
            animationState = MindMaintenance.AnimationState.WALK;
        }

        if (!canContinue()) {
            safeToWalk = false;
            setMovement(-getMovement());
            applyForce();
        } else {
            safeToWalk = true;
        }

        if (isJumping()) {
            jumpCooldown = jumpLimit;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
        }
        if (isAwareOfPlayer()) {
            //updateFollowSensor(scene.getAvatar());
            susCountdown = susCooldown;
        }
        else{
            susCountdown--;
        }
        if (!isShooting()) {
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
        TextureRegion frame = new TextureRegion();
        switch (animationState) {
            case WALK:
                frame = walkingSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case TURN:
                frame = turnSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case ATTACK:
                frame = attackSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case STUN:
                frame = stunnedSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case ALERT:
                frame = alertSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            default:
                frame = alertSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
        }

        if(facingRight){
            frame.flip(true,false);
        }

        float u = obstacle.getPhysicsUnits();

        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = width * u * 4f;
        float drawHeight = height * u * 1.15f;

        float originX = drawWidth / 2f;
        float originY = drawHeight / 2f;

        batch.draw(frame,
            posX - originX, // lower-left x position
            posY - originY, // lower-left y position
            originX,        // originX used for scaling and rotation
            originY,        // originY
            drawWidth,      // width
            drawHeight,     // height
            1f,             // scaleX
            1f,             // scaleY
            0f              // rotation (in degrees)
        );
        /*
        if (obstacle != null && mesh != null) {
            float scaleFactor = 1.4f; // Increase sprite size by 20%
            float x = obstacle.getX();
            float y = obstacle.getY();
            float a = obstacle.getAngle();

            transform.idt();
            transform.preScale(scaleFactor, scaleFactor); // Scale up sprite only
            transform.preRotate((float) ((double) (a * 180.0F) / Math.PI));
            transform.preTranslate(x * u, y * u);

            batch.setTextureRegion(sprite);
            batch.drawMesh(mesh, transform, false);
            batch.setTexture((Texture) null);
        }

         */
    }

    public boolean checkFollowRaycast() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();
        Vector2 pos = obstacle.getPosition();

        float rayLength = 7f;
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
            end = new Vector2(
                pos.x + rayLength * MathUtils.cos(angleToPlayer),
                pos.y + rayLength * MathUtils.sin(angleToPlayer)
            );
        } else {
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

        // message dispatch
        if (isAware && !wasAware) {
            dispatcher.dispatchMessage(null, scene, MessageType.ENEMY_SEES_PLAYER);
        } else if (!isAware && wasAware) {
            dispatcher.dispatchMessage(null, scene, MessageType.ENEMY_LOST_PLAYER);
        }

        wasAware = isAware;

        playerRaycast.reset();
    }



    /**
     * Draws the player sprite.
     * The sprite is flipped horizontally if the player is facing left.
     *
     * @param batch  The sprite batch used for drawing.
     */
    /*
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
     */

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

    public void drawFallCheckDebug(SpriteBatch batch) {
        // Get the world and critter position.
        World world = obstacle.getBody().getWorld();
        Vector2 pos = obstacle.getBody().getPosition();



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

    public Body getHeadBody() {
        return headBody;
    }
}
