/*
 * PlatformScene.java
 *
 * This is the game scene (player mode) specific to the platforming mini-game.
 * You SHOULD NOT need to modify this file. However, you may learn valuable
 * lessons for the rest of the lab by looking at it.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.g2d.Sprite;
import edu.cornell.cis3152.physics.AIControllerManager;
import edu.cornell.cis3152.physics.ObstacleGroup;

import java.util.HashMap;
import java.util.Iterator;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.cis3152.physics.InputController;
//import edu.cornell.cis3152.physics.rocket.Box;
import edu.cornell.gdiac.assets.AssetDirectory;
//import edu.cornell.cis3152.physics.PhysicsScene;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.physics2.*;

/**
 * The game scene for the platformer game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class PlatformScene implements ContactListener, Screen{
    // SOME EXIT CODES FOR GDXROOT
    /** Exit code for quitting the game */
    public static final int EXIT_QUIT = 0;
    /** Exit code for advancing to next level */
    public static final int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 180;

    public static final int STUN_COST = 1;
    public static final int CREATE_TELEPORTER_COST = 2;
    public static final int TAKE_TELEPORTER_COST = 1;

    /** The asset directory for retrieving textures, atlases */
    protected AssetDirectory directory;
    /** The drawing camera for this scene */
    protected OrthographicCamera camera;
    /** Reference to the sprite batch */
    protected SpriteBatch batch;

    protected float width;
    protected float height;

    /** The physics constants */
    protected JsonValue constants;

    /** The font for giving messages to the player */
    protected BitmapFont displayFont;
    /** A layout for drawing a victory message */
    private TextLayout goodMessage;
    /** A layout for drawing a failure message */
    private TextLayout badMessage;

    /** Number of velocity iterations for the constraint solvers */
    public static final int WORLD_VELOC = 6;
    /** Number of position iterations for the constraint solvers */
    public static final int WORLD_POSIT = 2;

    /** All the objects in the world. */
    protected PooledList<ObstacleSprite> sprites  = new PooledList<ObstacleSprite>();
    /** Queue for adding objects */
    protected PooledList<ObstacleSprite> addQueue = new PooledList<ObstacleSprite>();
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;

    private Texture background;

    /** Whether this is an active controller */
    protected boolean active;
    /** Whether we have completed this level */
    protected boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    protected boolean failed;
    /** Whether debug mode is active */
    protected boolean debug;
    /** Countdown active for winning or losing */
    protected int countdown;


    /** Texture asset for character avatar */
    private TextureRegion avatarTexture;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;
    /** Texture asset for the bullet */
    private TextureRegion bulletTexture;
    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;

    /** The jump sound. We only want to play once. */
    private SoundEffect jumpSound;
    /** The weapon fire sound. We only want to play once. */
    private SoundEffect fireSound;
    /** The weapon pop sound. We only want to play once. */
    private SoundEffect plopSound;
    /** The default sound volume */
    private float volume;

    /** Reference to the character avatar */
    private Player avatar;
    private CuriosityCritter critter;
    private MindMaintenance maintenance;
    private DreamDweller dreamDweller;
    private TextureRegion visionConeRegion;
    private Texture vision;
    private Texture light;
    private int prev_debug;
    private Sprite visionCone;

    private HashMap<Enemy, Sprite> visionCones;

    private AIControllerManager aiManager;
    /** Reference to the goalDoor (for collision detection) */
    private Door goalDoor;
    private int totalGoals;
    private int collectedGoals;
    private TextLayout dreamShardCountText;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    protected ObjectSet<Fixture> shadowSensorFixtures;

    /** Texture for fear meter**/
    private Texture fearMeterTexture;
    /** Texture asset for mouse crosshairs */
    private TextureRegion crosshairTexture;

    private TextureRegion scareEffectTexture;
    private int drawScareLimit;
    private int drawScareCooldown;
    private boolean drawScareEffect = false;

    private Vector2 queuedTeleportPosition = null;


    private CuriosityCritter queuedHarvestedEnemy = null;
    private Teleporter currentTeleporter = null;

    protected PooledList<Surface> shadowPlatformQueue = new PooledList<Surface>();
    private DreamDweller queuedHarvestedEnemyD = null;
    private HashMap<DreamDweller, Sprite> visionCones3;




    /**
     * Returns true if debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @return true if debug mode is active.
     */
    public boolean isDebug( ) {
        return debug;
    }

    /**
     * Sets whether debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @param value whether debug mode is active.
     */
    public void setDebug(boolean value) {
        debug = value;
    }

    /**
     * Returns true if the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @return true if the level is completed.
     */
    public boolean isComplete( ) {
        return complete;
    }

    /**
     * Sets whether the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    public void setComplete(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        complete = value;
    }

    /**
     * Returns true if the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure( ) {
        return failed;
    }

    /**
     * Sets whether the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @param value whether the level is failed.
     */
    public void setFailure(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        failed = value;
    }

    /**
     * Returns true if this is the active screen
     *
     * @return true if this is the active screen
     */
    public boolean isActive( ) {
        return active;
    }

    /**
     * Returns the sprite batch associated with this scene
     *
     * The canvas is shared across all scenes.
     *
     * @return the sprite batch associated with this scene
     */
    public SpriteBatch getSpriteBatch() {
        return batch;
    }

    /**
     * Sets the sprite batch associated with this scene
     *
     * The sprite batch is shared across all scenes.
     *
     * @param batch the sprite batch associated with this scene
     */
    public void setSpriteBatch(SpriteBatch batch) {
        this.batch = batch;
    }

    /**
     * Disposes of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        if (world != null) {
            for(ObstacleSprite sprite : sprites) {
                Obstacle obj = sprite.getObstacle();
                obj.deactivatePhysics(world);
            }
        }
        sprites.clear();
        addQueue.clear();
        world.dispose();
        addQueue = null;
        sprites = null;
        bounds = null;
        scale = null;
        world = null;
        batch = null;
        fearMeterTexture.dispose();
        background.dispose();
    }

    /**
     *
     * Adds a physics sprite in to the insertion queue.
     *
     * Objects on the queue are added just before collision processing. We do
     * this to control object creation.
     *
     * param sprite The sprite to add
     */
    public void addQueuedObject(ObstacleSprite sprite) {
        assert inBounds(sprite) : "Object is not in bounds";
        addQueue.add(sprite);
    }

    /**
     * Immediately adds a physics sprite to the physics world
     *
     * param sprite The sprite to add
     */
    protected void addSprite(ObstacleSprite sprite) {
        assert inBounds(sprite) : "Sprite is not in bounds";
        sprites.add(sprite);
        sprite.getObstacle().activatePhysics(world);
    }

    /**
     * Immediately adds a sprite group to the physics world
     *
     * param group  The sprite group to add
     */
    protected void addSpriteGroup(ObstacleGroup group) {
        for(ObstacleSprite sprite : group.getSprites()) {
            assert inBounds( sprite ) : "Sprite is not in bounds";
            sprites.add( sprite );
        }
        group.activatePhysics(world);
    }

    /**
     * Returns true if the sprite is in bounds.
     *
     * This assertion is useful for debugging the physics.
     *
     * @param sprite    The sprite to check.
     *
     * @return true if the sprite is in bounds.
     */
    public boolean inBounds(ObstacleSprite sprite) {
        Obstacle obj = sprite.getObstacle();
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
        return horiz && vert;
    }


    /**
     * Processes the physics for this frame
     *
     * Once the update phase is over, but before we draw, we are ready to
     * process physics. The primary method is the step() method in world. This
     * implementation works for all applications and should not need to be
     * overwritten.
     *
     * @param dt    Number of seconds since last animation frame
     */
    public void postUpdate(float dt) {
        // Add any objects created by actions
        while (!addQueue.isEmpty()) {
            addSprite(addQueue.poll());
        }

        // Turn the physics engine crank.
        // NORMALLY we would use a fixed step, not dt
        // But that is harder and a topic of the advanced class
        world.step(dt,WORLD_VELOC,WORLD_POSIT);

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<ObstacleSprite>.Entry> iterator = sprites.entryIterator();
        while (iterator.hasNext()) {
            PooledList<ObstacleSprite>.Entry entry = iterator.next();
            ObstacleSprite sprite = entry.getValue();
            Obstacle obj = sprite.getObstacle();
            if (obj.isRemoved()) {
                obj.deactivatePhysics(world);
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);
            }
        }
    }

    /**
     * Draws the physics objects to the screen
     *
     * For simple worlds, this method is enough by itself. It will need to be
     * overriden if the world needs fancy backgrounds or the like.
     *
     * The method draws all objects in the order that they were added.
     *
     * @param dt    Number of seconds since last animation frame
     */
    public void draw(float dt) {
        // Clear the screen (color is homage to the XNA years)
        ScreenUtils.clear(0.39f, 0.58f, 0.93f, 1.0f);

        // This shows off how powerful our new SpriteBatch is
        batch.begin(camera);
        background = directory.getEntry("background-proto", Texture.class);
        batch.draw(background, 0, 0);

        if (drawScareEffect)
        {
            if(drawScareCooldown <= drawScareLimit)
            {
                drawScareEffect();
                drawScareCooldown++;
            } else {
                drawScareCooldown = 0;
                drawScareEffect = false;
            }
        }

        // Draw the meshes (images)
        for(ObstacleSprite obj : sprites) {
            obj.draw(batch);
        }

        drawFearMeter();


        if (debug) {
            // Draw the outlines
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug( batch );
            }
        }
        batch.drawText(dreamShardCountText, 11, height - 50);
        // Draw a final message
        if (complete && !failed) {
            batch.drawText(goodMessage, width/2, height/2);
        } else if (failed) {
            batch.drawText(badMessage, width/2, height/2);
        }

        InputController input = InputController.getInstance();
        float units = height/bounds.height;
        float x = input.getCrossHair().x*units-units/2;
        float y = input.getCrossHair().y*units-units/2;
        batch.draw(crosshairTexture, x, y, units, units);

        //batch.setColor(foregroundColor);
        //batch.draw(foregroundTexture, 0, 0, width, height);

        float u = critter.getObstacle().getPhysicsUnits();

        for (Enemy e : visionCones.keySet()) {
            if (!e.isStunned()) {
                Vector2 headPos = e.getHeadBody().getPosition();
                float headAngleDeg = e.getHeadBody().getAngle() * MathUtils.radiansToDegrees;

                // Get the correct vision cone for the current enemy `e`
                Sprite visionCone = visionCones.get(e);

                visionCone.setPosition(headPos.x * u - visionCone.getOriginX(),
                    headPos.y * u - visionCone.getOriginY());
                visionCone.setRotation(headAngleDeg);
                visionCone.draw(batch);
            }
        }

        batch.end();
    }

    /** Draws Simple fear meter bar
     *
     *
     */
    private void drawFearMeter() {
        int fearLevel = avatar.getFearMeter();
        int maxFear = avatar.getMaxFearMeter();
        float meterWidth = 150 * ((float) fearLevel / maxFear);
        float meterHeight = 20;

        float x = 20; // Offset from the left
        float y = height - meterHeight - 20;

        float outlineThickness = 2;


        batch.setColor(Color.BLACK);
        batch.draw(fearMeterTexture, x - outlineThickness, y - outlineThickness,
            meterWidth + 2 * outlineThickness, meterHeight + 2 * outlineThickness);


        batch.setColor(Color.RED);
        batch.draw(fearMeterTexture, x, y, meterWidth, meterHeight);
        batch.setColor(Color.WHITE);
    }

    private void drawScareEffect(){
        float u = avatar.getObstacle().getPhysicsUnits();
        float size = scareEffectTexture.getRegionWidth() * 0.5f;
        float size2 = scareEffectTexture.getRegionHeight() * 0.5f;


        batch.draw(scareEffectTexture, avatar.getObstacle().getX() * u - size * 0.42f, avatar.getObstacle().getY() * u - size2 *0.57f, size, size2);
    }

    /**
     * Called when the Screen is resized.
     *
     * This can happen at any point during a non-paused state but will never
     * happen before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        this.width  = width;
        this.height = height;
        if (camera == null) {
            camera = new OrthographicCamera();
        }
        camera.setToOrtho( false, width, height );
        scale.x = width/bounds.width;
        scale.y = height/bounds.height;
        reset();
    }

    /**
     * Called when the Screen should render itself.
     *
     * We defer to the other methods update() and draw().  However, it is VERY
     * important that we only quit AFTER a draw.
     *
     * @param delta Number of seconds since last animation frame
     */
    public void render(float delta) {
        if (active) {
            if (preUpdate(delta)) {
                update(delta); // This is the one that must be defined.
                postUpdate(delta);
            }
            draw(delta);
        }
    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
        // TODO Auto-generated method stub
    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        // Useless if called in outside animation loop
        active = true;
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        // Useless if called in outside animation loop
        active = false;
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }
    private String mapkey  = "platform-constants";

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public PlatformScene(AssetDirectory directory,String mapkey) {
        this.directory = directory;
        this.mapkey = mapkey;
        constants = directory.getEntry(mapkey,JsonValue.class);
        JsonValue defaults = constants.get("world");

        crosshairTexture  = new TextureRegion(directory.getEntry( "ragdoll-crosshair", Texture.class ));
        scareEffectTexture = new TextureRegion(directory.getEntry("platform-scare-effect", Texture.class));

        scale = new Vector2();
        bounds = new Rectangle(0,0,defaults.get("bounds").getFloat( 0 ), defaults.get("bounds").getFloat( 1 ));
        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

        displayFont = directory.getEntry( "shared-retro" ,BitmapFont.class);
        goodMessage = new TextLayout();
        goodMessage.setFont( displayFont );
        goodMessage.setAlignment( TextAlign.middleCenter );
        goodMessage.setColor( Color.YELLOW );
        goodMessage.setText("VICTORY!");
        goodMessage.layout();

        badMessage = new TextLayout();
        badMessage.setFont( displayFont );
        badMessage.setAlignment( TextAlign.middleCenter );
        badMessage.setColor( Color.RED );
        badMessage.setText("FAILURE!");
        badMessage.layout();

        complete = false;
        failed = false;
        debug  = false;
        active = false;
        countdown = -1;

        BitmapFont smallFont = new BitmapFont(displayFont.getData(), displayFont.getRegions(), false);
        smallFont.getData().setScale(0.5f);
        dreamShardCountText = new TextLayout();
        dreamShardCountText.setFont(smallFont);
        dreamShardCountText.setAlignment(TextAlign.left);
        dreamShardCountText.setColor(Color.WHITE);
        dreamShardCountText.setText("Dream Shards: " + totalGoals);
        dreamShardCountText.layout();

        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        shadowSensorFixtures = new ObjectSet<Fixture>();

        // Pull out sounds
        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
        volume = constants.getFloat("volume", 1.0f);

        drawScareLimit = 60;
        drawScareCooldown = 0;
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
        totalGoals = goalpos.size;
        collectedGoals = 0;
        /*goalDoor = new Door(units, goal);
        goalDoor.setTexture( texture );
        goalDoor.getObstacle().setName("goal");
        addSprite(goalDoor);
         */
        System.out.println(goalpos);
        System.out.println(goalpos.size);
        for (int i = 0; i < goalpos.size; i++) {
            System.out.println("Fetching Goal Positions.");
            float x = goalpos.get(i).getFloat(0);
            System.out.println("X.");
            float y = goalpos.get(i).getFloat(1);
            System.out.println("Y.");

            Door goalDoor = new Door(units, goal, x, y);
            goalDoor.setTexture(texture);
            goalDoor.getObstacle().setName("goal_" + i);
            addSprite(goalDoor);
        }

        // Create ground pieces


        texture = directory.getEntry( "shared-cloud", Texture.class );
        Texture shadowedTexture = directory.getEntry("shared-shadow-cloud", Texture.class);



        aiManager = new AIControllerManager(avatar, directory);
        Surface wall;
        String wname = "wall";
        JsonValue walls = constants.get("walls");
        JsonValue walljv = walls.get("positions");
        for (int ii = 0; ii < walljv.size; ii++) {
            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls, false);
            wall.getObstacle().setName(wname+ii);
            wall.setTexture( texture );
            addSprite(wall);
        }

        Surface platform;
        String pname = "platform";
        JsonValue plats = constants.get("platforms");
        JsonValue platjv = plats.get("positions");
        JsonValue platShadow = plats.get("shadowed");
        for (int ii = 0; ii < platjv.size; ii++) {
            platform = new Surface(platjv.get(ii).asFloatArray(), units, walls, platShadow.getBoolean(ii) );
            platform.getObstacle().setName(pname+ii);
            if (platform.isShadowed()) {
                platform.setTexture(shadowedTexture);
                shadowPlatformQueue.add(platform);
            } else {
                platform.setTexture(texture);
            }
            addSprite(platform);
        }

        // Create Player
        texture = directory.getEntry( "platform-playerSprite", Texture.class );
        avatar = new Player(units, constants.get("player"));
        avatar.setTexture(texture);
        addSprite(avatar);
        // Have to do after body is created
        avatar.createSensor();

        avatar.createScareSensor();

        aiManager.setPlayer(avatar);



        JsonValue critters = constants.get("curiosity-critter");
        JsonValue critterspos = critters.get("pos");
        visionCones = new HashMap<>();

        for (int i = 0; i < critterspos.size; i++) {
            texture = directory.getEntry( "curiosity-critter-active", Texture.class );
            critter = new CuriosityCritter(units, constants.get("curiosity-critter"), critterspos.get(i).asFloatArray());
            critter.setTexture(texture);
            addSprite(critter);
            // Have to do after body is created
            critter.createSensor();
            critter.createVisionSensor();

            aiManager.register(critter);
            texture = directory.getEntry("vision_cone", Texture.class);
            visionConeRegion = new TextureRegion(texture);
            visionCone = new Sprite(visionConeRegion.getTexture());
            visionCone.setRegion(visionConeRegion);
            visionCone.setSize(240, 200);
            visionCone.setOrigin(visionCone.getWidth() / 2, 0);

            visionCones.put(critter, visionCone);
        }

        JsonValue maintainers = constants.get("mind-maintenance");
        JsonValue maintenancePos = maintainers.get("pos");

        for (int i = 0; i < maintenancePos.size; i++) {
            texture = directory.getEntry( "mind-maintenance-active", Texture.class );

            maintenance = new MindMaintenance(units, constants.get("mind-maintenance"), maintenancePos.get(i).asFloatArray());
            maintenance.setTexture(texture);
            addSprite(maintenance);
            // Have to do after body is created
            maintenance.createSensor();
            maintenance.createVisionSensor();

            aiManager.register(maintenance);
            texture = directory.getEntry("vision_cone", Texture.class);
            visionConeRegion = new TextureRegion(texture);
            visionCone = new Sprite(visionConeRegion.getTexture());
            visionCone.setRegion(visionConeRegion);
            visionCone.setSize(240, 200);
            visionCone.setOrigin(visionCone.getWidth() / 2, 0);

            visionCones.put(maintenance, visionCone);
        }

        JsonValue dreamdwellers = constants.get("dream-dweller");
        JsonValue dwellersPos = dreamdwellers.get("pos");
        for (int i = 0; i < dwellersPos.size; i++) {
            texture = directory.getEntry("dream-dweller-active", Texture.class);
            dreamDweller = new DreamDweller(units, constants.get("dream-dweller"), dwellersPos.get(i).asFloatArray());
            dreamDweller.setTexture(texture);
            addSprite(dreamDweller);
            // Have to do after body is created
            dreamDweller.createSensor();
            dreamDweller.createVisionSensor();

            aiManager.register(dreamDweller);
            texture = directory.getEntry("vision_cone", Texture.class);
            visionConeRegion = new TextureRegion(texture);
            visionCone = new Sprite(visionConeRegion.getTexture());
            visionCone.setRegion(visionConeRegion);
            visionCone.setSize(240, 200);
            visionCone.setOrigin(visionCone.getWidth() / 2, 0);
            visionCone.setColor(new Color(0.8f, 0.2f, 0.8f, 0.5f));

            visionCones.put(dreamDweller, visionCone);
        }



    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode. If not, the update proceeds
     * normally.
     *
     * @param dt    Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {

        InputController input = InputController.getInstance();
        input.sync(bounds, scale);
        if (listener == null) {
            return true;
        }

        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
        }

        // Handle resets
        if (input.didReset()) {
            reset();
        }

        // Now it is time to maybe switch screens.
        if (input.didExit()) {
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        } else if (input.didAdvance()) {
            pause();
            listener.exitScreen(this, EXIT_NEXT);
            return false;
        } else if (input.didRetreat()) {
            pause();
            listener.exitScreen(this, EXIT_PREV);
            return false;
        } else if (countdown > 0) {
            countdown--;
        } else if (countdown == 0) {
            if (failed) {
                reset();
            } else if (complete) {
                pause();
                listener.exitScreen(this, EXIT_NEXT);
                return false;
            }
        }
        if (!isFailure() && (avatar.getObstacle().getY() < -1 || avatar.getFearMeter() == 0)) {
            setFailure(true);
            return false;
        }

        return true;
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
        InputController input = InputController.getInstance();

        avatar.setStunning(input.didStun());
        avatar.setHarvesting(input.didSecondary());
        avatar.setTeleporting(input.didCreateTeleport());

        // Process actions in object model
        avatar.setMovement(input.getHorizontal() *avatar.getForce());

        if (avatar.isGrounded()) avatar.setJumping(input.didPrimary());

        if (avatar.isHarvesting())
        {
            drawScareEffect = true;
            if (queuedHarvestedEnemy != null)
            {
                if (!queuedHarvestedEnemy.getObstacle().isRemoved()) {
                    queuedHarvestedEnemy.getObstacle().markRemoved(true);
                    queuedHarvestedEnemy = null;
                    avatar.setFearMeter(avatar.getFearMeter() + 3);
                }
            } else if (queuedHarvestedEnemyD != null) {
                if (!queuedHarvestedEnemyD.getObstacle().isRemoved()) {
                    queuedHarvestedEnemyD.getObstacle().markRemoved(true);
                    queuedHarvestedEnemyD = null;
                    avatar.setFearMeter(avatar.getFearMeter() + 5);
                }
            }
        }

        if (avatar.isStunning() && avatar.getFearMeter() > STUN_COST) {
            createBullet();
            avatar.setFearMeter(avatar.getFearMeter() - STUN_COST);
        }

        if (avatar.isTeleporting() && avatar.getFearMeter() > CREATE_TELEPORTER_COST && avatar.isInShadow())
        {
            createTeleporter();
        }

        if (input.didTakeTeleport() && currentTeleporter != null && avatar.getFearMeter() > TAKE_TELEPORTER_COST) {
            takeTeleporter(currentTeleporter);
            currentTeleporter = null;
        }


        /*if(avatar.isTakingDamage())
        {
            avatar.setFearMeter(avatar.getFearMeter() - 1);
            avatar.setTakingDamage(false);
        }*/


        if (queuedTeleportPosition != null) {
            avatar.getObstacle().setPosition(queuedTeleportPosition);
            avatar.setFearMeter(Math.max(0,avatar.getFearMeter() - 1));
            queuedTeleportPosition = null; // Clear after applying
        }

//        System.out.println("critter pos" + critter.getObstacle().getPosition());
//        System.out.println("avatar pos" + avatar.getObstacle().getPosition());
        aiManager.update(dt);


        avatar.applyForce();
        if (avatar.isJumping()) {
            /* This jump sound is annoying
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
             */
        }
    }

    private void createTeleporter() {
        float units = height/bounds.height;
        InputController input = InputController.getInstance();
        Vector2 mousePosition = input.getCrossHair();
        float cursorX = mousePosition.x;
        float cursorY = mousePosition.y;

        // Necessary if using Anonymous Function
        final boolean[] isOnSurface = {false};

        // Use QueryAABB with a small box around the mouse point
        world.QueryAABB(new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                Object userData = fixture.getBody().getUserData();
                if (userData instanceof Surface) {
                    if (fixture.testPoint(mousePosition)) {
                        isOnSurface[0] = true;
                        return false; // Stop the query
                    }
                }
                return true; // Continue the query
            }
        }, cursorX - 0.1f, cursorY - 0.1f, cursorX + 0.1f, cursorY + 0.1f);

        if (isOnSurface[0]) {
            System.out.println("Cannot place teleporter: Mouse is directly on a surface!");
            return;
        }


        Vector2 rayStart = new Vector2(cursorX, cursorY);
        Vector2 rayEnd = new Vector2(cursorX, 0);

        // Use the PlatformRayCast class
        PlatformRayCast callback = new PlatformRayCast();

        // Perform the raycast
        world.rayCast(callback, rayStart, rayEnd);

        if (callback.getPlatformFixture() == null) {
            System.out.println("Platform not found");
            return;
        }

        Surface platform = (Surface)callback.getPlatformFixture().getBody().getUserData();
        Vector2 hitPoint = callback.getHitPoint();

        Vector2 teleporterPosition = new Vector2(cursorX, hitPoint.y + 0.75f);

        Vector2 avatarPosition = avatar.getObstacle().getPosition();
        float dist = avatarPosition.dst(teleporterPosition);

        if (dist >= avatar.getTeleportRangeRadius() / units) {
            return;
        }

        Texture texture = directory.getEntry("platform-teleporter", Texture.class);
        JsonValue teleporter = constants.get("teleporter");

        Teleporter originTeleporter = new Teleporter(units, teleporter, avatar.getObstacle().getPosition());
        originTeleporter.setTexture(texture);
        originTeleporter.getObstacle().setName("origin_teleporter");

        Teleporter exitTeleporter = new Teleporter(units, teleporter, teleporterPosition);
        exitTeleporter.setTexture(texture);
        exitTeleporter.getObstacle().setName("exit_teleporter");

        originTeleporter.setLinkedTeleporter(exitTeleporter);
        exitTeleporter.setLinkedTeleporter(originTeleporter);

        addSprite(originTeleporter);
        addSprite(exitTeleporter);

        avatar.setFearMeter(Math.max(0, avatar.getFearMeter() - CREATE_TELEPORTER_COST));
    }

    private void takeTeleporter(Teleporter tp)
    {
        System.out.println(tp.getLinkedTeleporter().getPosition());
        //avatar.getObstacle().setPosition(tp.getLinkedTeleporter().getPosition());
        queuedTeleportPosition = tp.getLinkedTeleporter().getPosition().cpy();

    }


    private void performHarvest(CuriosityCritter enemy)
    {
        queuedHarvestedEnemy = enemy;
    }

    private void performHarvestD(DreamDweller enemy)
    {
        queuedHarvestedEnemyD = enemy;
    }




    /**
     * Adds a new bullet to the world and send it in the right direction.
     */
    private void createBullet() {
        InputController input = InputController.getInstance();

        float units = height/bounds.height;
        Vector2 mousePosition = input.getCrossHair();
        JsonValue bulletjv = constants.get("bullet");
        Obstacle player = avatar.getObstacle();
        Vector2 shootAngle = mousePosition.sub(player.getPosition());
        shootAngle.nor();
        Texture texture = directory.getEntry("platform-bullet", Texture.class);
        Bullet bullet = new Bullet(units, bulletjv, player.getPosition(), shootAngle.nor());
        bullet.setTexture(texture);
        addQueuedObject(bullet);

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("fire", fireSound, volume);
    }

    /**
     * Removes a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(ObstacleSprite bullet) {
        bullet.getObstacle().markRemoved(true);
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }


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
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bodyDataA = fix1.getBody().getUserData();
        Object bodyDataB = fix2.getBody().getUserData();

        try {
            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();
            // Check for win condition
            if ((bd1 == avatar && bd2 instanceof Door) || (bd2 == avatar && bd1 instanceof Door)) {
                Door collectedDoor = (bd1 instanceof Door) ? (Door) bd1 : (Door) bd2;

                if (!collectedDoor.getObstacle().isRemoved()) {
                    collectedDoor.getObstacle().markRemoved(true);
                    collectedGoals++;

                    dreamShardCountText.setText("Dream Shards: " + (totalGoals - collectedGoals));
                    dreamShardCountText.layout();

                    if (collectedGoals == totalGoals) {
                        setComplete(true);
                    }
                }
            }

            // Check if an enemy's walk sensor detects a wall or another enemy
            if (("walk_sensor".equals(fd1) && (bd2 instanceof Surface || bd2 instanceof Enemy)) ||
                ("walk_sensor".equals(fd2) && (bd2 instanceof Surface || bd2 instanceof Enemy))) {
                System.out.println("walk_sensor collision detected with: " + bd1 + " and " + bd2);


                // Ensure the Enemy reference is correctly retrieved
                Enemy e = (bd1 instanceof Enemy) ? (Enemy) bd1
                    : (bd2 instanceof Enemy) ? (Enemy) bd2
                    : null;

                if (e != null) {
                    e.setSeesWall(true);
                    System.out.println("Enemy sees wall");
                } else {
                    System.out.println("WARNING: Walk sensor collision detected but Enemy reference is null.");
                }
            }

            // If there is a collision between a vision sensor and the player
            if ( ("vision_sensor".equals(fd1) || "vision_sensor".equals(fd2))
                && (bodyDataA instanceof Player || bodyDataB instanceof Player) ) {

                // Check if the vision sensor belongs to an "un-stunned" enemy, and if
                // so update the enemy's awareness and apply damage to player
                if ( bodyDataA instanceof Enemy && !((Enemy) bodyDataA).isStunned() ) {
                    ((Enemy) bodyDataA).setAwareOfPlayer(true);
                    System.out.println(bodyDataA.getClass() + " saw player!");
                    avatar.setTakingDamage(true);
                }
                else if ( bodyDataB instanceof Enemy && !((Enemy) bodyDataB).isStunned() )  {
                    System.out.println(bodyDataB.getClass() + " saw player!");
                    ((Enemy) bodyDataB).setAwareOfPlayer(true);
                    avatar.setTakingDamage(true);

                }
                // The player should always take damage when they are detected by a vision sensor
                // not associated with an enemy (e.g. lamp)

                else{
                    avatar.setTakingDamage(true);

                }

            }

            // BULLET COLLISION CASES

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != avatar && !(bd2 instanceof Door)) {
                // if it hits an eenemy
                if (bd2 instanceof Enemy) {
                    // make sure it hits the body of the enemy, and not any sensors
                    if (fd2 != "walk_sensor" && fd2 != "vision_sensor" && fd2 != "follow_sensor"
                        && fd2 != "alert_sensor") {
                        removeBullet(bd1);
                        if (bd2 instanceof CuriosityCritter) {
                            CuriosityCritter critter = (CuriosityCritter) bd2;
                            critter.setStunned(true);
                            critter.setStunTexture(directory);
                            System.out.println("Critter is stunned");
                        } else if (bd2 instanceof MindMaintenance) {
                            MindMaintenance maintenance = (MindMaintenance) bd2;
                            maintenance.setStunned(true);
                            maintenance.setStunTexture(directory);
                            System.out.println("Maintenance is stunned");
                        } else if (bd2 instanceof DreamDweller) {
                            DreamDweller dweller = (DreamDweller) bd2;
                            dweller.setStunned(true);
                            dweller.setStunTexture(directory);
                            System.out.println("Dweller is stunned");
                        } else {
                            System.out.println(
                                "WARNING: Bullet stun collision detected but Enemy reference is null.");
                        }
                    }
                }
                //otherwise the bullet hits a non-enemy and should be removed
                else {
                    removeBullet(bd1);
                }

            }

            // Test bullet collision with world
            if (bd2.getName().equals("bullet") && bd1 != avatar && !(bd1 instanceof Door)) {
                // if it hits an enemy
                if (bd1 instanceof Enemy) {
                    // make sure it hits the body of the enemy, and not any sensors
                    if (fd1 != "walk_sensor" && fd1 != "vision_sensor" && fd1 != "follow_sensor"
                        && fd1 != "alert_sensor") {
                        removeBullet(bd2);
                        if (bd1 instanceof CuriosityCritter) {
                            System.out.println("Critter ");
                            CuriosityCritter critter = (CuriosityCritter) bd1;
                            critter.setStunned(true);
                            critter.setStunTexture(directory);
                            System.out.println("Critter is stunned");
                        } else if (bd1 instanceof MindMaintenance) {
                            MindMaintenance maintenance = (MindMaintenance) bd1;
                            maintenance.setStunned(true);
                            maintenance.setStunTexture(directory);
                            System.out.println("Maintenance is stunned");
                        } else if (bd1 instanceof DreamDweller) {
                            DreamDweller dweller = (DreamDweller) bd1;
                            dweller.setStunned(true);
                            dweller.setStunTexture(directory);
                            System.out.println("Dweller is stunned");
                        } else {
                            System.out.println(
                                "WARNING: Bullet stun collision detected but Enemy reference is null.");
                        }
                    }
                }
                //otherwise the bullet hits a non-enemy and should be removed
                else {
                    removeBullet(bd2);
                }

            }

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && bd1 instanceof Surface) ||
                (avatar.getSensorName().equals(fd1) && bd2 instanceof Surface)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground

                Surface currentSurface;
                if (bd1 instanceof Surface)
                {
                    currentSurface = (Surface) bd1;
                } else {
                    currentSurface = (Surface) bd2;
                }

               if (currentSurface.isShadowed())
               {
                   avatar.setIsShadow(true);
                   shadowSensorFixtures.add(avatar == bd1 ? fix2 : fix1);
               }
            }

            if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof CuriosityCritter)) ||
                (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof CuriosityCritter)))
            {
                CuriosityCritter harvestedCC;
                if (avatar.getScareSensorName().equals(fd1))
                {
                    harvestedCC = (CuriosityCritter) bd2;
                    performHarvest(harvestedCC);
                } else if (avatar.getScareSensorName().equals(fd2))
                {
                    harvestedCC = (CuriosityCritter) bd1;
                    performHarvest(harvestedCC);
                }
                avatar.setHarvesting(true);

            }

            if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof DreamDweller) && fd2 != "dweller_vision_sensor" && fd2 != "dweller_alert_sensor") ||
                (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof DreamDweller) && fd1 != "dweller_vision_sensor" && fd1 != "dweller_alert_sensor"))
            {
               DreamDweller harvested;
                if (avatar.getScareSensorName().equals(fd1))
                {
                    harvested = (DreamDweller) bd2;
                    performHarvestD(harvested);
                } else if (avatar.getScareSensorName().equals(fd2))
                {
                    harvested = (DreamDweller) bd1;
                    performHarvestD(harvested);
                }
                avatar.setHarvesting(true);

            }

            if( !avatar.getScareSensorName().equals(fd1) && bd1 == avatar && bd2.getName().equals("origin_teleporter"))
            {
                if (!(bd2 instanceof Teleporter)) {
                    System.out.println("Error: bd2 is not a Teleporter!");
                } else {
                    currentTeleporter = (Teleporter) bd2;
                }
            } else if (bd1.getName().equals("origin_teleporter") && bd2 == avatar){
                if (!(bd1 instanceof Teleporter)) {
                    System.out.println("Error: bd2 is not a Teleporter!");
                } else {
                    currentTeleporter = (Teleporter) bd1;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch. The main use of
     * this method is to determine when the character is NOT on the ground. This
     * is how we prevent double jumping.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        Object bodyDataA = fix1.getBody().getUserData();
        Object bodyDataB = fix2.getBody().getUserData();

        if (("walk_sensor".equals(fd1) && bd2 instanceof Surface) ||
            ("walk_sensor".equals(fd2) && bd1 instanceof Surface)) {


            CuriosityCritter critter = (bd1 instanceof CuriosityCritter) ? (CuriosityCritter) bd1
                : (bd2 instanceof CuriosityCritter) ? (CuriosityCritter) bd2
                : null;

            if (critter != null) {
                critter.setSeesWall(false);
                System.out.println("Critter stopped seeing wall");
            } else {
                System.out.println("WARNING: Walk sensor end contact detected but Critter reference is null.");
            }
        }


        if (("follow_sensor".equals(fd1) || "follow_sensor".equals(fd2)) && (bodyDataA instanceof Player || bodyDataB instanceof Player)) {
            if (bodyDataA instanceof Enemy) {
                ((Enemy) bodyDataA).setAwareOfPlayer(false);
            }
            else {
                ((Enemy) bodyDataB).setAwareOfPlayer(false);
            }
            avatar.setTakingDamage(false);
            System.out.println("Enemy stopped seeing player");
        }

        if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof CuriosityCritter)) ||
            (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof CuriosityCritter)))
        {
            queuedHarvestedEnemy = null;
            avatar.setHarvesting(false);

        }


        if ((bd1 instanceof Teleporter && bd2 == avatar) || (bd1 == avatar && bd2 instanceof Teleporter)) {
            currentTeleporter = null;
            System.out.println("Player moved away from teleporter");
        }

        if ("dweller_vision_sensor".equals(fd1) || "dweller_vision_sensor".equals(fd2)) {
             bodyDataA = fix1.getBody().getUserData();
             bodyDataB = fix2.getBody().getUserData();

            DreamDweller dweller = null;
            Player playerObj = null;

            if (bodyDataA instanceof DreamDweller && bodyDataB instanceof Player) {
                dweller = (DreamDweller) bodyDataA;
                playerObj = (Player) bodyDataB;
            } else if (bodyDataA instanceof Player && bodyDataB instanceof DreamDweller) {
                dweller = (DreamDweller) bodyDataB;
                playerObj = (Player) bodyDataA;
            }

            if (dweller != null) {

                avatar.setTakingDamage(false);
                System.out.println("Dream Dweller lost sight of player");
            }
        }

        if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof DreamDweller)) ||
            (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof DreamDweller)))
        {
            queuedHarvestedEnemyD = null;
            avatar.setHarvesting(false);

        }




        if ((avatar.getSensorName().equals(fd2) && bd1 instanceof Surface) ||
            (avatar.getSensorName().equals(fd1) && bd2 instanceof Surface)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
            shadowSensorFixtures.remove(avatar == bd1? fix2 : fix1);
            if (shadowSensorFixtures.size == 0){
                avatar.setIsShadow(false);
            }
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop("plop");
        sounds.stop("fire");
        sounds.stop("jump");
    }
}

