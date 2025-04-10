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

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import edu.cornell.cis3152.physics.AIControllerManager;
import edu.cornell.cis3152.physics.ObstacleGroup;

import java.util.ArrayList;
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
import edu.cornell.cis3152.physics.platform.aibehavior.AIManager;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

import edu.cornell.cis3152.physics.InputController;
//import edu.cornell.cis3152.physics.rocket.Box;
//import edu.cornell.cis3152.physics.PhysicsScene;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;

/**
 * The game scene for the platformer game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class PlatformScene implements Screen{
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
    public static final int TELEPORT_COST = 2;

    /** The asset directory for retrieving textures, atlases */
    protected AssetDirectory directory;
    /** The drawing camera for this scene */
    protected OrthographicCamera camera;
    protected OrthographicCamera uiCamera;
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

    /** manages ai control for all entities */
    private AIControllerManager aiCManager;
    private AIManager aiManager;

    private LevelContactListener levelContactListener;
    /** Reference to the goalDoor (for collision detection) */
    private Shard goalShard;
    private int totalShards;
    private int collectedShards;
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

    private Enemy queuedHarvestedEnemy = null;
    private Teleporter currentTeleporter = null;

    protected PooledList<Surface> shadowPlatformQueue = new PooledList<Surface>();
    private DreamDweller queuedHarvestedEnemyD = null;
    private HashMap<DreamDweller, Sprite> visionCones3;
    private HashMap<Teleporter, Float> teleporterCreationTimes = new HashMap<>();
    private float timeElapsed = 0f;

    private ArrayList<Vector2> shardPos;

    /** tiled map + map info */
    private TiledMapInfo tiledMap;




    /*==============================ContactListener Getters/Setters===============================*/

    public Player getAvatar() { return avatar;}

    public void incrementGoal() {
        collectedShards++;}

    public int getTotalShards() {
        return totalShards;
    }

    public boolean checkCollectedAllGoals() {return collectedShards == totalShards;}

    public void setCurrentTeleporter(Teleporter tele) {currentTeleporter = tele; }

    //CHANGE: QUEUE OF ENEMIES ADDED EACH COLLISION. HARVEST SHOULD REMOVE ALL IN QUEUE.
    public void performHarvest(Enemy enemy)
    {
        queuedHarvestedEnemy = enemy;
    }

    public void removeHarvestedEnemy(Enemy enemy) {queuedHarvestedEnemy = null;}

    public void removeHarvestedD(DreamDweller enemy)
    {
        queuedHarvestedEnemyD = null;
    }


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
        addQueue = null;
        sprites = null;
        bounds = null;
        scale = null;
        world = null;
        batch = null;
        fearMeterTexture.dispose();
        tiledMap.disposeMap();
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
        if (sprite instanceof Bullet) {
            Bullet bullet = (Bullet) sprite;
            bullet.setFilter();
        }
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

        background = directory.getEntry("background-technical", Texture.class);
        float parallaxFactor = 0.4f;

        float bgX = camera.position.x * parallaxFactor - background.getWidth() / 2f;
        float bgY = camera.position.y * parallaxFactor - background.getHeight() / 2f;

        batch.draw(background, bgX, bgY, background.getWidth(), background.getHeight() * 2);

        dreamShardCountText.setText("Dream Shards: " + (totalShards - collectedShards));
        dreamShardCountText.layout();

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



        if (debug) {
            // Draw the outlines
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug( batch );
            }
        }

        // Draw a final message


        //batch.setColor(foregroundColor);
        //batch.draw(foregroundTexture, 0, 0, width, height);

        batch.end();
    }

    private void drawUI() {
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        float scaleFactor = 0.2f;
        float originalWidth = crosshairTexture.getRegionWidth();
        float originalHeight = crosshairTexture.getRegionHeight();
        float scaledWidth = originalWidth * scaleFactor;
        float scaledHeight = originalHeight * scaleFactor;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        batch.drawText(dreamShardCountText, 11, height - 50);

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        Vector3 playerWorldPos = new Vector3(avatar.getObstacle().getX() * units,
            avatar.getObstacle().getY() * units,
            0);
        camera.project(playerWorldPos);

        float teleportRadiusScreen = avatar.getTeleportRangeRadius() + 50f; // Because drawTeleportRadius uses it directly with u.

        Vector2 delta = new Vector2(mouseX - playerWorldPos.x, mouseY - playerWorldPos.y);
        if (delta.len() > teleportRadiusScreen) {
            delta.nor().scl(teleportRadiusScreen);
            mouseX = playerWorldPos.x + delta.x;
            mouseY = playerWorldPos.y + delta.y;
        }

        float crossX = mouseX - scaledWidth/2;
        float crossY = mouseY - scaledHeight/2;

        batch.draw(crosshairTexture, crossX, crossY, scaledWidth, scaledHeight);

        if (fearMeterTexture != null && avatar != null) {
            int fearLevel = avatar.getFearMeter();
            int maxFear = avatar.getMaxFearMeter();
            float meterWidth = 150 * ((float) fearLevel / maxFear);
            float meterHeight = 20;
            float meterX = 20;
            float meterY = 20;

            float outlineThickness = 2;
            batch.setColor(Color.RED);
            batch.draw(fearMeterTexture, meterX - outlineThickness, meterY - outlineThickness,
                meterWidth + 2 * outlineThickness, meterHeight + 2 * outlineThickness);

            batch.draw(fearMeterTexture, meterX, meterY, meterWidth, meterHeight);

            batch.setColor(Color.WHITE);
        }

        if (complete && !failed) {
            batch.drawText(goodMessage, width/2, height/2);
        } else if (failed) {
            batch.drawText(badMessage, width/2, height/2);
        }

        batch.end();
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
        scale.x = 1;
        scale.y = 1;
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
            // tiled world


            // camera interpolation
            float units = TiledMapInfo.PIXELS_PER_WORLD_METER;

            // TA feedback was to make it a little less smooth
            // if the camera is far from being directly over the player, we can try to increase the change

            // 2 / (1 + e^(-5(x-0.5)))

            if (debug) {
                Vector3 worldMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(worldMouse);

                // Compute the difference between the mouse and camera center.
                float dx = worldMouse.x - camera.position.x;
                float dy = worldMouse.y - camera.position.y;

                float thresholdX = camera.viewportWidth * 0.25f;
                float thresholdY = camera.viewportHeight * 0.25f;

                float moveX = 0, moveY = 0;
                if (Math.abs(dx) > thresholdX) {
                    moveX = dx - Math.signum(dx) * thresholdX;
                }
                if (Math.abs(dy) > thresholdY) {
                    moveY = dy - Math.signum(dy) * thresholdY;
                }

                // Apply a speed factor to smooth the movement.
                float speed = 0.1f;
                camera.position.x += moveX * speed;
                camera.position.y += moveY * speed;
                clampCamera();
                camera.update();
            } else {

                Vector3 position = this.camera.position;
                Vector3 playerPosition = new Vector3(this.avatar.getObstacle().getX() * units, this.avatar.getObstacle().getY() * units, 0);


                Vector3 diff = new Vector3(position).sub(playerPosition);
                float dis = diff.len();
                if (dis > 220.0) {
                    diff.nor().scl(250);
                    position.lerp(new Vector3((playerPosition).add(diff)), 0.1f);
                }

                float lerp = 3.0f;
                position.x += (this.avatar.getObstacle().getX() * units - position.x) * lerp * delta;
                position.y += (this.avatar.getObstacle().getY() * units - position.y) * lerp * delta;
                camera.position.set(position);
                camera.zoom = 0.7f;
                clampCamera();
                camera.update();
            }



            // box2d
            if (preUpdate(delta)) {
                update(delta);
                postUpdate(delta);
            }
            draw(delta);

            tiledMap.renderDefault(camera);


            drawUI();
        }
    }

    private void clampCamera() {
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        float halfViewportWidth = camera.viewportWidth / 2;
        float halfViewportHeight = camera.viewportHeight / 2;

        // Convert world bounds to screen coordinates
        float minX = bounds.x * units + halfViewportWidth;
        float maxX = (bounds.x + bounds.width) * units - halfViewportWidth;
        float maxY = bounds.y * units + halfViewportHeight;
        float minY = (bounds.y + bounds.height) * units - halfViewportHeight;

        camera.position.x = MathUtils.clamp(camera.position.x, minX-200, maxX+200);
        camera.position.y = MathUtils.clamp(camera.position.y, minY+100, maxY-50);
    }

    /**g
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

        fearMeterTexture = directory.getEntry("fear-meter", Texture.class);

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
        dreamShardCountText.layout();

        this.levelContactListener = new LevelContactListener(this);

        world.setContactListener(levelContactListener);
        sensorFixtures = new ObjectSet<Fixture>();
        shadowSensorFixtures = new ObjectSet<Fixture>();

        // Pull out sounds
        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
        volume = constants.getFloat("volume", 1.0f);

        drawScareLimit = 60;
        drawScareCooldown = 0;

        uiCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();


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
        world.setContactListener(levelContactListener);

        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // can change for testing
        tiledMap = new TiledMapInfo("maps/dreamwalker_alpha_testmap1.tmx");
        aiManager = new AIManager("behaviors/critter.tree", "behaviors/dweller.tree","behaviors/maintenance.tree", directory);
        aiManager.setPlayer(avatar);
        shardPos = new ArrayList<>();

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        System.out.println("units: " + units);

        // Add level goal
        Texture texture = directory.getEntry( "shared-goal", Texture.class );

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");
        totalShards = goalpos.size;
        collectedShards = 0;

        System.out.println(goalpos);
        System.out.println(goalpos.size);
        for (int i = 0; i < goalpos.size; i++) {
            System.out.println("Fetching Goal Positions.");
            float x = goalpos.get(i).getFloat(0);
            System.out.println("X.");
            float y = goalpos.get(i).getFloat(1);
            System.out.println("Y.");

            Shard goalShard = new Shard(units, goal, x, y, i);
            shardPos.add(i, new Vector2(x, y));

            goalShard.setTexture(texture);
            goalShard.getObstacle().setName("goal_" + i);
            addSprite(goalShard);
            goalShard.setFilter();
        }

        MapLayer collisionLayer = tiledMap.get().getLayers().get("CollisionLayer");
        int id = 0;
        for (MapObject o : collisionLayer.getObjects()) {
            if (o instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) o).getRectangle();
                // want to create "surfaces" where rects are in our tiled layer

                float x = o.getProperties().get("x", Float.class);
                float y = o.getProperties().get("y", Float.class);
                float height = o.getProperties().get("height", Float.class);
                float width = o.getProperties().get("width", Float.class);

                float worldX = x / units;
                float worldWidth = width/units;

                // tiles * pixel per tile
                float mapPixelHeight = tiledMap.get().getProperties().get("height", Integer.class) * TiledMapInfo.PIXELS_PER_WORLD_METER;
                float worldY = y / units;
                float worldHeight = height / units;

                Surface platform = new Surface(worldX, worldY, worldHeight, worldWidth, TiledMapInfo.PIXELS_PER_WORLD_METER, constants.get("platforms"), true);

                platform.setDebugColor(Color.BLUE);


                if (o.getProperties().get("isStair", Boolean.class)) {
                    platform.getObstacle().setName("stair " + id);
                } else {
                    platform.getObstacle().setName("platform " + id);
                }
                System.out.println(platform.getObstacle().getName());
                addSprite(platform);
                platform.setFilter();
                System.out.println("platform added!");
                id++;
            }
        }


        texture = directory.getEntry( "shared-test", Texture.class );
        Texture shadowedTexture = directory.getEntry("shared-shadow-test", Texture.class);



//        Surface wall;
//        String wname = "wall";
//        JsonValue walls = constants.get("walls");
//        JsonValue walljv = walls.get("positions");
//        for (int ii = 0; ii < walljv.size; ii++) {
//            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls, false);
//            wall.getObstacle().setName(wname+ii);
//            wall.setTexture( texture );
//            addSprite(wall);
//        }
//
//        Surface platform;
//        String pname = "platform";
//        JsonValue plats = constants.get("platforms");
//        JsonValue platjv = plats.get("positions");
//        JsonValue platShadow = plats.get("shadowed");
//        for (int ii = 0; ii < platjv.size; ii++) {
//            platform = new Surface(platjv.get(ii).asFloatArray(), units, walls, platShadow.getBoolean(ii) );
//            platform.getObstacle().setName(pname+ii);
//            if (platform.isShadowed()) {
//                platform.setTexture(shadowedTexture);
//                shadowPlatformQueue.add(platform);
//            } else {
//                platform.setTexture(texture);
//            }
//            addSprite(platform);
//        }

        // Create Player
        texture = directory.getEntry( "player-walk", Texture.class );
        avatar = new Player(units, constants.get("player"));
        Texture idle = directory.getEntry("player-idle", Texture.class);
        Texture jump = directory.getEntry("player-jump", Texture.class);

        addSprite(avatar);

        avatar.createAnimators(texture, idle, jump);
        // Have to do after body is created
        avatar.setFilter();

        avatar.createSensor();

        avatar.createScareSensor();

        aiManager.setPlayer(avatar);



        JsonValue critters = constants.get("curiosity-critter");
        JsonValue critterspos = critters.get("pos");

        for (int i = 0; i < critterspos.size; i++) {
            texture = directory.getEntry( "curiosity-critter-active", Texture.class );
            critter = new CuriosityCritter(units, constants.get("curiosity-critter"), critterspos.get(i).asFloatArray(), this);
            critter.setTexture(texture);
            addSprite(critter);

            // Have to do after body is created
            critter.setFilter();
            critter.createSensor();
            critter.createVisionSensor();

            aiManager.register(critter);
            texture = directory.getEntry("vision_cone", Texture.class);
            visionConeRegion = new TextureRegion(texture);
            visionCone = new Sprite(visionConeRegion.getTexture());
            visionCone.setRegion(visionConeRegion);
            visionCone.setSize(240, 200);
            visionCone.setOrigin(visionCone.getWidth() / 2, 0);


        }

        JsonValue maintainers = constants.get("mind-maintenance");
        JsonValue maintenancePos = maintainers.get("pos");

        for (int i = 0; i < maintenancePos.size; i++) {
            texture = directory.getEntry("mind-maintenance-active", Texture.class);

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

        }

        if (dreamShardCountText != null) {
            dreamShardCountText.setText("Dream Shards: " + (totalShards - collectedShards));
            dreamShardCountText.layout();
        }

    }

    public Vector2 getShardPos(int index) {
        return shardPos.get(index);
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
        avatar.setTeleporting(input.didM1());

        // Process actions in object model
        avatar.setMovement(input.getHorizontal() *avatar.getForce());

//        if (avatar.isGrounded()) avatar.setJumping(input.didPrimary());

        avatar.setJumping(input.didPrimary());
        avatar.setStunning(input.didStun());
        avatar.setHarvesting(input.didSecondary());
        avatar.setTeleporting(input.didM1());

        if (avatar.isHarvesting())
        {
            drawScareEffect = true;
            if (queuedHarvestedEnemy != null)
            {
                if (!queuedHarvestedEnemy.getObstacle().isRemoved()) {
                    queuedHarvestedEnemy.getObstacle().markRemoved(true);
                    queuedHarvestedEnemy = null;
                    avatar.setFearMeter(avatar.getFearMeter() + 3);
                    aiManager.unregister(queuedHarvestedEnemy);
                }
            } else if (queuedHarvestedEnemyD != null) {
                if (!queuedHarvestedEnemyD.getObstacle().isRemoved()) {
                    queuedHarvestedEnemyD.getObstacle().markRemoved(true);
                    queuedHarvestedEnemyD = null;
                    avatar.setFearMeter(avatar.getFearMeter() + 5);
                    aiManager.unregister(queuedHarvestedEnemyD);
                }
            }
            System.out.println(queuedHarvestedEnemy + " being harvested");
        }

        if (avatar.isStunning() && avatar.getFearMeter() > STUN_COST) {
            createBullet();
            avatar.setFearMeter(avatar.getFearMeter() - STUN_COST);
        }

        for (Teleporter tp : new ArrayList<>(teleporterCreationTimes.keySet())) {
            float creationTime = teleporterCreationTimes.get(tp);
            if(timeElapsed - creationTime >= 2.0f) {
                tp.getObstacle().markRemoved(true);
                teleporterCreationTimes.remove(tp);
            }
        }

        if (avatar.isTeleporting() && avatar.getFearMeter() > TELEPORT_COST)
        {
            teleport();
        }


        if(avatar.isTakingDamage())
        {
            avatar.setFearMeter(avatar.getFearMeter() - 1);
            avatar.setTakingDamage(false);
        }


        if (queuedTeleportPosition != null) {
            avatar.getObstacle().setPosition(queuedTeleportPosition);
            //avatar.setFearMeter(Math.max(0,avatar.getFearMeter() - TELEPORT_COST));
            queuedTeleportPosition = null; // Clear after applying
        }

        aiManager.update(dt);
        GdxAI.getTimepiece().update(dt);

        avatar.applyForce(world);
        if (avatar.isJumping()) {
            /* This jump sound is annoying
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
             */
        }
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    private void teleport() {
        InputController input = InputController.getInstance();
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;

        Vector2 playerPosition = avatar.getObstacle().getPosition();

        // Get crosshair position in screen coordinates
        Vector2 crosshairScreen = input.getMouse();

        // Unproject the crosshair screen position to get world coordinates
        Vector3 crosshairTemp = new Vector3(
            crosshairScreen.x,
            crosshairScreen.y,
            0
        );
        camera.unproject(crosshairTemp);
        Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);

        // clamp
        Vector2 delta = new Vector2(crosshairWorld).sub(playerPosition);
        // Convert teleport range from (screen or arbitrary) units to world units.
        float teleportRangeWorld = avatar.getTeleportRangeRadius() / units;
        if (delta.len() > teleportRangeWorld) {
            delta.nor().scl(teleportRangeWorld);
            crosshairWorld = new Vector2(playerPosition).add(delta);
        }

        Vector2 realTeleportPos = crosshairWorld.cpy();


        final boolean[] isInsideSurface = {false};
        // Check if trying to teleport into a surface
        world.QueryAABB(new QueryCallback() {
                            @Override
                            public boolean reportFixture(Fixture fixture) {
                                Object userData = fixture.getBody().getUserData();
                                if (userData instanceof Surface) {
                                    if (fixture.testPoint(realTeleportPos)) {
                                        isInsideSurface[0] = true;
                                        return false; // Stop the query
                                    }
                                }
                                return true; // Continue the query
                            }
                        }, crosshairWorld.x - 0.1f, crosshairWorld.y - 0.1f,
            crosshairWorld.x + 0.1f, crosshairWorld.y + 0.1f);

        if (isInsideSurface[0]) {
            System.out.println("Cannot place teleport in a surface");
            return;
        }

        Vector2 rayStart = new Vector2(playerPosition.x, playerPosition.y);
        Vector2 rayEnd = new Vector2(crosshairWorld.x, 0);
        PlatformRayCast callback = new PlatformRayCast();
        world.rayCast(callback, rayStart, rayEnd);

        if (callback.getPlatformFixture() == null) {
            System.out.println("Player is not shadowed, cannot teleport");
            return;
        }

        // Use raycast to find platform below cursor
        rayStart = new Vector2(crosshairWorld.x, crosshairWorld.y);
        rayEnd = new Vector2(crosshairWorld.x, 0);
        world.rayCast(callback, rayStart, rayEnd);

        if (callback.getPlatformFixture() == null) {
            System.out.println("Teleport location is not shadowed, cannot teleport");
            return;
        }

        System.out.println("Hitpoint Count: " + callback.getHitPointCount());
        if (callback.getHitPointCount()%2 == 1){
            System.out.println("Cannot teleport inside of surface");
            return;
        }
        Vector2 hitPoint = callback.getHitPoint();
        Vector2 initialPosition = new Vector2(crosshairWorld.x, crosshairWorld.y + 0.75f);
        Vector2 teleporterPosition = new Vector2(crosshairWorld.x, crosshairWorld.y + 0.75f);

        // Calculate distance in world coordinates
        float worldDistance = playerPosition.dst(teleporterPosition);
        float maxTeleportRange = avatar.getTeleportRangeRadius() / units;

        // Check if destination is outside range
        if (worldDistance > maxTeleportRange) {
            Vector2 direction = new Vector2(
                initialPosition.x - playerPosition.x,
                initialPosition.y - playerPosition.y
            ).nor();

            // Clamp position to max distance
            teleporterPosition = new Vector2(
                playerPosition.x + direction.x * maxTeleportRange,
                playerPosition.y + direction.y * maxTeleportRange
            );
        }

        // Raycast to check if the clamped position is above a surface
        Vector2 rayStartForClamped = new Vector2(teleporterPosition.x, teleporterPosition.y);
        Vector2 rayEndForClamped = new Vector2(teleporterPosition.x, 0);
        PlatformRayCast clampedCallback = new PlatformRayCast();
        world.rayCast(clampedCallback, rayStartForClamped, rayEndForClamped);

        if (clampedCallback.getPlatformFixture() != null) {
            Vector2 clampedHitPoint = clampedCallback.getHitPoint();
            // Ensure the teleporter position is above the surface
            if (teleporterPosition.y <= clampedHitPoint.y) {
                teleporterPosition.y = clampedHitPoint.y + 0.75f; // Adjust to be above the surface
            }
        } else {
            System.out.println("No platform found at clamped position");
            return;
        }

        queuedTeleportPosition = teleporterPosition;
    }

    /**
     * Adds a new bullet to the world and send it in the right direction.
     */
    private void createBullet() {
        InputController input = InputController.getInstance();

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        Vector2 crosshairScreen = input.getMouse();

        // Unproject the crosshair screen position to get world coordinates
        Vector3 crosshairTemp = new Vector3(
            crosshairScreen.x,
            crosshairScreen.y,
            0
        );
        camera.unproject(crosshairTemp);
        Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);

        JsonValue bulletjv = constants.get("bullet");
        Obstacle player = avatar.getObstacle();
        Vector2 shootAngle = crosshairWorld.sub(player.getPosition());
        shootAngle.nor();
        Texture texture = directory.getEntry("platform-bullet", Texture.class);
        Bullet bullet = new Bullet(units, bulletjv, player.getPosition(), shootAngle.nor());
        bullet.setTexture(texture);
        addQueuedObject(bullet);

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        //sounds.play("fire", fireSound, volume);
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

