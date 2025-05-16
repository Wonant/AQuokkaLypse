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
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import edu.cornell.cis3152.physics.AIControllerManager;
import edu.cornell.cis3152.physics.ObstacleGroup;

import java.util.*;

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
public class PlatformScene implements Screen, Telegraph {
    // SOME EXIT CODES FOR GDXROOT
    /** Exit code for quitting the game */
    public static final int EXIT_PAUSE = 0;
    /** Exit code for advancing to next level */
    public static final int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** Exit code for level select */
    public static final int EXIT_LEVELSELECT = 3;
    public static final int FROM_LEVELSELECT = 4;

    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 120;

    public static final int STUN_COST = 1;
    public static final int TELEPORT_COST = 2;

    /** The asset directory for retrieving textures, atlases */
    protected AssetDirectory directory;
    /** The drawing camera for this scene */
    protected OrthographicCamera camera;
    protected OrthographicCamera uiCamera;
    protected OrthographicCamera miniCam;
    private Vector3 defaultMiniCamPos;
    private Matrix4       mainProj;
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

    /** Used to slightly modify the PlatformScene for Level Select usage */
    private boolean isLevelSelect;


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
    private Sprite visionCone;

    /** manages ai control for all entities */
    private AIControllerManager aiCManager;
    private AIManager aiManager;

    private PooledList<Enemy> enemies = new PooledList<>();
    private LevelContactListener levelContactListener;


    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    protected ObjectSet<Fixture> shadowSensorFixtures;

    /** Texture for fear meter and visibility indicators **/
    private Texture fearMeterTexture;
    private Texture visibilityIndicatorTexture;

    /** Texture asset for mouse crosshairs */
    private TextureRegion crosshairTexture;
    private TextureRegion teleportCrosshairTexture;
    private Animator fearMeterSprite;

    private TextureRegion scareEffectTexture;
    private int drawScareLimit;
    private int drawScareCooldown;
    private boolean drawScareEffect = false;

    private Vector2 queuedTeleportPosition = null;
    private PooledList<Enemy> queuedHarvestedEnemy = new PooledList<Enemy>();

    protected PooledList<Surface> shadowPlatformQueue = new PooledList<Surface>();
    private HashMap<DreamDweller, Sprite> visionCones3;
    private float timeElapsed = 0f;

    /** Spawn positions of all shards */
    private ArrayList<Vector2> shardPos;

    /** Possible positions shards can be placed by enemies */
    private HashMap<Integer, Vector2> possibleShardPos;

    /** hashset of ids to keep track of shards that might be currently moving*/
    private Set<Integer> removedShards = new HashSet<>();

    private static class PendingShard {
        public final Vector2 location;
        public final Shard shard;
        public PendingShard(Vector2 loc, Shard s) { location = loc; shard = s; }
    }
    private final List<PendingShard> pendingShardSpawns = new ArrayList<>();

    // Reference to the shards
    private int totalShards;
    private int collectedShards;
    private TextLayout dreamShardCountText;
    private Map<Shard,Float> shardPickupTimers = new HashMap<>();
    public Shard currentInteractingShard = null;
    private float     vortexTimer        = 0f;

    private Animator swirlSprite;

    /** if shadow mode is on, which changes player m1 to a teleport rather than an attack */
    private boolean shadowMode;
    private float lastLShiftTime;

    /** How many enemies are aware of player in level (akin to GTA star system) */
    private int enemiesAlerted;

    /** How many critters are aware of the player (to manage slow factor) */
    private int crittersAlerted;

    /** tiled map + map info */
    private TiledMapInfo tiledMap;
    private TiledMapRenderer tiledRenderer;
    private String tiledLevelName;

    // minimap
    private MinimapRenderer minimapRenderer;
    private boolean miniMapActive;
    private boolean miniMapLastActive;
    private float miniMapTime;

    /** projectile pooled lists */
    protected PooledList<Door> doors = new PooledList<Door>();

    protected PooledList<ShieldWall> shieldWalls = new PooledList<ShieldWall>();
    protected PooledList<Spear> spears = new PooledList<Spear>();

    /** Telegraphing/event communication */
    private final MessageDispatcher dispatcher = new MessageDispatcher();

    private float timeSinceStart = 0f;
    /** last time a “see” event arrived (in seconds) */
    private float lastCritterSawTime = -1f;
    /** have we already slowed the player? */
    private boolean playerSlowed = false;
    /** how slow they go (half speed here) */
    private float slowSpeedFactor = 0.4f;


    // global game units
    float units;
    private String mapkey;

    private Animator teleportAnimator;
    private boolean isTeleporting = false;
    private final float TELEPORT_SURFACE_BUFFER = 0.5f;

    private float teleportAnimationTime = 0f;
    private Vector2 preTeleportPosition;
    private Vector2 teleportPosition;
    private float teleportAngle;
    private boolean teleportDirectionRight;

    // Shaders
    private Shader vortexShader, tendrilShader, rippleShader;
    private Shader rayShader, blurShader;

    private FrameBuffer sceneBuffer;
    private Texture blankTexture, screenTexture;
    private Sprite blankSprite;
    private Texture abstractGlassyTexture;


    private VertexBuffer bulletVB;
    private Shader glowShader;

    private float globalTime;

    /** Used to ensure scene doesn't reset when from pause scene*/
    private boolean resumingFromPause = false;

    private Array<Spear> pendingSpears = new Array<>();
    private float spearTimer = 0f;
    private int spearIndex = 0;
    private static final float SPEAR_FIRE_INTERVAL = 0.1f;

    // FADE CONSTANTS
    private float fadeAlpha = 0f;
    private float fadeSpeed = 0.01f;
    private boolean isFading = false;
    private Color fadeColor = new Color(0, 0, 0, 0);

    int nextIndex = 0;

    /*==============================ContactListener Getters/Setters===============================*/

    public Player getAvatar() { return avatar;}

    public void incrementGoal() {
        collectedShards++;}

    public int getTotalShards() {
        return totalShards;
    }

    public void queueShardSpawn(Vector2 world, Shard dropped) {
        pendingShardSpawns.add(new PendingShard(world.cpy(), dropped));
    }

    public boolean checkCollectedAllGoals() {return collectedShards == totalShards;}

    //CHANGE: QUEUE OF ENEMIES ADDED EACH COLLISION. HARVEST SHOULD REMOVE ALL IN QUEUE.
    public void performHarvest(Enemy enemy)
    {
        queuedHarvestedEnemy.add(enemy);
    }


    public void removeHarvestedEnemy(Enemy enemy) {
        queuedHarvestedEnemy.remove(enemy);
    }

    //public void removeHarvestedEnemy(Enemy enemy) {queuedHarvestedEnemy.remove(enemy);
    //    enemies.remove(enemy);      }




    // SHARDS //

    public HashMap<Integer, Vector2> getPossibleShardSpots() {
        return possibleShardPos;
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
            isFading = true;
            fadeAlpha = 0f;
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
            isFading = true;
            fadeAlpha = 0f;
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

    public boolean isShardRemoved(int id) {
        return removedShards.contains(id);
    }

    public void markShardRemoved(int id) {
        removedShards.add(id);
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
    /*============================================================================================*/

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
        visibilityIndicatorTexture.dispose();
        abstractGlassyTexture.dispose();
        if (bulletVB != null) {
            bulletVB.dispose();
            bulletVB = null;
        }
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

    public int getDoorDestination(){
        return nextIndex;
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

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public PlatformScene(AssetDirectory directory, String mapkey, String tiled, Boolean isLevelSelect) {
        this.isLevelSelect = isLevelSelect;
        this.directory = directory;
        this.mapkey = mapkey;
        tiledLevelName = tiled;
        constants = directory.getEntry(mapkey,JsonValue.class);
        JsonValue defaults = constants.get("world");

        crosshairTexture  = new TextureRegion(directory.getEntry( "crosshair", Texture.class ));
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
        visibilityIndicatorTexture = directory.getEntry("visibility-indicator", Texture.class);

        complete = false;
        failed = false;
        debug  = false;
        active = false;
        countdown = -1;

        BitmapFont smallFont = new BitmapFont(displayFont.getData(), displayFont.getRegions(), false);
        smallFont.getData().setScale(0.2f);
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

        drawScareLimit = avatar.getHarvestDuration();
        drawScareCooldown = 0;


        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        uiCamera = new OrthographicCamera(sw, sh);
        uiCamera.setToOrtho(false, sw, sh);
        uiCamera.update();


        miniCam = new OrthographicCamera(sw/2, sh/2);
        miniCam.setToOrtho(false, sw, sh);
        miniCam.zoom = 3f;
        defaultMiniCamPos = miniCam.position.cpy();



        // is this not supposed to be removed?
        units = height / bounds.height;


        Texture fear = directory.getEntry("fear-meter-sprite-sheet", Texture.class);
        Texture swirl = directory.getEntry("swirl-active", Texture.class);
        createAnimators(fear,swirl);

        dispatcher.addListener(this, MessageType.CRITTER_SEES_PLAYER);
        dispatcher.addListener(this, MessageType.CRITTER_LOST_PLAYER);

        // SHADERS


        vortexShader = new Shader(Gdx.files.internal("shaders/vortex.vert"),Gdx.files.internal("shaders/vortex.frag"));
        if (!vortexShader.isCompiled()) {
            Gdx.app.error("PlatformScene", "Swirl shader compile error: " + vortexShader.getLog());
        }

        tendrilShader = new Shader(Gdx.files.internal("shaders/tendrils.vert"),Gdx.files.internal("shaders/tendrils.frag"));
        rippleShader = new Shader(Gdx.files.internal("shaders/ripple.vert"),Gdx.files.internal("shaders/ripple.frag"));
        rayShader = new Shader(Gdx.files.internal("shaders/screen.vert"), Gdx.files.internal("shaders/fallingrays.frag"));
        glowShader = new Shader(Gdx.files.internal("shaders/star_pulse.vert"), Gdx.files.internal("shaders/star_pulse.frag"));
        blurShader = new Shader(Gdx.files.internal("shaders/screen_blur.vert"), Gdx.files.internal("shaders/screen_blur.frag"));

        if (!glowShader.isCompiled()) {
            System.out.println("Swirl shader compile error: " + vortexShader.getLog());
        }

        float[] verts = {
            // x,    y,    u,   v
            -0.5f, -0.5f, 0f, 0f,
            0.5f, -0.5f, 1f, 0f,
            0.5f,  0.5f, 1f, 1f,
            -0.5f,  0.5f, 0f, 1f
        };
        short[] idx = { 0,1,2,  2,3,0 };

        int stride = 4 * 4; //4 bytes by 4 bytes

        bulletVB = new VertexBuffer(stride, 4, 6);
        bulletVB.setupAttribute(ShaderProgram.POSITION_ATTRIBUTE, 2, GL30.GL_FLOAT, false, 0);
        bulletVB.setupAttribute(ShaderProgram.TEXCOORD_ATTRIBUTE+"0", 2, GL30.GL_FLOAT, false, 8);
        bulletVB.enableAttribute(ShaderProgram.POSITION_ATTRIBUTE);
        bulletVB.enableAttribute(ShaderProgram.TEXCOORD_ATTRIBUTE+"0");

        bulletVB.bind();
        bulletVB.loadVertexData(verts, 4, GL30.GL_STATIC_DRAW);
        bulletVB.loadIndexData(idx,    6, GL30.GL_STATIC_DRAW);
        bulletVB.unbind();

        sceneBuffer = new FrameBuffer(
            Pixmap.Format.RGBA8888,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            false
        );

        Pixmap pix = new Pixmap(1,1,Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        blankTexture = new Texture(pix);
        pix.dispose();
        blankSprite   = new Sprite(blankTexture);


        abstractGlassyTexture = directory.getEntry("abstract-glassy", Texture.class);
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
     * creates animator objects of sprite sheets
     */
    private void createAnimators(Texture fearMeter, Texture swirl) {
        fearMeterSprite = new Animator(fearMeter, 2, 6, 0.033f, 11, 0, 10);
        swirlSprite = new Animator(swirl, 1, 6, 0.10f, 6, 0, 5);
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        globalTime = 0;
        tiledMap = new TiledMapInfo(tiledLevelName);
        tiledRenderer = new TiledMapRenderer(tiledMap.map, batch, 32);
        aiCManager = new AIControllerManager(avatar,directory,world);
        aiManager = new AIManager("behaviors/critter.tree", "behaviors/dweller.tree","behaviors/maintenance.tree", directory);
        aiManager.setPlayer(avatar);
        shardPos = new ArrayList<>();
        possibleShardPos = new HashMap<>();
        enemiesAlerted = 0;
        crittersAlerted = 0;
        playerSlowed = false;
        lastCritterSawTime = -1f;
        timeSinceStart = 0;
        miniMapActive = false;
        miniMapLastActive = false;

        //minimap = new Minimap(this);

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        minimapRenderer = new MinimapRenderer(tiledMap.map, batch, units, bounds.width, bounds.height);
        int level = 0;


        // entity spawn handling from tiled

        // json constants for physics
        JsonValue critters = constants.get("curiosity-critter");

        Vector2 playerSpawnPos = new Vector2();

        MapLayer spawnLayer = tiledMap.get().getLayers().get("Spawn");
        for (MapObject o : spawnLayer.getObjects()) {
            if (o instanceof RectangleMapObject) {
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

                if (o.getName().startsWith("door")) {
                    try {
                        int destination = o.getProperties().get("level", Integer.class);
                        Door door = new Door(units, worldX, worldY, worldWidth, worldHeight,
                            destination);
                        doors.add(door);
                        addSprite(door);
                        door.setFilter();
                    }
                    catch(Exception e){
                        Door door = new Door(units, worldX, worldY, worldWidth, worldHeight,
                            level + 1);
                        doors.add(door);
                        addSprite(door);
                        door.setFilter();
                    }
                }
                if (o.getName().startsWith("Player")) {
                    playerSpawnPos.set(worldX, worldY);
                }
                if (o.getName().startsWith("curiosity critter")) {
                    Texture texture = directory.getEntry( "curiosity-critter-active", Texture.class );
                    critter = new CuriosityCritter(units, constants.get("curiosity-critter"), new float[]{worldX,worldY}, this, dispatcher);
                    critter.setTexture(texture);
                    addSprite(critter);

                    // Have to do after body is created
                    critter.setFilter();
                    critter.createSensor();
                    enemies.add(critter);
                    aiManager.register(critter);
                }
                if (o.getName().startsWith("dream dweller")) {
                    Texture texture = directory.getEntry("dream-dweller-active", Texture.class);

                    dreamDweller = new DreamDweller(units, constants.get("dream-dweller"), new float[]{worldX, worldY}, this);
                    dreamDweller.setTexture(texture);
                    addSprite(dreamDweller);
                    // Have to do after body is created
                    dreamDweller.setFilter();
                    dreamDweller.createSensor();
                    enemies.add(dreamDweller);
                    //aiManager.register(maintenance);
                    aiCManager.register(dreamDweller);
                }
                if (o.getName().startsWith("mind maintenance")) {
                    Texture texture = directory.getEntry("mind-maintenance-active", Texture.class);

                    maintenance = new MindMaintenance(units, constants.get("mind-maintenance"), new float[]{worldX, worldY}, this, dispatcher);
                    maintenance.setTexture(texture);
                    addSprite(maintenance);

                    texture = directory.getEntry( "maintenance-sprite-sheet", Texture.class);
                    // Have to do after body is created
                    maintenance.createAnimators(texture);
                    maintenance.setFilter();
                    maintenance.createSensor();
                    maintenance.createVisionSensor();
                    enemies.add(maintenance);
                    //aiManager.register(maintenance);
                    aiCManager.register(maintenance);
                }
            }
        }


        // dream shard creation from tiled layer
        Texture texture = directory.getEntry( "shard", Texture.class );
        MapLayer shardLayer = tiledMap.get().getLayers().get("Shards");
        JsonValue goal = constants.get("goal");

        totalShards = shardLayer.getProperties().get("totalShards", Integer.class);
        collectedShards = 0;

        int shardID = 0;
        for (MapObject o : shardLayer.getObjects()) {
            if (o instanceof RectangleMapObject) {
                float x = o.getProperties().get("x", Float.class);
                float y = o.getProperties().get("y", Float.class);
                float worldX = x / units;
                float worldY = y / units;

                MapObject reposition = o.getProperties().get("path", MapObject.class);

                if (reposition != null) {
                    possibleShardPos.put(shardID, new Vector2(reposition.getProperties().get("x", Float.class) / 32f, reposition.getProperties().get("y", Float.class) / 32f));
                }
                if (o.getName() == null) {
                    Shard goalShard = new Shard(units, goal, worldX, worldY, shardID);
                    shardPos.add(shardID, new Vector2(worldX, worldY));
                    goalShard.setTexture(texture);
                    goalShard.getObstacle().setName("goal_" + shardID);
                    addSprite(goalShard);
                    goalShard.setFilter();
                    shardID++;
                }



            }
        }

        MapLayer collisionLayer = tiledMap.get().getLayers().get("CollisionLayer");
        int id = 0;
        for (MapObject o : collisionLayer.getObjects()) {
            if (o instanceof PolygonMapObject) {
                PolygonMapObject polyObj = (PolygonMapObject) o;
                Polygon poly = polyObj.getPolygon();
                float[] localVtx = poly.getVertices();
                for (int i = 0; i < localVtx.length; i++) {
                    if (i % 2 == 0) {
                        System.out.print("x: " + localVtx[i] + " ");
                    } else {
                        System.out.println("y: " + localVtx[i]);
                    }
                }
                // The object’s own position in pixels
                float offsetX = poly.getX();
                float offsetY = poly.getY();

                Surface surface = new Surface(
                    localVtx,
                    offsetX,
                    offsetY,
                    TiledMapInfo.PIXELS_PER_WORLD_METER,
                    constants.get("platforms"),
                    true
                );

                surface.setDebugColor(Color.BLUE);
                Boolean isStair = o.getProperties().get("isStair", Boolean.class);
                if (isStair != null) {
                    surface.getObstacle().setName("stair " + id);
                } else {
                    surface.getObstacle().setName("platform " + id);
                }
                addSprite(surface);
                surface.setFilter();

                id++;
            }
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
                float rotationDeg = o.getProperties().get("rotation", 0f, Float.class);
                float rotationRad = (float)Math.toRadians(rotationDeg);
                if (o.getName() != null) {
                    if (o.getName().toLowerCase().startsWith("spike")) {
                        if (o.getProperties().get("direction", Integer.class) != null && o.getProperties().get("laser", Boolean.class) != null) {
                            DayglowSpike.Direction dir;
                            switch (o.getProperties().get("direction", Integer.class)) {
                                case 0:  dir = DayglowSpike.Direction.UP;    break;
                                case 1:  dir = DayglowSpike.Direction.RIGHT; break;
                                case 2:  dir = DayglowSpike.Direction.DOWN;  break;
                                case 3:  dir = DayglowSpike.Direction.LEFT;  break;
                                default: dir = DayglowSpike.Direction.UP;    break;
                            }
                            DayglowSpike spike = new DayglowSpike(units, worldX, worldY, worldWidth, worldHeight,
                                o.getProperties().get("laser", Boolean.class), dir);
                            spike.setTexture(directory.getEntry("dayglow-spike", Texture.class));
                            addSprite(spike);
                            spike.setFilter();
                            spike.setWorld(world);
                        }
                    }
                } else {
                    Surface platform = new Surface(worldX, worldY, worldHeight, worldWidth, TiledMapInfo.PIXELS_PER_WORLD_METER, constants.get("platforms"), true, rotationRad);

                    platform.setDebugColor(Color.BLUE);
                    platform.getObstacle().setName("platform " + id);
                    addSprite(platform);
                    platform.setFilter();
                    id++;
                }
            }
        }

        avatar = new Player(units, constants.get("player"), playerSpawnPos, this);

        Texture dreamwalker = directory.getEntry("player-sprite-sheet", Texture.class);
        Texture absorb = directory.getEntry("absorb-animation", Texture.class);
        addSprite(avatar);



        avatar.createAnimators(dreamwalker, absorb);
        // Have to do after body is created
        avatar.setFilter();

        avatar.createSensor();
        //avatar.createScareSensor();
        //avatar.createFallSensor();
        aiManager.setPlayer(avatar);


        JsonValue maintainers = constants.get("mind-maintenance");
        JsonValue maintenancePos = maintainers.get("pos");

        for (int i = 0; i < maintenancePos.size; i++) {

            texture = directory.getEntry( "maintenance-walk", Texture.class );
            Texture turning = directory.getEntry("maintenance-turn", Texture.class);

            //texture = directory.getEntry("mind-maintenance-active", Texture.class);


            maintenance = new MindMaintenance(units, constants.get("mind-maintenance"), maintenancePos.get(i).asFloatArray(), this, dispatcher);
            //maintenance.setTexture(texture);
            addSprite(maintenance);
//            maintenance.createAnimators(texture, turning);
            // Have to do after body is created
            maintenance.setFilter();
            maintenance.createSensor();
            maintenance.createVisionSensor();
            enemies.add(maintenance);
            //aiManager.register(maintenance);
            aiCManager.register(maintenance);
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

            dreamDweller = new DreamDweller(units, constants.get("dream-dweller"), dwellersPos.get(i).asFloatArray(), this);
            dreamDweller.setTexture(texture);
            addSprite(dreamDweller);
            // Have to do after body is created
            dreamDweller.setFilter();
            dreamDweller.createSensor();
            enemies.add(dreamDweller);
            //aiManager.register(maintenance);
            aiCManager.register(dreamDweller);
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

        initTeleportAnimation();

    }

    public Vector2 getShardPos(int index) {
        return shardPos.get(index);
    }

    /** Called when player first touches a shard */
    public void registerShardForPickup(Shard s) {
        shardPickupTimers.put(s, 0f);
    }

    /** Called when player moves off a shard */
    public void cancelShardPickup(Shard s) {
        shardPickupTimers.remove(s);
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
        globalTime += dt;
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
            listener.exitScreen(this, EXIT_PAUSE);
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
            } else if (complete && !isLevelSelect) {
                pause();
                System.out.println("Going to level: " + nextIndex);
                listener.exitScreen(this, FROM_LEVELSELECT);
                return false;
            } else if (isLevelSelect && complete){
                System.out.println("Going to level: " + nextIndex);
                listener.exitScreen(this, FROM_LEVELSELECT);
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
        dispatcher.update();
        InputController input = InputController.getInstance();
        aiManager.update(dt);
        aiCManager.update(dt);
        GdxAI.getTimepiece().update(dt);

        for (Enemy e: enemies){
            if (e instanceof MindMaintenance && ((MindMaintenance) e).isShooting()){
                units = TiledMapInfo.PIXELS_PER_WORLD_METER;
                Vector2 position = e.getObstacle().getPosition();
                float direction = ((MindMaintenance) e).isFacingRight() ? 1 : -1;
                float spawnOffset = 0.1f;
                position.set(position.x , position.y);
                JsonValue bulletjv = constants.get("bullet");
                Texture texture = directory.getEntry("platform-bullet", Texture.class);

                ShieldWall wall = new ShieldWall(units, bulletjv, position, direction);
                shieldWalls.add(wall);
                wall.setTexture(texture);
                addQueuedObject(wall);
            }
            else if (e instanceof DreamDweller && ((DreamDweller) e).isShooting()) {
                units = TiledMapInfo.PIXELS_PER_WORLD_METER;
                Vector2 position = e.getObstacle().getPosition();
                Vector2 playerPos = avatar.getObstacle().getPosition();

                float speed = 11.5f;
                JsonValue spearjv = constants.get("spear");
                Texture spearTravelTex = directory.getEntry("platform-spear-travel-sprite", Texture.class);
                Texture spearEndTex    = directory.getEntry("platform-spear-end-sprite", Texture.class);

                float[] rightAngles = new float[] { -8f, -3f, 3f, 8f };
                float[] leftAngles  = new float[] { 188f, 183f, 177f, 172f };
                float[] yOffsets = new float[] {-0.8f, -0.4f, 0.4f, 0.8f };

                pendingSpears.clear();
                spearTimer = 0f;
                spearIndex = 0;

                float direction = (playerPos.x < position.x) ? -1f : 1f;
                float[] chosenAngles = (direction > 0) ? rightAngles : leftAngles;

                for (int i = 0; i < chosenAngles.length; i++) {
                    Vector2 spawnPos = new Vector2(position.x, position.y + yOffsets[i]);

                    float angleDeg = chosenAngles[i];
                    float angleRad = (float) Math.toRadians(angleDeg);

                    Vector2 velocity = new Vector2(
                        speed * (float) Math.cos(angleRad),
                        speed * (float) Math.sin(angleRad)
                    );

                    Spear spear = new Spear(units, spearjv, spawnPos, velocity, spearTravelTex, spearEndTex);
                    pendingSpears.add(spear);
                }
            }
        }
        if (pendingSpears.size > 0 && spearIndex < pendingSpears.size) {
            spearTimer += dt;
            if (spearTimer >= SPEAR_FIRE_INTERVAL) {
                Spear spear = pendingSpears.get(pendingSpears.size - 1 - spearIndex);
                spears.add(spear);
                addQueuedObject(spear);
                spearIndex++;
                spearTimer = 0f;
            }
        }


        for (Door d: doors){
            if(d.isActive() && checkCollectedAllGoals() && avatar.isTakingDoor()){
                setComplete(true);
                nextIndex = d.getDestination();
            }
        }

        for(ShieldWall s: shieldWalls){
            s.update(dt);

            if (s.isDead()){
                removeBullet(s);
                shieldWalls.remove(s);
            }

        }
        for (Spear p : spears) {

            if (p.isDead()){
                removeBullet(p);
                spears.remove(p);
            }
        }


        timeSinceStart += dt;

        if (lastCritterSawTime >= 0
            && !playerSlowed
            && timeSinceStart - lastCritterSawTime > .2f) {
            avatar.setMaxSpeed(avatar.getMaxSpeed() * slowSpeedFactor);
            playerSlowed = true;
        }

        avatar.setSlowed(playerSlowed);

        avatar.setTakingDoor(input.didTakeDoor());
        avatar.setMovement(input.getHorizontal() *avatar.getForce());

        boolean jumpKeyPressed = input.didPrimary();
        avatar.setJumping(jumpKeyPressed); // Triggers the initial jump
        avatar.setJumpHeld(input.isPrimaryHeld());

        avatar.tryStartHarvesting(input.didSecondary());


        if (input.didLShift()) {
            shadowMode = !shadowMode;

            avatar.setShroudMode(shadowMode);

        }

        if (shadowMode) {

            avatar.setTeleporting(input.didM1());
        } else {
            avatar.setStunning(input.didM1());
        }

        // player damage

        if (enemiesAlerted > 0) {
            avatar.setTakingDamage(true);
        } else {
            avatar.setTakingDamage(false);
        }


        if (avatar.isHarvesting())
        {
            drawScareEffect = true;
            if (!queuedHarvestedEnemy.isEmpty())
            {
                for (Enemy harvest_enemy : queuedHarvestedEnemy) {
                    if (!harvest_enemy.getObstacle().isRemoved()) {
                        harvest_enemy.getObstacle().markRemoved(true);
                        enemies.remove(harvest_enemy);
                        avatar.setFearMeter(avatar.getFearMeter() + 3);
                        //BANDAID SLOW FIX
                        if (harvest_enemy instanceof CuriosityCritter && ((CuriosityCritter) harvest_enemy).isAwareOfPlayer()) {
                            enemiesAlerted--;
                            crittersAlerted--;

                            if (crittersAlerted <= 0) {
                                crittersAlerted = 0; // Safety to prevent negative values
                                lastCritterSawTime = -1f;
                                if (playerSlowed) {
                                    avatar.resetMaxSpeed();
                                    playerSlowed = false;
                                }
                            }
                        }
                        //harvest_enemy.dispatchHarvest();
                        // ^^^^ WAS ORIGINALLY CALLED OVER MY BIG PARAGRAPH
                    }
                    removeHarvestedEnemy(harvest_enemy);
                }
            }
        }

        if (avatar.isStunning() && avatar.getFearMeter() > STUN_COST) {
            createBullet();
            avatar.setFearMeter(avatar.getFearMeter() - STUN_COST);
            // also turn avatar to direction of cursor
            Vector2 crosshairScreen = input.getMouse();

            // Unproject the crosshair screen position to get world coordinates
            Vector3 crosshairTemp = new Vector3(
                crosshairScreen.x,
                crosshairScreen.y,
                0
            );
            camera.unproject(crosshairTemp);
            Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);

            avatar.setFaceRight(crosshairWorld.x > avatar.getObstacle().getX());
        }


        avatar.setInteracting(input.isInteractDown());

        // shard handling
        Iterator<Map.Entry<Shard,Float>> iter = shardPickupTimers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Shard, Float> e = iter.next();
            Shard s = e.getKey();
            float t = e.getValue();

            if (input.isInteractDown() && avatar.getHoverInteract()) {

                System.out.println("absorbing shard...");
                t += dt;
                if (t >= 3f) {
                    avatar.setFearMeter(avatar.getFearMeter() + 3);

                    if (!s.getObstacle().isRemoved()) {
                        s.getObstacle().markRemoved(true);
                        incrementGoal();
                        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
                        Vector2 worldPos = s.getObstacle().getPosition();
                        Vector3 screen = new Vector3(
                            worldPos.x * units,
                            worldPos.y * units,
                            0
                        );
                        camera.project(screen);

                    }
                    iter.remove();
                    continue;
                }
                e.setValue(t);
            } else {
                // E released → reset the timer
                e.setValue(0f);
            }
        }
        if (!shardPickupTimers.isEmpty()) {
            vortexTimer += dt;
        } else {
            vortexTimer = 0f;
        }


        if (isTeleporting) {
            teleportAnimationTime += dt;
            // I dont like the last frames so I'm cutting it. Also this is bad hardcoding but the
            //Animator function currently doens't work
            if (teleportAnimationTime >= 0.06f * 10) {
                isTeleporting = false;
                teleportAnimationTime = 0f;
            }
        }

        if (avatar.isTeleporting() && avatar.getFearMeter() > TELEPORT_COST && !isTeleporting) {
            // Calculate teleport position
            teleport();
            teleportDirectionRight = avatar.isFacingRight();

            if (queuedTeleportPosition != null) {
                // Start teleport animation
                isTeleporting = true;
                teleportAnimationTime = 0f;
                teleportPosition = queuedTeleportPosition.cpy();
                Vector2 dir = new Vector2(preTeleportPosition).sub(teleportPosition);
                teleportAngle = dir.angleDeg();
                teleportAnimator.reset();

                // Apply the teleport
                avatar.getObstacle().setPosition(queuedTeleportPosition);
                if(!isLevelSelect){
                    avatar.setFearMeter(Math.max(0, avatar.getFearMeter() - TELEPORT_COST));
                }
                queuedTeleportPosition = null;
            }
        }


        if (queuedTeleportPosition != null) {
            avatar.getObstacle().setPosition(queuedTeleportPosition);
            avatar.setFearMeter(Math.max(0,avatar.getFearMeter() - TELEPORT_COST));
            queuedTeleportPosition = null; // Clear after applying
        }

        avatar.applyForce(world);

    }

    public void initTeleportAnimation() {
        Texture teleportTexture = directory.getEntry("teleport", Texture.class);
        teleportAnimator = new Animator(teleportTexture, 2, 10, 0.08f, 10, 0, 9, false);
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    private void teleport() {
        preTeleportPosition = avatar.getObstacle().getPosition().cpy();
        InputController input = InputController.getInstance();
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        Vector2 playerPosition = avatar.getObstacle().getPosition();

        // Get crosshair position in screen coordinates
        Vector2 crosshairScreen = input.getMouse();

        // Unproject the crosshair screen position to get world coordinates
        Vector3 crosshairTemp = new Vector3(crosshairScreen.x, crosshairScreen.y, 0);
        camera.unproject(crosshairTemp);
        Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);

        // clamp
        Vector2 delta = new Vector2(crosshairWorld).sub(playerPosition);
        // Convert teleport range from screen units to world units.
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
                                    isInsideSurface[0] = true;
                                    return false; // Stop
                                }
                                return true; // Continue
                            }
                        },
            crosshairWorld.x - TELEPORT_SURFACE_BUFFER, crosshairWorld.y - TELEPORT_SURFACE_BUFFER,
            crosshairWorld.x + TELEPORT_SURFACE_BUFFER, crosshairWorld.y + TELEPORT_SURFACE_BUFFER);

        if (isInsideSurface[0]) {
            System.out.println("Cannot place teleport in a surface");
            return;
        }

        queuedTeleportPosition = new Vector2(crosshairWorld.x, crosshairWorld.y);
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

        Texture texture = directory.getEntry("bullet-active", Texture.class);
        Texture texture2 = directory.getEntry("bullet-end", Texture.class);

        Bullet bullet = new Bullet(units, bulletjv, player.getPosition(), shootAngle.nor(), texture, texture2);
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

        for (PendingShard ps : pendingShardSpawns) {
            Gdx.app.log("PlatformScene", "– spawning shard at " + ps.location);
            spawnShardAtLocation(ps.location, ps.shard);
        }
        pendingShardSpawns.clear();

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
        // This shows off how powerful our new SpriteBatch is

        batch.begin(camera);



        dreamShardCountText.setText("Dream Shards: " + (totalShards - collectedShards));
        dreamShardCountText.layout();


        if (drawScareEffect)
        {
            if(drawScareCooldown <= drawScareLimit)
            {
                batch.end();
                drawScareEffect();
                batch.begin();
                drawScareCooldown++;
            } else {
                drawScareCooldown = 0;
                drawScareEffect = false;
            }
        }
        batch.end();
        // Draw the meshes (images)
        for (ObstacleSprite obj : sprites) {
            if (obj instanceof Shard && shardPickupTimers.containsKey((Shard)obj) && avatar.isInteracting()) {
                drawVortexOverlay((Shard) obj, dt, shardPickupTimers.get((Shard) obj));
            }
        }
        batch.begin();


        for(ObstacleSprite obj : sprites) {
            if (obj instanceof Spear) {
                ((Spear) obj).drawOwnAnimation(batch);
            }
            else {
                obj.draw(batch);
            }
        }



        if (debug) {
            // Draw the outlines
            for (ObstacleSprite obj : sprites) {
                obj.drawDebug( batch );
            }
        }

        drawTeleport(dt);
        batch.end();

        // shaders

        for (ObstacleSprite sprite : sprites) {
            if (sprite instanceof Bullet) {
                drawBulletEffect((Bullet)sprite, dt);
            }
        }


        // atmospheric shaders
    }

    private void drawBulletEffect(Bullet bullet, float dt) {
        float t = bullet.getTimeAlive();
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        Vector2 pos = bullet.getObstacle().getPosition();
        float cx = pos.x * units;
        float cy = pos.y * units;

        float w = bullet.getWidth()  * units;
        float h = bullet.getHeight() * units;

        ShaderProgram prev = batch.getShader();

        batch.setBlendMode(SpriteBatch.BlendMode.ADDITIVE);

        batch.setShader(glowShader);
        glowShader.bind();

        glowShader.setUniformf("u_time",      t);
        glowShader.setUniformf("u_flashDur",  0.2f);
        glowShader.setUniformf("u_glowInt",   0.5f);
        glowShader.setUniformi("u_spikes",    4);
        glowShader.setUniformf("u_dir", bullet.angle.x, bullet.angle.y);

        Texture glassyCopy = new Texture(abstractGlassyTexture.getTextureData());
        glassyCopy.bind(0);

        batch.begin();
        batch.draw(glassyCopy, cx - w/2, cy - h/2, w, h);
        batch.end();

        batch.setShader(prev);
        batch.setBlendMode(SpriteBatch.BlendMode.ALPHA_BLEND);
    }

    private void drawVortexOverlay(Shard shard, float dt, float sTime) {
        ShaderProgram prev = batch.getShader();
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        Vector2 pos  = shard.getObstacle().getPosition();

        // on‐screen center
        float cx = pos.x * units;
        float cy = pos.y * units;
        float radius = 165;  // half‐size of the effect in pixels

        // configure blank sprite to cover the circle

        // tell the batch to use your GDIAC Shader
        batch.setShader(vortexShader);
        vortexShader.bind();

        // upload all the Shadertoy‐style uniforms
        vortexShader.setUniformf("iResolution", radius*2, radius*2, 0f);
        vortexShader.setUniformf("iTime", sTime);
        vortexShader.setUniformf("iTimeDelta",   dt);
        vortexShader.setUniformf("iFrameRate",   1f/dt);
        vortexShader.setUniformi("iFrame", (int) Gdx.graphics.getFrameId());
        vortexShader.setUniformi("iChannel0", 0);
        vortexShader.setUniformi("iChannel1", 1);

        Texture glassyCopy = new Texture(abstractGlassyTexture.getTextureData());
        abstractGlassyTexture.bind(0);
        glassyCopy.bind(1);



        batch.setBlendMode(SpriteBatch.BlendMode.ADDITIVE);
        batch.setColor(Color.WHITE);
        // draw the quad with the ripple shader
        batch.begin();
        batch.draw(abstractGlassyTexture, cx - radius / 2, cy - radius / 2, radius, radius);
        batch.setBlendMode(SpriteBatch.BlendMode.PREMULT);
        radius = 50;
        batch.draw(abstractGlassyTexture, cx - radius / 2, cy - radius / 2, radius, radius);
        batch.end();


        batch.setBlendMode(SpriteBatch.BlendMode.PREMULT);
        batch.setShader(tendrilShader);
        radius = 210;
        tendrilShader.bind();
        tendrilShader.setUniformf("iResolution", radius, radius, 0f);
        tendrilShader.setUniformf("iTime", sTime);

        batch.begin();
        batch.draw(abstractGlassyTexture, cx - radius, cy - radius, radius * 2, radius * 2);
        batch.end();



        batch.setBlendMode(SpriteBatch.BlendMode.ALPHA_BLEND);
        // restore default shader
        batch.setShader(prev);

        batch.begin();

        float fadeStart = 2.5f, fadeDur = 0.5f;
        float alpha = (sTime <= fadeStart)
            ? 1f
            : MathUtils.clamp(1f - (sTime - fadeStart) / fadeDur, 0f, 1f);

        batch.setColor(1f, 1f, 1f, alpha);

        TextureRegion frame = swirlSprite.getCurrentFrame(dt);
        batch.draw(
            frame,
            cx - radius/4f, cy - radius/4f,
            radius/2f, radius/2f
        );


        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private void drawTeleport(float dt)
    {
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;

        if (isTeleporting) {
            TextureRegion currentFrame = teleportAnimator.getCurrentFrame(Gdx.graphics.getDeltaTime());

            float width = currentFrame.getRegionWidth();
            float height = currentFrame.getRegionHeight();

            float scale = 0.15f;
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;

            float angleToTeleport = teleportAngle + 180;

            float xOffset;

            if (teleportDirectionRight) {
                xOffset = -avatar.getWidth() * units * 1.75f;
            } else {
                xOffset = -avatar.getWidth() * units * 0.50f;
            }

            float x = teleportPosition.x * units + xOffset;
            float y = teleportPosition.y * units - scaledHeight * 3/5;

            TextureRegion frameToDraw = new TextureRegion(currentFrame);

            batch.setColor(Color.WHITE);


            Sprite sprite = new Sprite(frameToDraw);
            sprite.setSize(scaledWidth, scaledHeight);
            sprite.setOriginCenter();
            sprite.setRotation(angleToTeleport); // Now consistent
            sprite.setPosition(x, y);
            sprite.draw(batch);

            // Draw at teleport origin
            float cx = preTeleportPosition.x * units + xOffset;
            float cy = preTeleportPosition.y * units - scaledHeight * 3/5;

            sprite = new Sprite(frameToDraw);
            sprite.setSize(scaledWidth, scaledHeight);
            sprite.setOriginCenter();
            sprite.setRotation(teleportAngle);
            sprite.setPosition(cx, cy);

            sprite.draw(batch);
        }


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
        float mouseY = Gdx.input.getY();

        batch.drawText(dreamShardCountText, 11, 50);

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;

        // Get player position in WORLD coordinates
        Vector2 playerPosition = avatar.getObstacle().getPosition();

        // Get mouse in WORLD coordinates
        Vector3 crosshairTemp = new Vector3(mouseX, mouseY, 0);
        camera.unproject(crosshairTemp);
        Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);

        // done in WORLD coordinates just like teleport() method :(
        Vector2 delta = new Vector2(crosshairWorld).sub(playerPosition);
        float teleportRangeWorld = avatar.getTeleportRangeRadius() / units;
        if (delta.len() > teleportRangeWorld) {
            delta.nor().scl(teleportRangeWorld);
            crosshairWorld = new Vector2(playerPosition).add(delta);
        }

        // Revert to SCREEN coordinate
        Vector3 clampedScreenPos = new Vector3(crosshairWorld.x * units, crosshairWorld.y * units, 0);
        camera.project(clampedScreenPos);
        mouseX = clampedScreenPos.x;
        mouseY = clampedScreenPos.y;

        float crossX = mouseX - scaledWidth/2;
        float crossY = mouseY - scaledHeight/2;

        if (!shadowMode) {
            batch.draw(crosshairTexture, crossX, crossY, scaledWidth, scaledHeight);
        } else {
            boolean canTeleport = false;
            final boolean[] isInsideSurface = {false};
            //Vector2 finalCrosshairWorld = crosshairWorld;
            world.QueryAABB(new QueryCallback() {
                                @Override
                                public boolean reportFixture(Fixture fixture) {
                                    Object userData = fixture.getBody().getUserData();
                                    if (userData instanceof Surface) {
                                        isInsideSurface[0] = true;
                                        return false;
                                    }
                                    return true;
                                }
                            },
                crosshairWorld.x - TELEPORT_SURFACE_BUFFER, crosshairWorld.y - TELEPORT_SURFACE_BUFFER,
                crosshairWorld.x + TELEPORT_SURFACE_BUFFER, crosshairWorld.y + TELEPORT_SURFACE_BUFFER);

            if (!isInsideSurface[0]) {
                canTeleport = true;
            }

            Color prev = batch.getColor();
            batch.setColor(canTeleport ? Color.WHITE : Color.BLACK);
            batch.draw(crosshairTexture, crossX, crossY, scaledWidth, scaledHeight);
            batch.setColor(prev);
        }

        if (fearMeterSprite != null && avatar != null) {
            batch.setColor(Color.WHITE);
            int fearLevel = avatar.getFearMeter();
            int maxFear   = avatar.getMaxFearMeter();
            int totalFrames = 11;  // how many frames in the sprite sheet
            int frameIndex = Math.round((fearLevel/(float)maxFear) * (totalFrames - 1));

            TextureRegion meterFrame = fearMeterSprite.getKeyFrame(frameIndex);

            // where to draw

            float meterX = 40;
            float meterY = 500;

            float meterWidth  = 7* meterFrame.getRegionWidth() / units;
            float meterHeight = 7*meterFrame.getRegionHeight() / units;

            // Draw it
            batch.draw(
                meterFrame,
                meterX, meterY,
                meterWidth, meterHeight
            );
            if (visibilityIndicatorTexture != null) {
                float indicatorWidth  = visibilityIndicatorTexture.getWidth()  / units;
                float indicatorHeight = visibilityIndicatorTexture.getHeight() / units;

                float startX = meterX + 130;
                float startY = meterY + 50;

                for (int i = 0; i < enemiesAlerted; i++) {
                    float x = startX + i * (indicatorWidth + 10);  // 2px spacing
                    batch.draw(
                        visibilityIndicatorTexture,
                        x, startY,
                        indicatorWidth * 3, indicatorHeight * 3
                    );
                }
            }
        }

        if (avatar.isBlinded()) {
            float alpha = MathUtils.clamp(1.8f * (float)Math.pow(0.5, avatar.getBlindProgress()*20), 0f, 1.0f);
            batch.setColor(1, 1, 1, alpha);
            batch.draw(Texture2D.getBlank(), 0, 0, width, height);
            batch.setColor(Color.WHITE);
        }



        String cameraPositionText = "Camera Position: (" + camera.position.x + ", " + camera.position.y + ", zoom " + camera.zoom + ")" +
            "\n Viewport: (" + camera.viewportWidth + ", " + camera.viewportHeight + ")" +
            "\n Bounds: (" + bounds.width + ", " + bounds.height + ")" +
            "\n UICamera Pos: (" + uiCamera.position.x + ", " + uiCamera.position.y + ")" +
            "\n miniMapCamera Pos: (" + miniCam.position.x + ", " + miniCam.position.y + ")";
        //displayFont.draw(batch, cameraPositionText, 10, height - 100);

        /*if (complete && !failed) {
            batch.drawText(goodMessage, width/2, height/2);
        } else if (failed) {
            batch.drawText(badMessage, width/2, height/2);
        }*/

        // FADE TO BLACK
        if ((complete || failed) && isFading) {
            fadeAlpha = Math.min(fadeAlpha + fadeSpeed, 1.0f);

            fadeColor.a = fadeAlpha;
            batch.setColor(fadeColor);
            batch.draw(blankTexture, 0, 0, width, height);
            batch.setColor(Color.WHITE);
        }

        batch.setColor(Color.WHITE);

        batch.end();
    }

    private void drawScareEffect() {

        float u = 32;
        Vector2 worldPos = avatar.getObstacle().getPosition();
        float cx = worldPos.x * u;
        float cy = worldPos.y * u;
        float radius = 300;
        float size = scareEffectTexture.getRegionWidth() * 0.5f;
        float size2 = scareEffectTexture.getRegionHeight() * 0.5f;
        batch.begin();
        batch.draw(scareEffectTexture, avatar.getObstacle().getX() * u - size * 0.42f, avatar.getObstacle().getY() * u - size2 *0.57f, size, size2);
        batch.end();
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
        if (!resumingFromPause) {
            if (camera == null) {
                camera = new OrthographicCamera();
            }
            camera.setToOrtho(false, width, height);
            scale.x = 1;
            scale.y = 1;
            reset();
        } else {
            resumingFromPause = false; // Reset the flag
        }

    }

    public void setResumingFromPause(boolean value) {
        resumingFromPause = value;
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
                //clampCamera();
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
                if (dis < 30.0) {
                    float lerp = 6.0f;
                    position.x += (this.avatar.getObstacle().getX() * units - position.x) * lerp * delta;
                    position.y += (this.avatar.getObstacle().getY() * units - position.y) * lerp * delta;
                    camera.position.set(position);
                } else {
                    float lerp = 3.0f;
                    position.x += (this.avatar.getObstacle().getX() * units - position.x) * lerp * delta;
                    position.y += (this.avatar.getObstacle().getY() * units - position.y) * lerp * delta;
                    camera.position.set(position);
                }

                camera.zoom = 0.5f;
                clampCamera();
                camera.update();
            }

            // box2d
            if (preUpdate(delta)) {
                update(delta);
                postUpdate(delta);
            }
            ScreenUtils.clear(0.9f, 0.9f, 0.93f, 1.0f);




            Vector2 p = avatar.getObstacle().getPosition();
            miniCam.update();


            InputController input = InputController.getInstance();

            if (input.didToggleMap()) {
                miniMapActive = !miniMapActive;
                if (miniMapActive) miniMapTime = 0f;
                miniCam.position.set(defaultMiniCamPos);
            }

            Texture background1 = directory.getEntry("background1", Texture.class);

            float parallaxFactor = 0.9f;
            float scale = 0.7f;
            float scaledWidth = background1.getWidth() * scale;
            float scaledHeight = background1.getHeight() * scale;

            float bgX = camera.position.x * parallaxFactor - scaledWidth / 2f;
            float bgY = camera.position.y * parallaxFactor - scaledHeight / 2f;

            batch.begin(camera);
            batch.draw(background1, bgX, bgY, scaledWidth, scaledHeight);

            batch.end();


            if (miniMapActive) {
                sceneBuffer.begin();
                ScreenUtils.clear(0.9f, 0.9f, 0.93f, 1.0f);
                tiledRenderer.renderAllLayers(camera);
                draw(delta);
                sceneBuffer.end();

                Texture scene = sceneBuffer.getColorBufferTexture();
                scene.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                ShaderProgram prevShader = batch.getShader();
                Matrix4        prevProj  = batch.getProjectionMatrix().cpy();
                Matrix4        prevTrans = batch.getTransformMatrix();

                Matrix4 screenProj = new Matrix4().setToOrtho2D(
                    0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight());

                batch.setProjectionMatrix(screenProj);
                batch.setTransformMatrix(new Matrix4());

                ShaderProgram prev = batch.getShader();
                batch.setShader(blurShader);
                blurShader.bind();
                blurShader.setUniformi("u_scene", 0);
                blurShader.setUniformf("u_texel",
                    1f / scene.getWidth(), 1f / scene.getHeight());
                blurShader.setUniformf("u_strength",
                    Math.min(miniMapTime / 0.75f, 1f));

                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                scene.bind();

                batch.begin();
                batch.draw(scene,
                    0,                0,
                    Gdx.graphics.getWidth(),  Gdx.graphics.getHeight(),
                    0, 1, 1, 0);   // u,v coords
                batch.end();

                batch.setShader(prevShader);
                batch.setProjectionMatrix(prevProj);
                batch.setTransformMatrix(prevTrans);

                Vector3 worldMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                miniCam.unproject(worldMouse);

                // Compute the difference between the mouse and camera center.
                float dx = worldMouse.x - miniCam.position.x;
                float dy = worldMouse.y - miniCam.position.y;

                float thresholdX = miniCam.viewportWidth * 0.9f;
                float thresholdY = miniCam.viewportHeight * 0.9f;

                float moveX = 0, moveY = 0;
                if (Math.abs(dx) > thresholdX) {
                    moveX = dx - thresholdX;
                }
                if (Math.abs(dy) > thresholdY) {
                    moveY = dy - thresholdY;
                }

                miniCam.position.x += moveX * 0.01f;
                miniCam.position.y += moveY * 0.01f;


                miniCam.update();

                minimapRenderer.render(p, miniCam, miniMapTime);
                miniMapTime += delta;                 // advance the shader clock
            } else {
                tiledRenderer.renderAllLayers(camera);
                draw(delta);
                drawUI();
            }


//            sceneBuffer.begin();
//            ScreenUtils.clear(0.9f, 0.9f, 0.93f, 1.0f);
//            Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT);
//            tiledRenderer.renderAllLayers(camera);
//            draw(delta);
//            drawUI();
//            sceneBuffer.end();
//
//            Texture sceneTex = sceneBuffer.getColorBufferTexture();
//            sceneTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//            sceneTex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
//
//            ShaderProgram old = batch.getShader();
//            Matrix4 oldProj    = batch.getProjectionMatrix().cpy();
//            batch.setProjectionMatrix(uiCamera.combined);
//
//            batch.setShader(rayShader);
//            rayShader.bind();
//
//            rayShader.setUniformi ("u_scene",        0);
//            rayShader.setUniformf("u_resolution",
//                Gdx.graphics.getWidth(),
//                Gdx.graphics.getHeight()
//            );
//            rayShader.setUniformf("u_dim",           0.8f);
//            rayShader.setUniformf("u_lightColor",    1f,0.85f,0.6f);
//            rayShader.setUniformf("u_lightPos",      avatar.getObstacle().getX()/bounds.width,
//                1);
//            rayShader.setUniformf("u_lightRadius",   0.2f);
//            rayShader.setUniformf("u_bloomStrength", 0.04f);
//            rayShader.setUniformf("u_bloomRadius",   6.0f);
//
//            Gdx.gl.glActiveTexture(GL30.GL_TEXTURE0);
//            sceneTex.bind(0);
//
//            batch.begin();
//            batch.draw(sceneTex,
//                0, height,
//                width, -height);
//            batch.end();
//            batch.setShader(old);
//            batch.setProjectionMatrix(oldProj);
//
//            drawUI();
        }
    }

    private void clampCamera() {
        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;

        float halfViewportWidth = camera.viewportWidth / 2;
        float halfViewportHeight = camera.viewportHeight / 2;

        // Convert world bounds to screen coordinates
        float minX = 0 + halfViewportWidth * camera.zoom;
        float maxX = bounds.width * units - (halfViewportWidth * camera.zoom);
        float minY = 0 + halfViewportHeight * camera.zoom;
        float maxY = bounds.height * units - (halfViewportHeight * camera.zoom);

        camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
        camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
    }

    public void spawnShardAtLocation(Vector2 world, Shard s) {
        JsonValue goal = constants.get("goal");
        Shard newShard = new Shard(
            TiledMapInfo.PIXELS_PER_WORLD_METER,
            goal,
            world.x, world.y,
            s.id
        );

        // set up the sprite
        newShard.setTexture(directory.getEntry("shard", Texture.class));
        newShard.getObstacle().setName("goal_" + newShard.id);
        addSprite(newShard);
        newShard.setFilter();
        // record it in your internal lists
        shardPos.add(newShard.id, new Vector2(world.x, world.y));
    }

    //telegraph
    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == MessageType.CRITTER_SEES_PLAYER) {
            CuriosityCritter critter = (CuriosityCritter) msg.extraInfo;
            onPlayerSpotted(critter);
            return true;
        }
        if (msg.message == MessageType.CRITTER_LOST_PLAYER) {
            CuriosityCritter critter = (CuriosityCritter) msg.extraInfo;
            onPlayerLost(critter);
            return true;
        }
        return false;
    }

    public void onPlayerSpotted(CuriosityCritter enemy) {
        System.out.println("Message Dispatcher received - enemy raycast sees player");
        enemiesAlerted++;
        crittersAlerted++;
        lastCritterSawTime = timeSinceStart;
        // reset any previous slow (so repeated sees restart the 1s timer)
        if (playerSlowed) {
            avatar.resetMaxSpeed();
            playerSlowed = false;
        }
    }

    public void onPlayerLost(CuriosityCritter enemy) {
        System.out.println("Message Dispatcher received - enemy raycast lost player");
        enemiesAlerted--;
        crittersAlerted--;
        if (playerSlowed) {
            avatar.resetMaxSpeed();
            playerSlowed = false;
        }
        //last critter lost player
        if (crittersAlerted == 0) {
            lastCritterSawTime = -1f;
        }
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

