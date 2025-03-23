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
            obstacle = new CapsuleObstacle(x, y, width, height);
            ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

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

            teleportRangeRadius = 300;

            // Create a rectangular mesh for Player. This is the same as for door,
            // since Player is a rectangular image. But note that the capsule is
            // actually smaller than the image, making a tighter hitbox. You can
            // see this when you enable debug mode.
            mesh.set(-sizeWidth/2.0f,-sizeHeight/2.0f,sizeWidth,sizeHeight);
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
        public void applyForce() {
            if (!obstacle.isActive()) {
                return;
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
                forceCache.set(0, jump_force);
                body.applyLinearImpulse(forceCache,pos,true);
            }

            //smoothStairClimb();
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

        public void smoothStairClimb() {
            if (!isGrounded() || Math.abs(getMovement()) < 0.1f) {
                return;
            }

            Body body = obstacle.getBody();
            Vector2 pos = obstacle.getPosition();
            World world = body.getWorld();

            float sensorWidth = width * 0.8f;
            float sensorHeight = height * 0.2f;  // adjust this to maximum step height
            float recastOffset = sensorHeight + 0.2f; // offset from bottom of player

            float horizontalOffset = isFacingRight() ? width / 2 : -width / 2;

            Vector2 sensorCenter = new Vector2(pos.x + horizontalOffset, pos.y - height / 2 + recastOffset);
            final boolean[] stepClear = { true };

            world.QueryAABB(new QueryCallback(){
                                @Override
                                public boolean reportFixture(Fixture fixture) {
                                    Object userData = fixture.getUserData();
                                    // Adjust the condition as needed to recognize your wall/platform fixtures.
                                    if (userData != null && userData.toString().startsWith("platform")) {
                                        stepClear[0] = false;
                                        return false; // stop the query early
                                    }
                                    return true;
                                }
                            },
                sensorCenter.x - sensorWidth / 2,
                sensorCenter.y - sensorHeight / 2,
                sensorCenter.x + sensorWidth / 2,
                sensorCenter.y + sensorHeight / 2);

            if (stepClear[0]) {
                float climbSpeed = 0.05f;  // change this value for faster or slower climbing
                body.setTransform(pos.x, pos.y + climbSpeed, body.getAngle());
            }
        }

        @Override
        public void draw(SpriteBatch batch) {
            if (faceRight) {
                flipCache.setToScaling( 1,1 );
            } else {
                flipCache.setToScaling( -1,1 );
            }
            super.draw(batch,flipCache);

        }

        @Override
        public void drawDebug(SpriteBatch batch) {
            super.drawDebug( batch );

            drawSensorDebug(batch, sensorOutline, sensorColor);
            drawSensorDebug(batch, sensorScareOutline, sensorScareColor);
            drawTeleportRadius(batch);
            //drawRecastSensor(batch);
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

        public void drawSensorDebug (SpriteBatch batch, Path2 outline, Color color)
        {
            if (outline!= null) {
                batch.setTexture( Texture2D.getBlank() );
                batch.setColor( color );

                Vector2 p = obstacle.getPosition();
                float a = obstacle.getAngle();
                float u = obstacle.getPhysicsUnits();

                // transform is an inherited cache variable
                transform.idt();
                transform.preRotate( (float) (a * 180.0f / Math.PI) );
                transform.preTranslate( p.x * u, p.y * u );

                //
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
