 package edu.cornell.cis3152.physics.platform;

    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.ai.msg.MessageDispatcher;
    import com.badlogic.gdx.graphics.g2d.TextureRegion;
    import com.badlogic.gdx.graphics.glutils.ShaderProgram;
    import com.badlogic.gdx.math.*;
    import com.badlogic.gdx.graphics.*;
    import com.badlogic.gdx.physics.box2d.*;

    import com.badlogic.gdx.utils.JsonValue;
    import edu.cornell.gdiac.assets.ParserUtils;
    import edu.cornell.gdiac.graphics.Shader;
    import edu.cornell.gdiac.graphics.SpriteBatch;
    import edu.cornell.gdiac.graphics.Texture2D;
    import edu.cornell.gdiac.math.Path2;
    import edu.cornell.gdiac.math.PathFactory;
    import edu.cornell.gdiac.physics2.*;

    import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;


    public class Player extends ObstacleSprite {

        private PlatformScene scene;

        /** The initializing data (to avoid magic numbers) */
        private final JsonValue data;
        /** The width of Player's avatar */
        private float width;
        /** The height of Player's avatar */
        private float height;

        /** The factor to multiply by the input */
        private float force;
        /** The amount to slow the character down */
        private float damping;
        /** The maximum character speed */
        private float maxspeed;
        private float defaultMaxSpeed;

        private boolean slowed = false;
        private float slowTime = 0f;
        private float slowAlpha = 1f;
        /** The impulse for the character jump */
        private float jump_force;
        /** The impulse for the character dash */
        private float dash_force;


        /** Cooldown (in animation frames) for jumping */
        private int jumpLimit;
        /** How long until we can jump again */
        private int jumpCooldown;
        /** Whether we are actively jumping */
        private boolean isJumping;
        private boolean lastJumping;
        private int coyoteTimeFrames = 5;
        private int coyoteTimeCounter = 0;

        private int jumpAnimFrames = 0;
        private static final int MIN_JUMP_ANIM_FRAMES = 10;

        // hold jumping controls
        private boolean jumpHeld = false;
        private int jumpHoldFrames = 0;
        private final int MIN_JUMP_HOLD_FRAMES = 10; // Minimum frames to hold for full jump
        private boolean jumpReleased = false;


        /** How long (in animation frames) the harvesting attack lasts  */
        private int harvestDuration;
        /** How long until the harvest attack is finished  */
        private int harvestDurationCounter;
        /** Cooldown (in animation frames) for harvesting */
        private int harvestLimit;
        /** How long until we can harvest again */
        private int harvestCooldown;
        /** Whether player has started to harvest */
        private boolean startedHarvest;

        /** Cooldown (in animation frames) for stunning */
        private int stunLimit;
        /** How long until we can stun again */
        private int stunCooldown;
        /** Whether we are actively stunning */
        private boolean isStunning;

        /** Cooldown (in animation frames) for teleporting */
        private int teleportLimit;
        /** How long until we can teleport again */
        private int teleportCooldown;
        /** Whether we are actively teleporting */
        private boolean isTeleporting;

        /** Cooldown (in animation frames) for taking damage */
        private int takeDamageLimit;
        /** How long until we can take damage again */
        private int takeDamageCooldown;
        /** Whether we are actively taking damage */
        private boolean isTakingDamage;
        /** Whether we are actively taking door */
        private boolean isTakingDoor;
        /** The current horizontal movement of the character */
        private float   movement;
        /** Which direction is the character facing */
        private boolean faceRight;

        private boolean didChangeDir = false;
        /** Whether our feet are on the ground */
        private boolean isGrounded;
        /** Whether our feet are on a shadowed tile*/
        private boolean isInShadow;
        private boolean wasGrounded = false;



        /** The outline of the sensor obstacle */
        private Path2 sensorOutline;
        /** The debug color for the sensor */
        private Color sensorColor;
        /** The name of the sensor fixture */
        private String sensorName;

        /** Cache for internal force calculations */
        private final Vector2 forceCache = new Vector2();
        /** Cache for the affine flip */
        private final Affine2 flipCache = new Affine2();

        /** Ability and Health meter for CatDemon**/
        private int fearMeter;
        /** MAX Ability and Health meter for CatDemon**/
        private int maxFearMeter;

        private float teleportRangeRadius;

        /** The outline of the sensor obstacle */
        private Path2 sensorScareOutline;
        /** The debug color for the sensor */
        private Color sensorScareColor;
        /** The name of the sensor fixture */
        private String sensorScareName;

        private Fixture scareSensorFixture;

        // falling
        private Path2   fallSensorOutline;
        private String  fallSensorName;
        private boolean fallSensorContact = false;

        private Path2 grabSensorOutline;



        /** raycasting for stair interpolation */
        private float stepRayLength;
        private PlayerVisionRaycast playerVisionRaycast;
        private Vector2 debugRayStart;
        private Vector2 debugRayEnd;
        private boolean seenAStep;
        private static final int FRAME_STAIR_COOLDOWN = 0;
        private int stairCooldown = 0;

        /** animation */
        private Animator walkingSprite;
        private Animator idleSprite;
        private Animator jumpSprite;
        private Animator stairSprite;
        private Animator fallSprite;
        private Animator landingSprite;
        private Animator stunningSprite;
        private Animator interactingSprite;
        private Animator absorbSprite;
        private AnimationState animationState;

        private boolean shroudMode;
        public float shroudModeTime = 0f;
        private float shroudAlpha = 1.0f;

        // animation constants/variables //
        private boolean isClimbing = false;
        private int climbCounter = 0;
        private final int CLIMB_DURATION = 11;
        private Vector2 climbStart = new Vector2();
        private Vector2 climbTarget = new Vector2();

        private boolean isLanding = false;
        private int landCounter = 0;
        private final int LAND_DURATION = 18;

        private boolean inStunAnimation = false;
        private int attackFrameCounter = 0;
        private final int ATTACK_FRAME_DURATION = 24;

        private boolean isInteracting;
        private boolean lastInteracting = false;
        private boolean inInteractAnimation = false;
        private int interactFrameCounter = 0;
        private static final int INTERACT_FRAME_DURATION = 46;
        private boolean hoverInteract;
        private boolean absorbing;

        // if i am blinded by dd
        private boolean isBlinded = false;
        private float blindTimer = 0f;
        private static final float MAX_BLIND_TIME = 2.0f;

        // knockback settings
        private boolean knockback;
        private boolean knockbackDmg;
        private int knockbackTimer;
        private static final int KNOCKBACK_DURATION = 20;
        private Vector2 knockbackCache = new Vector2();

        // message dispatcher for telegraphing player state to platform scene
        private final MessageDispatcher dispatcher = new MessageDispatcher();

        // SHADERS
        private Shader playerShroudShader, playerSlowShader;



        private enum AnimationState {
            WALK,
            IDLE,
            JUMP,
            STAIR,
            FALL,
            LAND,
            STUN,
            INTERACT,
            ABSORB
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public void setBlinded(boolean blinded) {
            if (blinded) {
                this.isBlinded = true;
                this.blindTimer = 0f;
            }
        }

        public boolean isBlinded() {
            return isBlinded;
        }

        public void updateBlind(float dt) {
            if (isBlinded) {
                blindTimer += dt;
                if (blindTimer >= MAX_BLIND_TIME) {
                    isBlinded = false;
                    blindTimer = 0f;
                }
            }
        }


        /**
         * Returns the left/right movement of this character.
         *
         * This is the result of input times force.
         *
         * @return the left/right movement of this character.
         */
        public float getMovement() {
            return movement;
        }

        /**
         * Sets the left/right movement of this character.
         *
         * This is the result of input times force.
         *
         * @param value the left/right movement of this character.
         */
        public void setMovement(float value) {
            if (isInteracting) {
                movement = 0;
                return;
            }
//            if (isLanding) {
//                movement = value;
//                return;
//            }

            movement = value;
            // Change facing if appropriate
            didChangeDir = movement < 0 && isFacingRight() || movement > 0 && isFacingRight();


            if (movement < 0) {
                faceRight = false;
            } else if (movement > 0) {
                faceRight = true;
            }
        }

        public void setFaceRight(boolean value) {
            faceRight = value;
        }

        /** Returns teleport range radius
         *
         * @return teleport range radius
         */
        public float getTeleportRangeRadius() {
            return teleportRangeRadius;
        }

        /** Sets teleport range radius to value
         *
         * @param value value
         */
        public void setTeleportRangeRadius(float value) {
            teleportRangeRadius = value;
        }

        /** Returns current fear meter charge
         *
         * @return current fear meter charge
         */
        public int getFearMeter() {
            return fearMeter;
        }


        public int getMaxFearMeter() {
            return maxFearMeter;
        }

        /** Set fear meter charge to value. Checks if within bounds.
         *
         * @param value value to set fear meter to
         */
        public void setFearMeter(int value) {
            if (value < 0)
            {
                fearMeter = 0;
            } else if (value > maxFearMeter) {
                fearMeter = maxFearMeter;
            } else {
                fearMeter = value;
            }
        }


        /** Sets maxFearMeter to value
         *
         * @param value
         */
        public void setMaxFearMeter(int value) {
            maxFearMeter = value;
        }


        /**
         * Returns true if Player is actively harvesting.
         *
         * @return true if Player is actively harvesting.
         */
        public boolean isHarvesting() {
            return harvestDurationCounter > 0;
            //return isHarvesting && harvestCooldown <= 0;
        }

        public float getHarvestCooldown() {
            return harvestCooldown;
        }
        public int getHarvestDuration() {
            return harvestDuration;
        }
        /**
         * Sets whether Player is actively firing.
         *
         * @param value whether Player is actively firing.
         */
        public void tryStartHarvesting(boolean value) {
            startedHarvest = (value && harvestCooldown <= 0);
        }

        /**
         * Returns true if CatDemon is actively stunning.
         *
         * @return true if CatDemon is actively stunning.
         */
        public boolean isStunning() {
            return isStunning && stunCooldown <= 0;
        }

        /**
         * Sets whether CatDemon is actively stunning.
         *
         * @param value whether CatDemon is actively stunning.
         */
        public void setStunning(boolean value) {
            isStunning = value;
        }

        public void setInteracting(boolean value) {
            isInteracting = value;
        }

        public boolean isInteracting() {
            return isInteracting;
        }

        public void setHoverInteract(boolean value) {
            hoverInteract = value;
        }

        public boolean getHoverInteract() {
            return hoverInteract;
        }


        /**
         * Returns true if CatDemon is actively harvesting.
         *
         * @return true if CatDemon is actively harvesting.
         */
        public boolean isTeleporting() {
            dispatcher.dispatchMessage(null, scene, MessageType.CRITTER_LOST_PLAYER);
            return isTeleporting && shroudMode && teleportCooldown <= 0;

        }


        /**
         * Sets whether CatDemon is actively harvesting.
         *
         * @param value whether CatDemon is actively harvesting.
         */
        public void setTeleporting(boolean value) {
            isTeleporting = value;
        }

        /**
         * Returns true if Player is actively taking damage.
         *
         * @return true if Player is actively taking damage.
         */
        public boolean isTakingDamage() {
            return isTakingDamage && takeDamageCooldown <= 0;
        }

        /**
         * Sets whether Player is actively taking damage.
         *
         * @param value whether Player is actively taking damage.
         */
        public void setTakingDamage(boolean value) {
            isTakingDamage = value;
        }

        public boolean isKnockedBack() {
            return knockback;
        }

        public void setKnockingBack(boolean value, Vector2 knockOrigin) {
            knockback = value;
            if (value) {
                knockbackDmg = true;
                knockbackCache.set(knockOrigin);
                this.knockbackTimer = 0;
            }
        }

        /**
         * Returns true if Player is actively taking door.
         *
         * @return true if Player is actively taking door.
         */
        public boolean isTakingDoor() {
            return isTakingDoor;
        }

        /**
         * Sets whether Player is actively taking door.
         *
         * @param value whether Player is actively taking door.
         */
        public void setTakingDoor(boolean value) {
            isTakingDoor = value;
        }

        /**
         * Returns true if Player is actively jumping.
         *
         * @return true if Player is actively jumping.
         */
        public boolean isJumping() {
            return isJumping && (isGrounded || coyoteTimeCounter > 0) && jumpCooldown <= 0;
        }

        /**
         * Sets whether the jump button is being held down.
         *
         * @param value whether jump is being held
         */
        public void setJumpHeld(boolean value) {
            if (jumpHeld && !value) {
                jumpReleased = true;
            }
            jumpHeld = value;
        }

        /**
         * Sets whether Player is actively jumping.
         *
         * @param value whether Player is actively jumping.
         */
        public void setJumping(boolean value) {
            isJumping = value;
        }

        /**
         * Returns true if Player is on the ground.
         *
         * @return true if Player is on the ground.
         */
        public boolean isGrounded() {
            return isGrounded;
        }

        /**
         * Sets whether Player is on the ground.
         *
         * @param value whether Player is on the ground.
         */
        public void setGrounded(boolean value) {
            isGrounded = value;
        }


        /**
         * Returns true if Player is in shadow
         *
         * @return true if Player is in shadow
         */
        public boolean isInShadow() { return isInShadow;}

        /**
         * Sets whether Player is in a shadow.
         *
         * @param value whether Player is in a shadow.
         */
        public void setIsShadow(boolean value) {
            isInShadow = value;
        }

        /**
         * Returns how much force to apply to get Player moving
         *
         * Multiply this by the input to get the movement value.
         *
         * @return how much force to apply to get Player moving
         */
        public float getForce() {
            return force;
        }

        /**
         * Returns how hard the brakes are applied to stop Player moving
         *
         * @return how hard the brakes are applied to stop Player moving
         */
        public float getDamping() {
            return damping;
        }

        /**
         * Returns the upper limit on Player's left-right movement.
         *
         * This does NOT apply to vertical movement.
         *
         * @return the upper limit on Player's left-right movement.
         */
        public float getMaxSpeed() {
            return maxspeed;
        }

        /**
         * Returns the name of the ground sensor
         *
         * This is used by the ContactListener. Because we do not associate the
         * sensor with its own obstacle,
         *
         * @return the name of the ground sensor
         */
        public String getSensorName() {
            return sensorName;
        }

        /** Returns name of scare sensor
         *
         * @return
         */
        public String getScareSensorName() {
            return sensorScareName;
        }

        /**
         * Returns true if this character is facing right
         *
         * @return true if this character is facing right
         */
        public boolean isFacingRight() {
            return faceRight;
        }

        /**
         * Set if the fall sensor is contact with a platform(for landing animations)
         *
         */
        public void setFallSensorContact(boolean value) {
            this.fallSensorContact = value;
        }

        public void setMaxSpeed(float s) {
            this.maxspeed = s;
        }

        public void setSlowed(boolean value) {
            slowed = value;
        }
        /** Restores the original max speed from JSON. */
        public void resetMaxSpeed() {
            this.maxspeed = defaultMaxSpeed;
        }

        public void setShroudMode(boolean value) {
            shroudMode = value;
        }

        public boolean getShroudMode() {
            return shroudMode;
        }


        /**
         * Creates a new Player avatar with the given physics data
         *
         * The physics units are used to size the mesh relative to the physics
         * body. All other attributes are defined by the JSON file. Because of
         * transparency around the image file, the physics object will be slightly
         * thinner than the mesh in order to give a tighter hitbox.
         *
         * @param units     The physics units
         * @param data      The physics constants for Player
         */
        public Player(float units, JsonValue data, Vector2 spawn, PlatformScene scene) {
            this.data = data;
            JsonValue debugInfo = data.get("debug");


            float x = spawn.x;
            float y = spawn.y;
            float s = data.getFloat( "size" );
            float sizeWidth = s*units;
            float sizeHeight = s*units*1.8f;



            // The capsule is smaller than the image
            // "inner" is the fraction of the original size for the capsule
            width = s*data.get("inner").getFloat(0);
            height = s*data.get("inner").getFloat(1);
            obstacle = new CapsuleObstacle(x, y, width, height*0.9f);
            ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

            obstacle.setDensity( data.getFloat( "density", 0 ) );
            obstacle.setFriction( data.getFloat( "friction", 0 ) );
            obstacle.setRestitution(0f);
            obstacle.setFixedRotation(true);
            obstacle.setPhysicsUnits( units );
            obstacle.setUserData( this );
            obstacle.setName("player");

            debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
            sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);
            sensorScareColor = ParserUtils.parseColor( debugInfo.get("scare_sensor"),  Color.GREEN);

            maxspeed = data.getFloat("maxspeed", 0);
            defaultMaxSpeed = maxspeed;
            damping = 0;
            force = data.getFloat("force", 0);
            jump_force = data.getFloat( "jump_force", 0 );
            jump_force *= 1.55f;
            dash_force = data.getFloat("dash_force", 0);
            jumpLimit = data.getInt( "jump_cool", 0 );
            harvestLimit = 60;
            harvestDuration = 20;
            stunLimit = data.getInt( "shot_cool", 0 );
            teleportLimit = data.getInt( "shot_cool", 0 );
            takeDamageLimit = 120;
            shroudMode = false;

            // Gameplay attributes
            isGrounded = false;
            startedHarvest = false;
            isStunning = false;
            isTeleporting = false;
            isJumping = false;
            faceRight = true;
            isInShadow = false;

            harvestCooldown = 0;
            stunCooldown = 0;
            teleportCooldown = 0;
            jumpCooldown = 0;
            takeDamageCooldown = 0;

            sensorScareName = "scare_sensor";


            maxFearMeter = 20;
            fearMeter = maxFearMeter;

            teleportRangeRadius = 200;

            // Create a rectangular mesh for Player. This is the same as for door,
            // since Player is a rectangular image. But note that the capsule is
            // actually smaller than the image, making a tighter hitbox. You can
            // see this when you enable debug mode.
            mesh.set(-sizeWidth/2.0f,-sizeHeight/2.0f,sizeWidth * 2 ,sizeHeight * 2);

            stepRayLength = height/2.0f;

            playerVisionRaycast = new PlayerVisionRaycast(PlayerVisionRaycast.VisionMode.STAIR_CHECK, stepRayLength * units);

            scene = scene;
            playerShroudShader = new Shader(Gdx.files.internal("shaders/shroud.vert"), Gdx.files.internal("shaders/shroud.frag"));
            playerSlowShader = new Shader(Gdx.files.internal("shaders/shroud.vert"), Gdx.files.internal("shaders/pink.frag"));
        }

        public void createAnimators(Texture dreamwalker, Texture absorb) {
            walkingSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 0, 15);
            idleSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 37, 56);
            jumpSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 20, 26, false);
            stairSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 126, 133);
            fallSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 26, 26, false);
            landingSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 138, 156, false);
            stunningSprite = new Animator(dreamwalker, 11, 16, 0.033f, 176, 157, 174, false);
            interactingSprite = new Animator(absorb, 5, 5, 0.033f, 23, 0, 22, false);
            absorbSprite = new Animator(absorb, 5, 5, 0.033f, 23, 10, 11);
        }

        public void setFilter() {
            for (Fixture fixture : obstacle.getBody().getFixtureList()) {
                Object ud = fixture.getUserData();
                if (ud != null && ud.equals("player_sensor")) {
                    continue;
                }
                // Otherwise, assume this is the collision capsule fixture.
                Filter filter = fixture.getFilterData();
                filter.categoryBits = CATEGORY_PLAYER;
                // Player should collide with scenery but not with enemies.
                filter.maskBits = CATEGORY_SCENERY | CATEGORY_ENEMY_PROJECTILE;
                fixture.setFilterData(filter);
            }
        }


        /**
         * Creates the sensor for Player.
         *
         * We only allow the Player to jump when she's on the ground. Double jumping
         * is not allowed.
         *
         * To determine whether Player is on the ground we create a thin sensor under
         * her feet, which reports collisions with the world but has no collision
         * response. This sensor is just a FIXTURE, it is not an obstacle. We will
         * talk about the different between these later.
         *
         * Note this method is not part of the constructor. It can only be called
         * once the physics obstacle has been activated.
         */
        public void createSensor() {
            Vector2 sensorCenter = new Vector2(0, -height / 2.2f);
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = data.getFloat("density",0);
            sensorDef.isSensor = true;

            JsonValue sensorjv = data.get("sensor");
            float w = sensorjv.getFloat("shrink",0)*width * 0.3f;
            float h = sensorjv.getFloat("height",0) * 1.2f;
            PolygonShape sensorShape = new PolygonShape();
            sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
            sensorDef.shape = sensorShape;

            // Ground sensor to represent our feet
            Body body = obstacle.getBody();
            Fixture sensorFixture = body.createFixture( sensorDef );
            sensorName = "player_sensor";
            sensorFixture.setUserData(sensorName);

            Filter sensorFilter = sensorFixture.getFilterData();
            sensorFilter.categoryBits = CollisionFiltering.CATEGORY_PLAYER;    // or a dedicated sensor category
            sensorFilter.maskBits = CollisionFiltering.CATEGORY_SCENERY;       // so it collides with the ground
            sensorFixture.setFilterData(sensorFilter);

            // Finally, we need a debug outline
            float u = obstacle.getPhysicsUnits();
            PathFactory factory = new PathFactory();
            sensorOutline = new Path2();
            factory.makeRect( (sensorCenter.x-w/2)*u,(sensorCenter.y-h/2)*u, w*u, h*u,  sensorOutline);
        }

        public void createFallSensor() {
            float units = obstacle.getPhysicsUnits();
            float fallDepth = data.getFloat("fall_sensor_depth", 0.2f);
            // how far below feet to sense (in physics units)
            Vector2 center = new Vector2(0, -height/2.3f - fallDepth/2);

            FixtureDef def = new FixtureDef();
            def.isSensor = true;
            def.density  = 0;
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(width/2 * 0.2f, fallDepth/2, center, 0);
            def.shape = shape;

            Body body = obstacle.getBody();
            Fixture f = body.createFixture(def);
            fallSensorName = "fall_sensor";
            f.setUserData(fallSensorName);

            // collide only with scenery
            Filter filter = f.getFilterData();
            filter.categoryBits = CATEGORY_PLAYER;
            filter.maskBits     = CATEGORY_SCENERY;
            f.setFilterData(filter);

            // debug outline
            float u = units;
            PathFactory factory = new PathFactory();
            fallSensorOutline = new Path2();
            factory.makeRect(
                (center.x - width/2)*u,
                (center.y - fallDepth/2)*u,
                width*u,
                fallDepth*u,
                fallSensorOutline
            );
        }

        public void createScareSensor(){
            if (scareSensorFixture != null) return;
            Vector2 sensorScareCenter = new Vector2(0, 0);
            FixtureDef scareSensorDef = new FixtureDef();
            scareSensorDef.density = 0;
            scareSensorDef.isSensor = true;


            float w = width * 1.5f; // Cover full width
            float h = height / 2.0f; // Cover full height
            PolygonShape sensorScareShape = new PolygonShape();
            sensorScareShape.setAsBox(w, h, sensorScareCenter, 0.0f);
            scareSensorDef.shape = sensorScareShape;

            Body body = obstacle.getBody();
            scareSensorFixture = body.createFixture( scareSensorDef );
            sensorScareName = "scare_sensor";
            scareSensorFixture.setUserData(sensorScareName);

            sensorScareShape.dispose();

            float u = obstacle.getPhysicsUnits();
            PathFactory factory = new PathFactory();
            sensorScareOutline = new Path2();
            factory.makeRect((sensorScareCenter.x - w) * u, (sensorScareCenter.y - h) * u, w * 2 * u, h * 2 * u, sensorScareOutline);

        }


        /**
         * Applies the force to the body of Player
         *
         * This method should be called after the force attribute is set.
         */
        public void applyForce(World world) {
            float direction = isFacingRight() ? 1 : -1;

            if (!obstacle.isActive()) return;



            float maxUpwardVelocity = 15.0f;
            float maxFallingVelocity = -20.0f;

            Vector2 pos = obstacle.getPosition();
            float vx = obstacle.getVX();
            float vy = obstacle.getVY();
            Body body = obstacle.getBody();



            // Velocity too high, clamp it
            if (getMovement() == 0f && !isJumping()) {
                obstacle.setVX(0);
            } else if (Math.abs(vx) >= getMaxSpeed()) {
                obstacle.setVX(Math.signum(vx) * getMaxSpeed());
            }

            // Horizontal movement logic
            if (movement != 0) {
                forceCache.set(getMovement(), 0);
                body.applyForce(forceCache, pos, true);
            }

            // Dash logic
            if (startedHarvest && movement != 0){
                forceCache.set(direction * dash_force, 0);
                body.applyLinearImpulse(forceCache, pos, true);
            }

            // Jumping logic
            if (/*!shroudMode &&*/ isJumping()) {
                body.setLinearDamping(0f); // Reset any climbing damping
                jumpSprite.reset();

                // Don't want gravity to affect the jump if in coyote time
                if ((!isGrounded && coyoteTimeCounter > 0) || isClimbing) {
                    float currentHorizontalVelocity = obstacle.getVX();
                    body.setLinearVelocity(currentHorizontalVelocity, 0);
                }

                forceCache.set(0, jump_force);
                body.applyLinearImpulse(forceCache, pos, true);
                isClimbing = false;
            }
//                jumpHoldFrames = 0; // initiated a jump
//                jumpReleased = false;
//            } else if (jumpReleased && vy > 0 && jumpHoldFrames < MIN_JUMP_HOLD_FRAMES) {
//                // Cut the jump short by reducing upward velocity
//                float reducedVelocity = vy * 0.2f; // Cut velocity in half
//                body.setLinearVelocity(body.getLinearVelocity().x, reducedVelocity);
//                jumpReleased = false;
//            }

            // Climbing logic
            else if (isClimbing && isGrounded) {
                forceCache.set(new Vector2(14.7f * direction, 0));
                body.applyForce(forceCache, pos, true);

                if (movement == 0) {
                    body.setLinearDamping(100000f); // Prevent sliding
                } else {
                    body.setLinearDamping(0f);
                }
            }

            if (isFacingRight() && obstacle.getVX() < 0) {
                System.out.println("VELOCITY RIGHT: " + getMovement() + " " + obstacle.getVX());
                obstacle.setVX(0);
            } else if (!isFacingRight() && obstacle.getVX() > 0) {
                System.out.println("VELOCITY LEFT: " + getMovement() + " " + obstacle.getVX());
                obstacle.setVX(0);
            }

            if (isKnockedBack()) {
                Vector2 dir = pos.cpy().sub(knockbackCache);
                if (dir.isZero()) { dir.set(0,1); }  // fallback up
                dir.nor();

                float strength = 1f / (float) (1 - Math.pow((Math.E), -.05 * (knockbackTimer + 1)));
                strength *= 1.5f;
                dir.scl(strength);
                System.out.println("knockback" + dir);
                obstacle.setLinearVelocity(dir);
                knockbackTimer++;
                if (knockbackTimer >= KNOCKBACK_DURATION) {
                    // end the leap
                    knockback = false;
                    knockbackTimer = 0;
                }
                return;
            }

            // fast falling
            if (shroudMode && vy < 0) {
                obstacle.setGravityScale(1.75f);
            } else if (vy < 0) {
                obstacle.setGravityScale(1.75f);
            } else {
                obstacle.setGravityScale(1);
            }

            if (shroudMode && !isGrounded) obstacle.setLinearVelocity(obstacle.getLinearVelocity().scl(1f));

            // Clamp vertical velocity
            if (vy > maxUpwardVelocity) {
                obstacle.setVY(0);
                System.out.println("Stopping extreme Upward velocity!");
            } else if (vy < maxFallingVelocity) {
                System.out.println("MAX FALLING REACHED!");
                Vector2 currentVelocity = body.getLinearVelocity();
                body.setLinearVelocity(currentVelocity.x, maxFallingVelocity);
            }
        }

        public boolean isPlatformStep(World world, float raylength) {
            playerVisionRaycast.reset();
            Body body = obstacle.getBody();

            Vector2 start = (isFacingRight()) ?
                obstacle.getBody().getPosition().cpy().add(0, 0) :
                obstacle.getBody().getPosition().cpy().add(0, 0);
            Vector2 end = start.cpy().add(0, -raylength);


            debugRayStart = start;
            debugRayEnd = end;

            world.rayCast(playerVisionRaycast, start, end);
            Vector2 normal = playerVisionRaycast.getHitNormal();
            if (normal != null) {
                // build the tangent (perpendicular to the normal)
                Vector2 tangent = new Vector2(normal.y, -normal.x).nor();
                // downward ray direction
                Vector2 rayDir  = new Vector2(0, -1);
                // angle between ray and surface tangent (in degrees)
                float dot = tangent.dot(rayDir);
                dot = MathUtils.clamp(dot, -1f, 1f);
                float angleDeg = (float)(Math.acos(dot) * MathUtils.radiansToDegrees);

                // if this is *almost* 90°, it's a flat surface → skip snapping
                if (Math.abs(angleDeg - 90f) < 1f) {
                    isClimbing = false;
                    body.setLinearDamping(0f);
                    playerVisionRaycast.reset();
                    stairCooldown = FRAME_STAIR_COOLDOWN;
                    return false;
                }
            }

            if (playerVisionRaycast.getHitFixture() == null) {
                isClimbing = false;
                return false;
            } else if (playerVisionRaycast.fixtureIsStair) {
                Vector2 stairHit = new Vector2(playerVisionRaycast.getHitPoint());
                playerVisionRaycast.reset();

                stairCooldown = FRAME_STAIR_COOLDOWN;
                if (isGrounded && Math.abs(movement) > 0) {
                    float targetCenterY = stairHit.y + height/2;

                    isClimbing = true;
                    debugRayEnd = stairHit;
                    return true;
                }
                if (movement == 0 && !isJumping()) {

                    float targetCenterY = stairHit.y + height/2f;  // tiny offset to avoid re-trigger
                    body.setTransform(body.getPosition().x, targetCenterY, body.getAngle());
                }
            }



            return false;
        }

        /**
         * Updates the object's physics state (NOT GAME LOGIC).
         *
         * We use this method to reset cooldowns.
         *
         * @param dt    Number of seconds since last animation frame
         */
        @Override
        public void update(float dt) {
            boolean prevGrounded = wasGrounded;
            wasGrounded = isGrounded;


            World world = obstacle.getBody().getWorld();
            if (isPlatformStep(world, stepRayLength)) {
                System.out.println("seen a step");
                seenAStep = true;
            } else {
                seenAStep = false;
            }

            if (slowed) {
                slowTime += dt;
            } else {
                slowTime = 0;
            }

            if (slowed) {
                slowAlpha = Math.min(1f,
                    1f/ (float) (1 + 7 * Math.pow((Math.E), -8.6 * (slowTime))) );
            } else {
                slowAlpha = 0f;
            }

            if (shroudMode) {
                shroudModeTime += dt;
            }
            if (!shroudMode) {
                shroudModeTime = 0;
            }

            if (shroudMode) {
                shroudAlpha = Math.min(0.75f,
                    1f/ (float) (1 + 7 * Math.pow((Math.E), -6.6 * (shroudModeTime))) );
            } else {
                shroudAlpha = 0f; // or fade out if you want
            }

            if (wasGrounded && !isGrounded) {
                // we just left the ground
                jumpAnimFrames = 0;
            }

            if (isKnockedBack()) {

                if (knockbackDmg) {
                    setFearMeter(getFearMeter() - 2);
                    System.out.println("TAKEN KNOCKBACK DMG");
                    knockbackDmg = false;
                }
            }



            // Apply cooldowns

            if (harvestDurationCounter <= 0 && scareSensorFixture != null) {
                Body body = obstacle.getBody();
                body.destroyFixture(scareSensorFixture);

                // Clear out our references (so we can re-create next time)
                scareSensorFixture = null;
                sensorScareOutline = null;
            }


            if (isJumping()) {
                jumpCooldown = jumpLimit;
            } else {
                jumpCooldown = Math.max(0, jumpCooldown - 1);
            }

            if (startedHarvest) {
                harvestCooldown = harvestLimit;
                harvestDurationCounter = harvestDuration;
                if (scareSensorFixture == null) {
                    createScareSensor();
                }
            } else {
                harvestCooldown = Math.max(0, harvestCooldown - 1);
                harvestDurationCounter = Math.max(0, harvestDurationCounter - 1);
            }

            if (isStunning()) {
                stunCooldown = stunLimit;
            } else {
                stunCooldown = Math.max(0, stunCooldown - 1);
            }

            if (isTeleporting()) {
                teleportCooldown = teleportLimit;
            } else {
                teleportCooldown = Math.max(0, teleportCooldown - 1);
            }

            if (isTakingDamage())
            {
                setFearMeter(fearMeter - 1);
                takeDamageCooldown = takeDamageLimit;
                setTakingDamage(false);

            } else {
                takeDamageCooldown = Math.max(0, takeDamageCooldown - 1);
            }
            updateBlind(dt);

            // animation locks
            if (absorbing) {
                if (isInteracting) {
                    super.update(dt);
                    return;
                }
            }
            absorbing = false;
            if (inInteractAnimation) {
                interactFrameCounter++;

                if ( hoverInteract
                    && isInteracting
                    && interactFrameCounter >= INTERACT_FRAME_DURATION/2
                ) {
                    animationState = AnimationState.ABSORB;
                    inInteractAnimation = false;
                    absorbing = true;
                } else {
                    animationState = AnimationState.INTERACT;
                }
                if (interactFrameCounter >= INTERACT_FRAME_DURATION) {
                    interactFrameCounter = 0;
                    inInteractAnimation = false;
                }
                super.update(dt);
                return;
            }



            if (jumpHeld && !isGrounded && obstacle.getVY() > 0) {
                jumpHoldFrames++;
            }

            if (!prevGrounded && isGrounded) {
                isLanding    = true;
                landCounter  = 0;
                landingSprite.reset();
                animationState = AnimationState.LAND;
                jumpHoldFrames = 0;
                jumpReleased = false;
            }



            if (isLanding && !isHarvesting()) {

                landCounter++;
                super.update(dt);
                if (landCounter >= LAND_DURATION) {
                    landCounter = 0;
                    isLanding = false;
                }
                super.update(dt);
                return;
            }


            if (inStunAnimation) {
                attackFrameCounter++;
                super.update(dt);
                if (attackFrameCounter > ATTACK_FRAME_DURATION) {
                    attackFrameCounter = 0;
                    inStunAnimation = false;
                }
                super.update(dt);
                return;
            }



            if (isInteracting && isGrounded) {
                if (!inInteractAnimation) {
                    inInteractAnimation = true;
                    interactFrameCounter = 0;
                    interactingSprite.reset();
                }
                animationState = AnimationState.INTERACT;
            } else if (isStunning()) {
                if (!inStunAnimation) {
                    inStunAnimation = true;
                    attackFrameCounter = 0;
                    stunningSprite.reset();
                }
                animationState = AnimationState.STUN;
            } else if (isGrounded) {
                if (movement == 0) {
                    animationState = AnimationState.IDLE;
                } else if (isClimbing) {
                    animationState = AnimationState.STAIR;
                } else {
                    animationState = AnimationState.WALK;
                }
            } else {
                jumpAnimFrames++;
                if (obstacle.getVY() > 0 || jumpAnimFrames < MIN_JUMP_ANIM_FRAMES) {
                    // not grounded, in the state of moving up = jumping
                    animationState = AnimationState.JUMP;
                } else {
                    // we are falling
                    animationState= AnimationState.FALL;
                }

            }

            if(isGrounded)
            {
                coyoteTimeCounter = coyoteTimeFrames;
            } else {
                coyoteTimeCounter = Math.max(0, coyoteTimeCounter - 1);
            }


            super.update(dt);

        }

        @Override
        public void draw(SpriteBatch batch) {

            TextureRegion frame = new TextureRegion();
            float scale = 1.05f;
            float dt = Gdx.graphics.getDeltaTime();
            if (slowed) {
                dt /= 2;
            }
            if (animationState == null) {
                return;
            }
            switch (animationState) {
                case WALK:
                    frame = walkingSprite.getCurrentFrame(dt);
                    break;
                case IDLE:
                    frame = idleSprite.getCurrentFrame(dt);
                    break;
                case JUMP:
                    if (!lastJumping && isJumping()) {
                        jumpSprite.reset();
                    }
                    frame = jumpSprite.getCurrentFrame(dt);
                    break;
                case STAIR:
                    frame = walkingSprite.getCurrentFrame(dt);
                    break;
                case FALL:
                    frame = fallSprite.getCurrentFrame(dt);
                    break;
                case LAND:
                    frame = landingSprite.getCurrentFrame(dt);
                    break;
                case STUN:
                    frame = stunningSprite.getCurrentFrame(dt);
                    break;
                case INTERACT:
                    frame = interactingSprite.getCurrentFrame(dt);
                    break;
                case ABSORB:
                    frame = absorbSprite.getCurrentFrame(dt);
                    break;
                default:
                    frame = idleSprite.getCurrentFrame(dt);
                    break;
            }

            float u = obstacle.getPhysicsUnits();
            // Determine drawing coordinates.
            // Here we assume obstacle.getX() and getY() return the center position.
            float posX = obstacle.getX() * u;
            float posY = obstacle.getY() * u;
            float drawWidth = width * u * 3f * scale;
            float drawHeight = height * u * scale * 1.25f;

            float originX = (faceRight) ? drawWidth / 2.0f : drawWidth / 1.75f;
            float originY = drawHeight / 1.92f;
            if (animationState == AnimationState.FALL || animationState == AnimationState.JUMP) {
                originY = drawHeight/1.3f;
            }

            if (faceRight) {

            } else {
                frame.flip(true, false);
            }

            ShaderProgram prev = batch.getShader();
            if (shroudMode) {
                batch.end();

                batch.setShader(playerShroudShader);
                batch.setColor(1,1,1,shroudAlpha);

                playerShroudShader.setUniformf("iTime", shroudModeTime);
                System.out.println("shroud time " + shroudModeTime);
                playerShroudShader.setUniformf("iResolution", drawWidth, drawHeight);

                batch.begin();
            }
            if (slowed) {
                batch.end();

                batch.setShader(playerSlowShader);
                batch.setColor(1,1,1,shroudAlpha);

                playerSlowShader.setUniformf("iTime", slowTime);
                System.out.println("slow time " + slowTime);
                playerSlowShader.setUniformf("iResolution", drawWidth, drawHeight);

                batch.begin();
            }

            // Draw the current frame centered on the player's position.
            batch.draw(frame,
                posX - originX, // lower-left x position
                posY - originY, // lower-left y position
                originX,        // originX used for scaling and rotation
                originY,        // originY
                drawWidth * 1.1f,      // width
                drawHeight * 1.1f,     // height
                1f,             // scaleX
                1f,             // scaleY
                0f              // rotation (in degrees)
            );
            batch.setColor(Color.WHITE);

//            if (faceRight) {
//                flipCache.setToScaling( 1,1 );
//            } else {
//                flipCache.setToScaling( -1,1 );
//            }
//            super.draw(batch,flipCache);
            if ((shroudMode || slowed) && prev != null) {
                batch.end();
                batch.setShader(prev);
                batch.begin();
            }
        }

        @Override
        public void drawDebug(SpriteBatch batch) {
            super.drawDebug( batch );

            drawSensorDebug(batch, sensorOutline, sensorColor);
            drawSensorDebug(batch, sensorScareOutline, sensorScareColor);
            drawSensorDebug(batch, fallSensorOutline, Color.GREEN);
            drawSensorDebug(batch, grabSensorOutline, Color.BLUE);
            drawTeleportRadius(batch);
            //drawRecastSensor(batch);
            drawRayDebug(batch);
        }

        public void drawRecastSensor(SpriteBatch batch) {


            Vector2 pos = obstacle.getPosition();
            float u = obstacle.getPhysicsUnits();

            float sensorWidth = width * 0.8f;
            float sensorHeight = height * 0.4f;
            float recastOffset = sensorHeight + 0.1f;

            float horizontalOffset = isFacingRight() ? width / 2 : -width / 2;

            Vector2 sensorCenter = new Vector2(pos.x + horizontalOffset,
                pos.y - height / 2 + recastOffset);

            Path2 recastSensorOutline = new Path2();
            PathFactory factory = new PathFactory();
            factory.makeRect((sensorCenter.x - sensorWidth / 2) * u,
                (sensorCenter.y - sensorHeight / 2) * u,
                sensorWidth * u,
                sensorHeight * u,
                recastSensorOutline);

            Affine2 transform = new Affine2();
            transform.idt();
            transform.preRotate((float) (obstacle.getAngle() * 180.0 / Math.PI));
            transform.preTranslate(pos.x * u, pos.y * u);

            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.RED);
            batch.outline(recastSensorOutline, transform);
            batch.setColor(Color.WHITE);
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
        }

        public void drawSensorDebug (SpriteBatch batch, Path2 outline, Color color)
        {
            if (outline!= null) {
                batch.setTexture( Texture2D.getBlank() );
                batch.setColor( color );

                Vector2 p = obstacle.getPosition();
                float a = obstacle.getAngle();
                float u = obstacle.getPhysicsUnits();

                transform.idt();
                transform.preRotate( (float) (a * 180.0f / Math.PI) );
                transform.preTranslate( p.x * u, p.y * u );

                batch.outline( outline, transform );
            }
        }

        public void drawTeleportRadius(SpriteBatch batch)
        {
            PathFactory pathTool = new PathFactory();
            float u = obstacle.getPhysicsUnits();
            Path2 teleportCircle = pathTool.makeCircle(obstacle.getPosition().x * u , obstacle.getPosition().y * u  , teleportRangeRadius);
            batch.setColor(Color.RED);
            batch.outline(teleportCircle);
            batch.setColor(Color.WHITE);
        }
        public float getBlindProgress() {
            return MathUtils.clamp(blindTimer / MAX_BLIND_TIME, 0f, 1f);
        }
        public void setBlindTimer(float time) {
            this.blindTimer = time;
        }
        public int getTakeDamageCooldown() {
            return takeDamageCooldown;
        }
        public float getHeight() {
            return height;
        }

    }
