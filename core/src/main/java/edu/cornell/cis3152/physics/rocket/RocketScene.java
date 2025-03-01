/*
 * RocketScene.java
 *
 * This is the game scene (player mode) specific to the rocket lander mini-game.
 * This is one of the files that you are expected to modify. Please limit
 * changes to the regions that say INSERT CODE HERE.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.rocket;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.cis3152.physics.*;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.physics2.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.util.RandomGenerator;

/**
 * The game scene for the rocket lander game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class RocketScene extends PhysicsScene implements ContactListener {
    // Physics objects for the game

    /** Reference to the goalDoor (for collision detection) */
    private Box goalDoor;
    /** Reference to the rocket/player avatar */
    private Rocket rocket;

    // Global audio

    /** The sounds for collisions */
    private SoundEffect bumpSound;
    /** The maximum number of bumps allowed */
    private int maxBumps;
    /** The current bump index (for counting) */
    private int bumpIndx;
    /** Volume for bumping */
    private float bumpVol;
    /** Threshold for generating sound on collision */
    private float bumpThresh;

    /**
     * Creates and initializes a new instance of the rocket lander game
     *
     * The game has default gravity and other settings
     */
    public RocketScene(AssetDirectory directory) {
        super(directory,"rocket");
        world.setContactListener(this);
        bumpSound = directory.getEntry( "rocket-bump", SoundEffect.class );
        bumpVol = constants.get("collisions").getFloat( "volume", 1 );
        maxBumps = constants.get("collisions").getInt( "maximum", 1 );
        bumpThresh = constants.get("collisions").getFloat( "threshold", 0 );
        bumpIndx = 0;
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        JsonValue values = constants.get("world");
        Vector2 gravity = new Vector2(0, values.getFloat( "gravity" ));

        for(ObstacleSprite sprite : sprites) {
            Obstacle obj = sprite.getObstacle();
            sprite.getObstacle().deactivatePhysics(world);
        }
        sprites.clear();
        addQueue.clear();
        if (world != null) {
            world.dispose();
        }

        world = new World(gravity,false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        float units = height/bounds.height;

        // Add level goal
        Texture texture = directory.getEntry( "shared-goal", Texture.class );

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");
        goalDoor = new Box(goalpos.getFloat(0),goalpos.getFloat(1),
                                 units, goal);
        goalDoor.setTexture( texture );

        Obstacle obj = goalDoor.getObstacle();
        obj.setName("goal");
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setSensor(true);

        addSprite(goalDoor);

        // Create ground pieces
        texture = directory.getEntry( "shared-earth", Texture.class );

        Surface wall;
        JsonValue walls = constants.get("walls");
        JsonValue walljv = walls.get("positions");
        wall = new Surface(walljv.get(0).asFloatArray(), units, walls);
        wall.getObstacle().setName("wall1");
        wall.setTexture( texture );
        addSprite(wall);

        wall = new Surface(walljv.get(1).asFloatArray(), units, walls);
        wall.getObstacle().setName("wall2");
        wall.setTexture( texture );
        addSprite(wall);

        wall = new Surface(walljv.get(2).asFloatArray(), units, walls);
        wall.getObstacle().setName("wall3");
        wall.setTexture( texture );
        addSprite(wall);

        // Create the pile of boxes
        JsonValue crates = constants.get("boxes");
        JsonValue boxjv = crates.get("positions");
        for (int ii = 0; ii < boxjv.size; ii += 2) {
            int id = RandomGenerator.getInt(0,1)+1;
            texture = directory.getEntry( "rocket-crate0"+id, Texture.class );

            float sz  = crates.getFloat( "size" );
            Box box = new Box(boxjv.getFloat(ii), boxjv.getFloat(ii+1),
                                          units, crates);
            box.getObstacle().setName("crate"+id);
            box.setTexture( texture );
            addSprite(box);
        }

        // Create the rocket avatar
        texture = directory.getEntry( "rocket-rocket", Texture.class );

        JsonValue rockjv = constants.get("rocket");
        rocket = new Rocket(units, rockjv);
        rocket.setTexture(texture);


        // Attach the sprites
        SpriteSheet sprite;
        SoundEffect effect;

        sprite = directory.getEntry("rocket-main.fire", SpriteSheet.class);
        effect = directory.getEntry("rocket-mainburn", SoundEffect.class);
        rocket.setBurnerSheet( Rocket.Burner.MAIN,  sprite);
        rocket.setBurnerSound( Rocket.Burner.MAIN, effect );

        sprite = directory.getEntry("rocket-left.fire", SpriteSheet.class);
        effect = directory.getEntry("rocket-leftburn", SoundEffect.class);
        rocket.setBurnerSheet( Rocket.Burner.LEFT,  sprite);
        rocket.setBurnerSound( Rocket.Burner.LEFT, effect );

        sprite = directory.getEntry("rocket-right.fire", SpriteSheet.class);
        effect = directory.getEntry("rocket-rightburn", SoundEffect.class);
        rocket.setBurnerSheet( Rocket.Burner.RIGHT,  sprite);
        rocket.setBurnerSound( Rocket.Burner.RIGHT, effect );

        addSprite( rocket );
    }

    /**
     * Advances the core gameplay loop of this world.
     *
     * This method contains the specific update code for this mini-game. It
     * does not handle collisions, as those are managed by the parent class
     * PhysicsScene. This method is called after input is synced to the current
     * frame, but before collisions are resolved. The very last thing that it
     * should do is apply forces to the appropriate objects.
     *
     * @param dt    Number of seconds since last animation frame
     */
    public void update(float dt) {
        //#region INSERT CODE HERE
        // Read from the input and add the force to the rocket model
        // Then apply the force using the method you modified in RocketObject

        InputController input = InputController.getInstance();
        rocket.setFX(rocket.getThrust() * input.getHorizontal());
        rocket.setFY(rocket.getThrust() * input.getVertical());
        rocket.applyForce();
        //#endregion

        // Animate the three burners
        rocket.updateBurners();
    }

    /// CONTACT LISTENER METHODS
    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.
     * We use this method to test if it is the "right" kind of collision. In
     * particular, we use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    public void beginContact(Contact contact) {
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();

        // Recover the obstacle (NOT THE SPRITE) from the body
        ObstacleSprite sprite1 = (ObstacleSprite)body1.getUserData();
        ObstacleSprite sprite2 = (ObstacleSprite)body2.getUserData();

        if( (sprite1.getName() == "rocket" && sprite2.getName() == "goal") ||
            (sprite1.getName() == "goal"   && sprite2.getName() == "rocket")) {
            if (!isComplete()) {
                setComplete( true );
            }
        }
    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  We do not use it.
     */
    public void endContact(Contact contact) {}

    private final Vector2 cache = new Vector2();

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    /**
     * Makes any modifications necessary before collision resolution
     *
     * This method is called just before Box2D resolves a collision. We use
     * this method to implement sound on contact, using the algorithms outlined
     * similar to those in Ian Parberry's "Introduction to Game Physics with Box2D".
     *
     * However, we cannot use the proper algorithms, because LibGDX does not
     * implement b2GetPointStates from Box2D. The danger with our approximation
     * is that we may get a collision over multiple frames (instead of detecting
     * the first frame), and so play a sound repeatedly. Fortunately, our limit
     * to the number of simultaneous sounds addresses this issue.
     *
     * @param  contact      The two bodies that collided
     * @param  oldManifold  The collision manifold before contact
     */
    public void preSolve(Contact contact, Manifold oldManifold) {
        float speed = 0;

        // Use Ian Parberry's method to compute a speed threshold
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();
        WorldManifold worldManifold = contact.getWorldManifold();
        Vector2 wp = worldManifold.getPoints()[0];
        cache.set(body1.getLinearVelocityFromWorldPoint(wp));
        cache.sub(body2.getLinearVelocityFromWorldPoint(wp));
        speed = cache.dot(worldManifold.getNormal());

        // Play a sound if above threshold (otherwise too many sounds)
        if (speed > constants.get("collisions").getFloat( "threshold" )) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play( "bump"+bumpIndx, bumpSound );
            bumpIndx = (bumpIndx +1 ) % maxBumps;
        }
    }

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        // THIS IS CRITICAL. THE AFTERBURNER IS ON A LOOP.
        // IF YOU DO NOT STOP IT THE AFTERBURNER WILL PLAY FOREVER!
        rocket.stopSounds();
    }
}
