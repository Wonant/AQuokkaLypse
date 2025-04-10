    /*
     * Player.java
     *
     * This is the class for Player Nathans-Kelly cartoon avatar. WHile it is also
     * an ObstacleSprite, this class is much more than an organizational tool. This
     * class has all sorts of logic, like the whether Player can jump or whether
     * Player can fire a bullet.
     *
     * You SHOULD NOT need to modify this file. However, you may learn valuable
     * lessons for the rest of the lab by looking at it.
     *
     * Based on the original PhysicsDemo Lab by Don Holden, 2007
     *
     * Author:  Walker M. White
     * Version: 2/8/2025
     */
    package edu.cornell.cis3152.physics.platform;

    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

    /**
     * Player's avatar for the platform game.
     *
     * An ObstacleSprite is a sprite (specifically a textured mesh) that is
     * connected to a obstacle. It is designed to be the same size as the
     * physics object, and it tracks the physics object, matching its position
     * and angle at all times.
     *
     * Note that unlike a traditional ObstacleSprite, this attaches some additional
     * information to the obstacle. In particular, we add a sensor fixture. This
     * sensor is used to prevent double-jumping. However, we only have one mesh,
     * the mesh for Player. The sensor is invisible and only shows up in debug mode.
     * While we could have made the fixture a separate obstacle, we want it to be a
     * simple fixture so that we can attach it to the obstacle WITHOUT using joints.
     */
    public class Player extends ObstacleSprite {
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
        /** The impulse for the character jump */
        private float jump_force;

        /** Cooldown (in animation frames) for jumping */
        private int jumpLimit;
        /** How long until we can jump again */
        private int jumpCooldown;
        /** Whether we are actively jumping */
        private boolean isJumping;
        private boolean lastJumping;

        /** Cooldown (in animation frames) for harvesting */
        private int harvestLimit;
        /** How long until we can harvest again */
        private int harvestCooldown;
        /** Whether we are actively harvesting */
        private boolean isHarvesting;

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

        /** The current horizontal movement of the character */
        private float   movement;
        /** Which direction is the character facing */
        private boolean faceRight;
        /** Whether our feet are on the ground */
        private boolean isGrounded;
        /** Whether our feet are on a shadowed tile*/
        private boolean isInShadow;


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



        /** raycasting for stair interpolation */
        private float stepRayLength;
        private PlayerVisionRaycast playerVisionRaycast;
        private Vector2 debugRayStart;
        private Vector2 debugRayEnd;
        private boolean seenAStep;

        /** animation */
        private Animator walkingSprite;
        private Animator idleSprite;
        private Animator jumpSprite;
        private AnimationState animationState;


        private enum AnimationState {
            WALK,
            IDLE,
            JUMP,
            STAIR
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
            movement = value;
            // Change facing if appropriate
            if (movement < 0) {
                faceRight = false;
            } else if (movement > 0) {
                faceRight = true;
            }
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
            return isHarvesting && harvestCooldown <= 0;
        }

        public float getHarvestCooldown() {
            return harvestCooldown;
        }

        /**
         * Sets whether Player is actively firing.
         *
         * @param value whether Player is actively firing.
         */
        public void setHarvesting(boolean value) {
            isHarvesting = value;
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


        /**
         * Returns true if CatDemon is actively harvesting.
         *
         * @return true if CatDemon is actively harvesting.
         */
        public boolean isTeleporting() {
            return isTeleporting && teleportCooldown <= 0;
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
         * Returns true if CatDemon is actively taking damage.
         *
         * @return true if CatDemon is actively taking damage.
         */
        public boolean isTakingDamage() {
            return isTakingDamage && takeDamageCooldown <= 0;
        }

        /**
         * Sets whether CatDemon is actively taking damage.
         *
         * @param value whether CatDemon is actively taking damage.
         */
        public void setTakingDamage(boolean value) {
            isTakingDamage = value;
        }


        /**
         * Returns true if Player is actively jumping.
         *
         * @return true if Player is actively jumping.
         */
        public boolean isJumping() {
            return isJumping && isGrounded && jumpCooldown <= 0;
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
        public Player(float units, JsonValue data) {
            this.data = data;
            JsonValue debugInfo = data.get("debug");


            float x = data.get("pos").getFloat(0);
            float y = data.get("pos").getFloat(1);
            float s = data.getFloat( "size" );
            float sizeWidth = s*units;
            float sizeHeight = s*units*1.5f;



            // The capsule is smaller than the image
            // "inner" is the fraction of the original size for the capsule
            width = s*data.get("inner").getFloat(0);
            height = s*data.get("inner").getFloat(1);
            obstacle = new BoxObstacle(x, y, width, height*0.9f);
            //((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

            obstacle.setDensity( data.getFloat( "density", 0 ) );
            obstacle.setFriction( data.getFloat( "friction", 0 ) );
            obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
            obstacle.setFixedRotation(true);
            obstacle.setPhysicsUnits( units );
            obstacle.setUserData( this );
            obstacle.setName("player");

            debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
            sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);
            sensorScareColor = ParserUtils.parseColor( debugInfo.get("scare_sensor"),  Color.GREEN);

            maxspeed = data.getFloat("maxspeed", 0);
            damping = data.getFloat("damping", 0);
            force = data.getFloat("force", 0);
            jump_force = data.getFloat( "jump_force", 0 );
            jumpLimit = data.getInt( "jump_cool", 0 );
            harvestLimit = 60;
            stunLimit = data.getInt( "shot_cool", 0 );
            teleportLimit = data.getInt( "shot_cool", 0 );
            takeDamageLimit = 120;

            // Gameplay attributes
            isGrounded = false;
            isHarvesting = false;
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


            maxFearMeter = data.getInt("maxfear", 0);
            fearMeter = maxFearMeter;

            teleportRangeRadius = 200;

            // Create a rectangular mesh for Player. This is the same as for door,
            // since Player is a rectangular image. But note that the capsule is
            // actually smaller than the image, making a tighter hitbox. You can
            // see this when you enable debug mode.
            mesh.set(-sizeWidth/2.0f,-sizeHeight/2.0f,sizeWidth,sizeHeight);

            stepRayLength = height/2.5f;

            playerVisionRaycast = new PlayerVisionRaycast(PlayerVisionRaycast.VisionMode.STAIR_CHECK, stepRayLength * units);



        }

        public void createAnimators(Texture walkTexture, Texture idleTexture, Texture jumpTexture) {
            walkingSprite = new Animator(walkTexture, 4, 5, 0.08f, 17);
            idleSprite = new Animator(idleTexture, 6, 5, 0.08f, 30);
            jumpSprite = new Animator(jumpTexture, 12, 5, 0.08f, 50, false);
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
                filter.maskBits = CATEGORY_SCENERY;
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
            Vector2 sensorCenter = new Vector2(0, -height / 2);
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = data.getFloat("density",0);
            sensorDef.isSensor = true;

            JsonValue sensorjv = data.get("sensor");
            float w = sensorjv.getFloat("shrink",0)*width/2.0f;
            float h = sensorjv.getFloat("height",0);
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

        public void createScareSensor(){

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
            Fixture ScareSensorFixture = body.createFixture( scareSensorDef );
            sensorScareName = "scare_sensor";
            ScareSensorFixture.setUserData(sensorScareName);

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
            if (!obstacle.isActive()) {
                return;
            }

            if (isPlatformStep(world, stepRayLength)) {
                System.out.println("seen a step");
                seenAStep = true;
            } else {
                seenAStep = false;
            }


            Vector2 pos = obstacle.getPosition();
            float vx = obstacle.getVX();
            Body body = obstacle.getBody();



            // Don't want to be moving. Damp out player motion
            if (getMovement() == 0f) {
                forceCache.set(-getDamping()*vx,0);
                body.applyForce(forceCache,pos,true);
            }

            // Velocity too high, clamp it
            if (Math.abs(vx) >= getMaxSpeed()) {
                obstacle.setVX(Math.signum(vx)*getMaxSpeed());
            } else {
                forceCache.set(getMovement(),0);
                body.applyForce(forceCache,pos,true);
            }

            if (isJumping()) {
                jumpSprite.reset();
                forceCache.set(0, jump_force);
                body.applyLinearImpulse(forceCache,pos,true);
            }

            //smoothStairClimb();
        }

        public boolean isPlatformStep(World world, float raylength) {
            Vector2 start = (isFacingRight()) ?
                obstacle.getBody().getPosition().cpy().add(width/2 + 0.1f, 0) :
                obstacle.getBody().getPosition().cpy().add(-width/2 - 0.1f, 0);
            Vector2 end = start.cpy().add(0, -raylength);


            debugRayStart = start;
            debugRayEnd = end;

            world.rayCast(playerVisionRaycast, start, end);

            if (playerVisionRaycast.getHitFixture() == null) {
                return false;
            } else if (playerVisionRaycast.fixtureIsStair) {
                Vector2 stairHit = new Vector2(playerVisionRaycast.getHitPoint());


                if (isGrounded && Math.abs(movement) > 0) {
                    float targetCenterY = stairHit.y + height/2;
                    Body body = obstacle.getBody();
                    Vector2 pos = body.getPosition();
                    body.setTransform(stairHit.x, targetCenterY, body.getAngle());

                    debugRayEnd = stairHit;
                }
            }

            playerVisionRaycast.reset();

            return true;
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
            // change character state

            if (isGrounded) {
                if (movement == 0) {
                    animationState = AnimationState.IDLE;
                } else {
                    if (seenAStep) {
                        animationState = AnimationState.STAIR;
                    } else {
                        animationState = AnimationState.WALK;
                    }
                }
            } else {
                animationState = AnimationState.JUMP;
            }

            // Apply cooldowns
            if (isJumping()) {
                jumpCooldown = jumpLimit;
            } else {
                jumpCooldown = Math.max(0, jumpCooldown - 1);
            }

            if (isHarvesting()) {
                harvestCooldown = harvestLimit;
            } else {
                harvestCooldown = Math.max(0, harvestCooldown - 1);
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

            } else {
                takeDamageCooldown = Math.max(0, takeDamageCooldown - 1);
            }
            super.update(dt);

        }

        @Override
        public void draw(SpriteBatch batch) {

            TextureRegion frame = new TextureRegion();
            float scale = 1f;
            switch (animationState) {
                case WALK:
                    frame = walkingSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                    break;
                case IDLE:
                    frame = idleSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                    break;
                case JUMP:
                    if (!lastJumping && isJumping) {
                        jumpSprite.reset();
                    }
                    scale = 1.3f;
                    frame = jumpSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                    break;
                default:
                    frame = idleSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                    break;
            }

            float u = obstacle.getPhysicsUnits();
            // Determine drawing coordinates.
            // Here we assume obstacle.getX() and getY() return the center position.
            float posX = obstacle.getX() * u;
            float posY = obstacle.getY() * u;
            float drawWidth = width * u * 2f * scale;
            float drawHeight = height * u * scale;

            float originX = drawWidth / 2f;
            float originY = drawHeight / 2f;

            if (faceRight) {

            } else {
                frame.flip(true, false);
            }

            // Draw the current frame centered on the player's position.
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

//            if (faceRight) {
//                flipCache.setToScaling( 1,1 );
//            } else {
//                flipCache.setToScaling( -1,1 );
//            }
//            super.draw(batch,flipCache);

        }

        @Override
        public void drawDebug(SpriteBatch batch) {
            super.drawDebug( batch );

            drawSensorDebug(batch, sensorOutline, sensorColor);
            drawSensorDebug(batch, sensorScareOutline, sensorScareColor);
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

    }
