/*
 * BubbleGenerator.java
 *
 * This object is only a body. It does not have a fixture and does not collide.
 * It is a physics object so that we can weld it to the ragdoll mask. That
 * way it always looks like bubles are coming from the snorkle, no matter
 * which way the head moves.
 *
 * Even though we have no fixtures, we still make this an ObstacleMesh so that
 * we can have meshes for the bubbles. Like an ObstacleSprite, the initial
 * drawing of the sprite tracks the body. But once it leaves the snorkle, it
 * moves on its own.
 *
 * This is another example of a particle system.  Like the photons in the first
 * lab, it preallocates all of its objects ahead of time. However, this time we
 * use the built-in memory pool from LibGDX to do it.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.ragdoll;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import edu.cornell.gdiac.util.RandomGenerator;

/**
 * Physics object that generates non-physics bubble shapes.
 *
 * This class has a particle system for generating bubbles. When a bubble is
 * created, it tracks the location of the bubble generator (using the API of
 * ObstacleSprite). But after that, it travels straight upwards until it is
 * off the screen.
 */
public class BubbleGenerator extends ObstacleSprite {

    /** Representation of the bubbles for drawing purposes. */
    private class Particle implements Pool.Poolable {
        /** Position of the bubble in Box2d space */
        public Vector2 position;
        /** The number of animation frames left to live */
        public int life;

        /** Creates a new Particle with no lifespace */
        public Particle() {
            position = new Vector2();
            life = -1;
        }

        /** Resets the particle so it can be reclaimed by the pool */
        public void reset() {
            position.set(0,0);
            life = -1;
        }
    }

    /**
     * Memory pool supporting the particle system.
     *
     * This pool preallocates all of the particles.  When a particle dies, it is
     * released back to the pool for reuse.
     */
    private class ParticlePool extends Pool<Particle> {
        /**
         * That is all we got
         */
        private static final int MAX_PARTICLES = 6;
        /**
         * The backing list of particles
         */
        private Particle[] particles;
        /**
         * The current allocation position in the array
         */
        private int offset;

        /**
         * Creates a new pool to allocate Particles
         * <p>
         * This constructor preallocates the objects
         */
        public ParticlePool() {
            super();
            particles = new Particle[MAX_PARTICLES];
            for (int ii = 0; ii < MAX_PARTICLES; ii++) {
                particles[ii] = new Particle();
            }
            offset = 0;
        }

        /**
         * Returns the backing list (so that we can iterate over it)
         *
         * @return the backing list
         */
        public Particle[] getPool() {
            return particles;
        }

        /**
         * Returns the next available object in the backing list
         * <p>
         * If the backing list is exhausted, we return null
         *
         * @return the next available object in the backing list
         */
        protected Particle newObject() {
            if (offset < particles.length) {
                offset++;
                return particles[offset - 1];
            }
            return null;  // OUT OF MEMORY
        }
    }

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** Memory pool to allocate new particles */
    private ParticlePool memory;

    // Dimensional information for drawing texture.
    /** The welding offset for this bubbler */
    private Vector2 offset;
    /** The size dimension of a bubble */
    private Vector2 dimension;

    /** How long bubbles live after creation */
    private int lifespan;
    /** The maximum time beteween bubbles */
    private int timelimit;
    /** How long until we can make another bubble */
    private int cooldown;
    /** Whether we made a bubble this animation frame */
    private boolean bubbled;

    private SoundEffect[] bubbleSounds;

    public Vector2 getOffset() {
        return offset;
    }

    /**
     * Creates a new bubble generator with the given physics data.
     *
     * The data is used to define the properties of the drawn meshes, not
     * the body (as it has no fixtures or mass).
     *
     * @param units     The physics units
     * @param data      The constants for the bubbler
     */
    public BubbleGenerator(float units, JsonValue data) {
        super();
        JsonValue bodyPos = data.get("doll").get("torso").get("position");
        JsonValue headPos = data.get("doll").get("head").get("offset");
        JsonValue bubbPos = data.get("bubbles").get("offset");

        offset = new Vector2(bubbPos.getFloat(0), bubbPos.getFloat(1));
        float x = bodyPos.getFloat(0)+headPos.getFloat(0)+offset.x;
        float y = bodyPos.getFloat(1)+headPos.getFloat(1)+offset.y;
        float radius = data.get("bubbles").getFloat("size",0);

        this.data = data.get("bubbles");

        debug = ParserUtils.parseColor( this.data.get("debug"),  Color.WHITE);

        obstacle = new WheelObstacle(x, y, radius);
        obstacle.setDensity( this.data.getFloat("density",0));
        obstacle.setPhysicsUnits( units );
        obstacle.setName("bubbler");
        obstacle.setUserData( this );

        // THis will be the mesh of the bubbles, not the bubbler
        // For all meshes attached to a physics body, we want (0,0) to be in
        // the center of the mesh. Note that we scale by the physics units.
        mesh.set(-radius*units,-radius*units,2*radius*units,2*radius*units);

        // Other attributes
        lifespan = this.data.getInt("lifespan",0);
        timelimit = this.data.getInt("cooldown",0);
        cooldown = 0;
        bubbled = false;
        memory = new ParticlePool();
    }

    /**
     * Sets the assets for this bubble generator
     *
     * In addition to assigning a texture for the bubble, we also want sounds
     * for when bubbles are generated. We extract them from the asset directory
     * here.
     *
     * @param directory The directory of loaded assets
     */
    public void setAssets(AssetDirectory directory) {
        setTexture( directory.getEntry("ragdoll-bubble",Texture.class) );

        bubbleSounds = new SoundEffect[data.getInt("sounds",0)];
        for(int ii = 0; ii < bubbleSounds.length; ii++) {
            bubbleSounds[ii] = directory.getEntry( "ragdoll-glub"+(ii+1), SoundEffect.class );
        }
    }

    /** Generates a new bubble object and put it on the screen. */
    private void bubble() {
        Particle p = memory.obtain();
        if (p != null) {
            p.position.set(obstacle.getPosition());
            p.life = lifespan;

            // Pick a sound
            int indx =  RandomGenerator.getInt(0,bubbleSounds.length-1);
            bubbleSounds[indx].play(); // This time it is okay to play simultaneous copies
        }
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method for cooldowns and bubble movement.
     *
     * @param v Number of seconds since last animation frame
     */
    public void update(float dt) {
        float x = obstacle.getX();
        float y = obstacle.getY();
        float u = obstacle.getPhysicsUnits();
        for(Particle p : memory.getPool()) {
            if (p.life > 0) {
                p.position.y += 1/u;
                p.life -= 1;
                if (p.life == 0) {
                    memory.free(p);
                }
            }
        }

        if (cooldown == 0) {
            bubbled = true;
            bubble();
            cooldown = timelimit;
        } else {
            bubbled = false;
            cooldown--;
        }
    }

    /**
     * Draws the bubbles.
     *
     * This loops through the particle system and draws the bubble at each
     * position. Note that we have a shared mesh (the one for this ObstacleSprite).
     * We just translate it to the position of each bullet.
     *
     * @param batch The sprite batch to draw to
     */
    public void draw(SpriteBatch batch) {
        if (sprite.getTexture() == null) {
            return;
        }

        float w = sprite.getRegionWidth();
        float h = sprite.getRegionHeight();
        float u = obstacle.getPhysicsUnits();
        batch.setColor(Color.WHITE);
        batch.setTextureRegion( sprite );

        for(Particle p : memory.getPool()) {
            if (p.life > 0) {
                float x = p.position.x*u;
                float y = p.position.y*u;

                // Draw the flames
                transform.idt();
                transform.translate(x, y);
                batch.drawMesh(mesh, transform, false);
            }
        }
    }
}
