/*
 * Rocket.java
 *
 * This is the class for the rocket. WHile it is also an ObstacleSprite, this
 * class is much more than an organizational tool. This class has also manages
 * sounds and animations for the various rocket burners. Note how this class
 * combines physics and animation. This is a good template for models in your
 * game.
 *
 * This is one of the files that you are expected to modify. Please limit
 * changes to the regions that say INSERT CODE HERE.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.rocket;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.physics2.*;
import edu.cornell.gdiac.graphics.*;

/**
 * The player avatar for the rocket lander game.
 *
 * An ObstacleSprite is a sprite (specifically a textured mesh) that is
 * connected to a obstacle. It is designed to be the same size as the
 * physics object, and it tracks the physics object, matching its position
 * and angle at all times.
 *
 * Note that unlike a traditional ObstacleSprite, this class does not just
 * have one mesh. It also has some associated sprite sheets for animation
 * of the burners. We do not need to add fixtures or additional bodies for
 * these burners. We just draw the sprite sheets relative to the physics body.
 */
public class Rocket extends ObstacleSprite {
    /**
     * An enumeration to identify the rocket afterburner
     */
    public enum Burner {
        /** The main afterburner */
        MAIN,
        /** The left side thruster */
        LEFT,
        /** The right side thruster */
        RIGHT
    };

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    /** The upward force for the afterburners */
    private final float thrust;
    /** The size of this rocket in pixels */
    private Vector2 size;

    /** The sprite sheet for the main afterburner */
    private SpriteSheet mainBurner;
    /** The associated sound for the main afterburner */
    private SoundEffect mainSound;
    /** The animation phase for the main afterburner */
    private boolean mainCycle = true;

    /** The sprite sheet for the left side burner */
    private SpriteSheet leftBurner;
    /** The associated sound for the left side burner */
    private SoundEffect leftSound;
    /** The animation phase for the left side burner */
    private boolean leftCycle = true;

    /** The sprite sheet for the right side burner */
    private SpriteSheet rghtBurner;
    /** The associated sound for the right side burner */
    private SoundEffect rghtSound;
    /** The associated sound for the right side burner */
    private boolean rghtCycle  = true;

    /** Volume for afterburners */
    private float burnVol;

    /** Cache object for the force to apply to this rocket */
    private final Vector2 force = new Vector2();
    /** Cache object for transforming the force according the object angle */
    private final Affine2 affineCache = new Affine2();

    /**
     * Returns the force applied to this rocket.
     *
     * This method returns a reference to the force vector, allowing it to be
     * modified. Remember to modify the input values by the thrust amount before
     * assigning the value to force.
     *
     * @return the force applied to this rocket.
     */
    public Vector2 getForce() {
        return force;
    }

    /**
     * Returns the x-component of the force applied to this rocket.
     *
     * Remember to modify the input values by the thrust amount before assigning
     * the value to force.
     *
     * @return the x-component of the force applied to this rocket.
     */
    public float getFX() {
        return force.x;
    }

    /**
     * Sets the x-component of the force applied to this rocket.
     *
     * Remember to modify the input values by the thrust amount before assigning
     * the value to force.
     *
     * @param value the x-component of the force applied to this rocket.
     */
    public void setFX(float value) {
        force.x = value;
    }

    /**
     * Returns the y-component of the force applied to this rocket.
     *
     * Remember to modify the input values by the thrust amount before assigning
     * the value to force.
     *
     * @return the y-component of the force applied to this rocket.
     */
    public float getFY() {
        return force.y;
    }

    /**
     * Sets the x-component of the force applied to this rocket.
     *
     * Remember to modify the input values by the thrust amount before assigning
     * the value to force.
     *
     * @param value the x-component of the force applied to this rocket.
     */
    public void setFY(float value) {
        force.y = value;
    }

    /**
     * Returns the amount of thrust that this rocket has.
     *
     * Multiply this value times the horizontal and vertical values in the
     * input controller to get the force.
     *
     * @return the amount of thrust that this rocket has.
     */
    public float getThrust() {
        return thrust;
    }

    /**
     * Creates a new rocket with the given physics data.
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file.
     *
     * @param units     The physics units
     * @param constants The physics constants for the rocket
     */
    public Rocket(float units, JsonValue constants) {
        this.data = constants;

        // Get box2d units and pixel units
        float x = data.get( "pos" ).getFloat(0);
        float y = data.get( "pos" ).getFloat(1);
        float sx = data.get( "size" ).getFloat(0);
        float sy = data.get( "size" ).getFloat(1);
        size = new Vector2(sx*units, sy*units);


        obstacle = new BoxObstacle(x, y, sx, sy);

        obstacle.setDensity( data.getFloat( "density", 0 ) );
        obstacle.setFriction( data.getFloat( "friction", 0 ) );
        obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );

        thrust = data.getFloat( "thrust", 0.0f );
        debug = ParserUtils.parseColor( data.get("debug"),  Color.WHITE);

        // Create a rectangular mesh with a standard texture
        mesh.set(-size.x/2.0f,-size.y/2.0f,size.x,size.y);
        obstacle.setName("rocket");

        //#region INSERT CODE HERE
        // Insert code here to prevent the body from rotating
        obstacle.setFixedRotation(true);

        //#endregion

        burnVol = data.getFloat("volume", 1);

    }

    /**
     * Applies the force to the body of this ship
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        // Orient the force with rotation.
        affineCache.setToRotationRad(obstacle.getAngle());
        affineCache.applyTo(force);

        //#region INSERT CODE HERE
        // Apply force to the rocket BODY, not the rocket
        obstacle.getBody().applyForceToCenter(force, true);

        //#endregion
    }

    // Animation methods (DO NOT CHANGE)
    /**
     * Returns the sprite sheet for the given afterburner
     *
     * @param  burner   enumeration to identify the afterburner
     *
     * @return the sprite sheet for the given afterburner
     */
    public SpriteSheet getBurnerSheet(Burner burner) {
        switch (burner) {
            case MAIN:
                return mainBurner;
            case LEFT:
                return leftBurner;
            case RIGHT:
                return rghtBurner;
        }
        assert false : "Invalid burner enumeration";
        return null;
    }

    /**
     * Sets the sprite sheet for the given afterburner
     *
     * @param  burner   enumeration to identify the afterburner
     *
     * @param  strip     the sprite sheet for the given afterburner
     */
    public void setBurnerSheet(Burner burner, SpriteSheet strip) {
        switch (burner) {
            case MAIN:
                mainBurner = strip;
                break;
            case LEFT:
                leftBurner = strip;
                break;
            case RIGHT:
                rghtBurner = strip;
                break;
            default:
                assert false : "Invalid burner enumeration";
        }
    }

    /**
     * Returns the sound to accompany the given afterburner
     *
     * @param  burner   enumeration to identify the afterburner
     *
     * @return the sound to accompany the given afterburner
     */
    public Sound getBurnerSound(Burner burner) {
        switch (burner) {
            case MAIN:
                return mainSound;
            case LEFT:
                return leftSound;
            case RIGHT:
                return rghtSound;
        }
        assert false : "Invalid burner enumeration";
        return null;
    }

    /**
     * Sets the sound to accompany the given afterburner
     *
     * @param  burner   enumeration to identify the afterburner
     * @param  sound       the sound to accompany the given afterburner
     */
    public void setBurnerSound(Burner burner, SoundEffect sound) {
        switch (burner) {
            case MAIN:
                mainSound = sound;
                break;
            case LEFT:
                leftSound = sound;
                break;
            case RIGHT:
                rghtSound = sound;
                break;
            default:
                assert false : "Invalid burner enumeration";
        }
    }

    /**
     * Updates the sounds and animation for each burner
     */
    public void updateBurners() {
        updateBurner( Burner.MAIN, getFY() > 1);
        updateBurner( Burner.LEFT, getFX() > 1);
        updateBurner( Burner.RIGHT, getFX() < -1);
    }

    /**
     * Updates that animation and sound for a single burner
     *
     * In the past, we have done this type of thing in the scene because of
     * all the dependencies involved. However, because SoundEffectManager is
     * a singleton, that makes it easier to process this in the model now,
     * which is nicer for code organizaiton.
     *
     * @param  burner   The rocket burner to animate
     * @param  on       Whether to turn the animation on or off
     */
    private void updateBurner(Burner burner, boolean on) {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        SoundEffect effect = null;
        String key = "";
        switch (burner) {
            case MAIN:
                effect = mainSound;
                key = "mainburner";
                break;
            case LEFT:
                effect = leftSound;
                key = "leftburner";
                break;
            case RIGHT:
                effect = rghtSound;
                key = "rightburner";
                break;
            default:
                assert false : "Invalid burner enumeration";
        }

        if (on) {
            animateBurner(burner, true);
            if (effect != null && !sounds.isActive(key)) {
                sounds.play( key,  effect, burnVol, true);
            }
        } else {
            animateBurner(burner, false);
            if (sounds.isActive(key)) {
                sounds.stop(key);
            }
        }
    }

    /**
     * Animates the given burner.
     *
     * If the animation is not active, it will reset to the initial animation frame.
     *
     * @param  burner   The reference to the rocket burner
     * @param  on       Whether the animation is active
     */
    private void animateBurner(Burner burner, boolean on) {
        SpriteSheet node = null;
        boolean  cycle = true;

        switch (burner) {
            case MAIN:
                node  = mainBurner;
                cycle = mainCycle;
                break;
            case LEFT:
                node  = leftBurner;
                cycle = leftCycle;
                break;
            case RIGHT:
                node  = rghtBurner;
                cycle = rghtCycle;
                break;
            default:
                assert false : "Invalid burner enumeration";
        }

        if (node == null) {
            return;
        }

        if (on) {
            // Turn on the flames and go back and forth
            if (node.getFrame() == 0 || node.getFrame() == 1) {
                cycle = true;
            } else if (node.getFrame() == node.getSize()-1) {
                cycle = false;
            }

            // Increment
            if (cycle) {
                node.setFrame(node.getFrame()+1);
            } else {
                node.setFrame(node.getFrame()-1);
            }
        } else {
            node.setFrame(0);
        }

        switch (burner) {
        case MAIN:
            mainCycle = cycle;
            break;
        case LEFT:
            leftCycle = cycle;
            break;
        case RIGHT:
            rghtCycle = cycle;
            break;
        default:
            assert false : "Invalid burner enumeration";
        }
    }


    /**
     * Draws this rocket to the given sprite batch
     *
     * This method will draw the mesh for the rocket, as well as the animation
     * for the burners.
     *
     * @param batch    The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);

        float x = obstacle.getX();
        float y = obstacle.getY();
        float a = obstacle.getAngle();
        float u = obstacle.getPhysicsUnits();

        // Draw the flames
        batch.setColor(Color.WHITE);
        if (mainBurner != null) {
            // We need to place this relative to the ship
            // transform is an inherited cache variable
            float offsety = size.y/2-mainBurner.getRegionHeight();
            transform.idt();
            transform.preTranslate(-size.x/2, offsety);
            transform.preRotate((float)(a*180.0f/Math.PI));
            transform.preTranslate(x*u, y*u);

            batch.draw(mainBurner,transform);
        }
        if (leftBurner != null) {
            float fx = -leftBurner.getRegionWidth()/2.0f;
            float fy = -leftBurner.getRegionHeight()/2.0f;

            // We need to place this relative to the ship
            // transform is an inherited cache variable
            transform.idt();
            transform.preTranslate(fx,fy);
            transform.preRotate((float)(a*180.0f/Math.PI));
            transform.preTranslate(x*u, y*u);

            batch.draw(leftBurner,transform);
        }
        if (rghtBurner != null) {
            float fx = -rghtBurner.getRegionWidth()/2.0f;
            float fy = -rghtBurner.getRegionHeight()/2.0f;

            // We need to place this relative to the ship
            // transform is an inherited cache variable
            transform.idt();
            transform.preTranslate(fx,fy);
            transform.preRotate((float)(a*180.0f/Math.PI));
            transform.preTranslate(x*u, y*u);

            batch.draw(rghtBurner,transform);
        }
    }

    /**
     * Stops the sounds associated with this rocket.
     *
     * Because these sounds are on a loop, we cannot wait for them to end when
     * we wish to switch scenes. Instead we must stop them manually.
     */
    public void stopSounds() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop( "mainburner" );
        sounds.stop( "leftburner" );
        sounds.stop( "rightburner" );
    }

}
