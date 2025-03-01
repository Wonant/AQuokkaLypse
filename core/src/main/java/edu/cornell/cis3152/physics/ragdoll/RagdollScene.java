/*
 * RagdollScene.java
 *
 * This is the game scene (player mode) specific to the ragdoll mini-game.
 * You SHOULD NOT need to modify this file. However, you may learn valuable
 * lessons for the rest of the lab by looking at it.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.ragdoll;

import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSelector;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.RandomGenerator;

import edu.cornell.cis3152.physics.*;

/**
 * The game scene for the ragdoll fishtank.
 *
 * Look at the methods {@link #populateLevel} for how we initialize the scene.
 * Other than that, the only other interesting thing is the ObstacleSelector
 * (we do not have a ContactListener for this mini-game). This allows us to
 * pick up physics objects with a mouse.
 */
public class RagdollScene extends PhysicsScene {

    /** Texture asset for mouse crosshairs */
    private TextureRegion crosshairTexture;
    /** Texture asset for background image */
    private TextureRegion backgroundTexture;
    /** Texture asset for watery foreground */
    private TextureRegion foregroundTexture;
    /** The transparency for foreground image */
    private Color foregroundColor;

    /** Reference to the character's ragdoll */
    private Ragdoll ragdoll;

    /** Mouse selector to move the ragdoll */
    private ObstacleSelector selector;

    /**
     * Creates and initialize a new instance of the ragdoll fishtank
     *
     * The world has lower gravity to simulate being underwater.
     */
    public RagdollScene(AssetDirectory directory) {
        super(directory,"ragdoll");

        // These are the assets managed by this class
        // All other assets go to their models
        crosshairTexture  = new TextureRegion(directory.getEntry( "ragdoll-crosshair", Texture.class ));
        backgroundTexture = new TextureRegion(directory.getEntry( "ragdoll-background", Texture.class ));
        foregroundTexture = new TextureRegion(directory.getEntry( "ragdoll-foreground", Texture.class ));
        foregroundColor = ParserUtils.parseColor(constants.get("world").get("forecolor"), Color.RED);
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
        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        float units = height/ bounds.height;

        Texture texture;

        // Make the ragdoll
        ragdoll = new Ragdoll(units, constants);
        ragdoll.setAssets( directory );
        ragdoll.getBubbleGenerator().setAssets( directory );
        addSpriteGroup(ragdoll);

        // Create ground pieces
        texture = directory.getEntry( "shared-earth", Texture.class );

        // This time we show that we do not need a dedicated wall class
        Wall wall;
        JsonValue walls = constants.get("walls");
        JsonValue walljv = walls.get("positions");
        wall = new Wall(walljv.get(0).asFloatArray(), units, walls);
        wall.getObstacle().setName("wall1");
        wall.setTexture( texture );
        addSprite(wall);

        wall = new Wall(walljv.get(1).asFloatArray(), units, walls);
        wall.getObstacle().setName("wall2");
        wall.setTexture( texture );
        addSprite(wall);

        selector = new ObstacleSelector(world);
        // TODO: Rename that one
        selector.setTexture(directory.getEntry( "ragdoll-crosshair", Texture.class ));
        selector.setPhysicsUnits(units);
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
        // Move an object if touched
        InputController input = InputController.getInstance();
        if (input.didTertiary() && !selector.isSelected()) {
            selector.select(input.getCrossHair().x,input.getCrossHair().y);
        } else if (!input.didTertiary() && selector.isSelected()) {
            selector.deselect();
        } else {
            selector.moveTo(input.getCrossHair().x,input.getCrossHair().y);
        }
    }


    /**
     * Draws the physics objects to the screen
     *
     * We override the default draw method to add a foreground, a background,
     * and cross hairs for the ObstacleSelector.
     *
     * @param dt    Number of seconds since last animation frame
     */
    @Override
    public void draw(float dt) {
        float units = height/bounds.height;

        // Color is irrelevant for this mini-game
        ScreenUtils.clear( Color.BLACK );

        batch.begin(camera);
        batch.setColor(Color.WHITE);
        batch.draw(backgroundTexture,0,0, width, height);

        for(ObstacleSprite obj : sprites) {
            obj.draw(batch);
        }

        if (debug) {
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug( batch );
            }
        }

        // Draw the crosshair
        InputController input = InputController.getInstance();
        float x = input.getCrossHair().x*units-units/2;
        float y = input.getCrossHair().y*units-units/2;
        batch.draw(crosshairTexture, x, y, units, units);

        batch.setColor(foregroundColor);
        batch.draw(foregroundTexture, 0, 0, width, height);

        batch.end();
    }

}
