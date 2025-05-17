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
    /** Reference to the sprite batch */
    protected SpriteBatch batch;

    protected float width;
    protected float height;

    /** The physics constants */
    protected JsonValue constants;

    /** The font for giving messages to the player */
    protected BitmapFont displayFont;
    /** LAYOUT FOR DRAWING MESSAGE*/
    private TextLayout goodMessage;

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

    // Audio
    /** The sound effect manager that is used to play sounds  */
    SoundEffectManager soundManager = SoundEffectManager.getInstance();
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

    /** manages ai control for all entities */
    private AIControllerManager aiCManager;
    private AIManager aiManager;

    private PooledList<Enemy> enemies = new PooledList<>();
    private LevelContactListener levelContactListener;


    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    protected ObjectSet<Fixture> shadowSensorFixtures;

    private Texture shardTexture;
    private Texture stunProjectileTexture;
    private Texture spikeTexture;
    private Texture miniMapTexture;
    private Texture teleportTexture;
    private Texture backgroundTexture;
    private TextureRegion currentTeleportFrame;
    private TextureRegion teleportFrameToDraw;
    private TextureRegion swirlFrame;
    private Texture dreamwalkerTexture;
    private Texture absorbTexture;
    private Texture critterTexture;

    /** Enemy textures */
    private Texture maintenanceTexture;
    private Texture dwellerTexture;
    private Texture spearTravelTex;
    private Texture spearEndTex;
    private Texture wallTravelTex;
    private Texture wallEndTex;

    /** Texture for fear meter and visibility indicators **/
    private Texture fearMeterTexture;
    private Texture visibilityIndicatorTexture;
    private TextureRegion meterFrame;

    /** Animation Textures */
    private Texture fearTexture;
    private Texture swirlTexture;

    /** Texture asset for mouse crosshairs */
    private TextureRegion crosshairTexture;
    private Animator fearMeterSprite;

    private TextureRegion scareEffectTexture;

    private int drawScareLimit;
    private int drawScareCooldown;
    private boolean drawScareEffect = false;

    private Vector2 queuedTeleportPosition = null;
    private PooledList<Enemy> queuedHarvestedEnemy = new PooledList<Enemy>();

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

    private Animator swirlSprite;

    /** if shadow mode is on, which changes player m1 to a teleport rather than an attack */
    private boolean shadowMode;

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

    // SHADERS
    private Shader vortexShader, tendrilShader, rippleShader;
    private Shader rayShader, blurShader;
    private FrameBuffer sceneBuffer;
    private Texture blankTexture;
    private Texture abstractGlassyTexture;

    private VertexBuffer bulletVB;
    private Shader glowShader;

    /** Used to ensure scene doesn't reset when resuming from pause scene*/
    private boolean resumingFromPause = false;

    private Array<Spear> pendingSpears = new Array<>();
    private float spearTimer = 0f;
    private int spearIndex = 0;
    private static final float SPEAR_FIRE_INTERVAL = 0.3f;
    private boolean preparingSpears = false;

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

    public void performHarvest(Enemy enemy)
    {
        enemy.takeDamage();
        if(enemy.isDead()) {
            queuedHarvestedEnemy.add(enemy);
        }
    }

    public void removeHarvestedEnemy(Enemy enemy) {
        queuedHarvestedEnemy.remove(enemy);
    }

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

        // I HATE MEMORY LEAKS (GRRRRRRR)
        shardTexture.dispose();
        stunProjectileTexture.dispose();
        spikeTexture.dispose();
        miniMapTexture.dispose();
        teleportTexture.dispose();
        backgroundTexture.dispose();
        dreamwalkerTexture.dispose();
        absorbTexture.dispose();
        critterTexture.dispose();
        maintenanceTexture.dispose();
        dwellerTexture.dispose();
        spearTravelTex.dispose();
        spearEndTex.dispose();
        wallTravelTex.dispose();
        wallEndTex.dispose();
        fearMeterTexture.dispose();
        visibilityIndicatorTexture.dispose();
        fearTexture.dispose();
        swirlTexture.dispose();
        abstractGlassyTexture.dispose();
        blankTexture.dispose();
        tiledMap.disposeMap();

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
        this.directory = directory;
        this.mapkey = mapkey;
        tiledLevelName = tiled;
        this.isLevelSelect = isLevelSelect;
        constants = directory.getEntry(mapkey,JsonValue.class);
        JsonValue defaults = constants.get("world");
        scale = new Vector2();
        bounds = new Rectangle(0,0,defaults.get("bounds").getFloat( 0 ), defaults.get("bounds").getFloat( 1 ));
        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

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

        // TEXTURES
        maintenanceTexture = directory.getEntry( "maintenance-sprite-sheet", Texture.class);
        dwellerTexture = directory.getEntry("dream-dweller-active", Texture.class);
        spearTravelTex = directory.getEntry("spear-travel-sprite", Texture.class);
        spearEndTex = directory.getEntry("spear-end-sprite", Texture.class);
        wallTravelTex = directory.getEntry("wall-travel-sprite", Texture.class);
        wallEndTex = directory.getEntry("wall-end-sprite", Texture.class);
        fearTexture = directory.getEntry("fear-meter-sprite-sheet", Texture.class);
        swirlTexture = directory.getEntry("swirl-active", Texture.class);
        crosshairTexture  = new TextureRegion(directory.getEntry( "crosshair", Texture.class ));
        scareEffectTexture = new TextureRegion(directory.getEntry("platform-scare-effect", Texture.class));
        shardTexture = directory.getEntry("shard-sprite", Texture.class);
        stunProjectileTexture = directory.getEntry("bullet-sprite-sheet", Texture.class);
        fearMeterTexture = directory.getEntry("fear-meter", Texture.class);
        visibilityIndicatorTexture = directory.getEntry("visibility-indicator", Texture.class);
        spikeTexture = directory.getEntry("dayglow-spike", Texture.class);
        miniMapTexture = sceneBuffer.getColorBufferTexture();
        abstractGlassyTexture = directory.getEntry("abstract-glassy", Texture.class);
        teleportTexture = directory.getEntry("teleport", Texture.class);
        backgroundTexture = directory.getEntry("background1", Texture.class);
        critterTexture = directory.getEntry( "curiosity-critter-active", Texture.class );

        // REFERENCE FOR NEW FONT
        displayFont = directory.getEntry( "shared-retro" ,BitmapFont.class);
        /*
        goodMessage = new TextLayout();
        goodMessage.setFont( displayFont );
        goodMessage.setAlignment( TextAlign.middleCenter );
        goodMessage.setColor( Color.YELLOW );
        goodMessage.setText("VICTORY!");
        goodMessage.layout();
         */

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

        // SOUNDS
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

        createAnimators(fearTexture,swirlTexture);

        dispatcher.addListener(this, MessageType.CRITTER_SEES_PLAYER);
        dispatcher.addListener(this, MessageType.CRITTER_LOST_PLAYER);
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

        shadowMode = false;
        avatar.setShroudMode(false);
        pendingSpears.clear();
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

        float units = TiledMapInfo.PIXELS_PER_WORLD_METER;
        minimapRenderer = new MinimapRenderer(tiledMap.map, batch, units, bounds.width, bounds.height);
        int level = 0;

        // entity spawn handling from tiled
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
                    critter = new CuriosityCritter(units, constants.get("curiosity-critter"), new float[]{worldX,worldY}, this, dispatcher);
                    critter.setTexture(critterTexture);
                    addSprite(critter);


                    critterTexture = directory.getEntry( "critter-sprite-sheet", Texture.class);
                    critter.createAnimators(critterTexture);

                    // Have to do after body is created

                    critter.setFilter();
                    critter.createSensor();
                    enemies.add(critter);
                    aiManager.register(critter);
                }
                if (o.getName().startsWith("dream dweller")) {
                    dreamDweller = new DreamDweller(units, constants.get("dream-dweller"), new float[]{worldX, worldY}, this);
                    dreamDweller.setTexture(dwellerTexture);
                    addSprite(dreamDweller);
                    dreamDweller.setFilter();
                    dreamDweller.createSensor();
                    enemies.add(dreamDweller);
                    aiCManager.register(dreamDweller);
                    Texture dwellerSheet1 = directory.getEntry("dweller-sheet-1", Texture.class);
                    Texture dwellerSheet2 = directory.getEntry("dweller-sheet-2", Texture.class);
                    Texture dwellerSheet3 = directory.getEntry("dweller-sheet-3", Texture.class);
                    dreamDweller.createAnimators(dwellerSheet1, dwellerSheet2, dwellerSheet3);
                }
                if (o.getName().startsWith("mind maintenance")) {
                    maintenance = new MindMaintenance(units, constants.get("mind-maintenance"), new float[]{worldX, worldY}, this, dispatcher);
                    addSprite(maintenance);
                    maintenanceTexture = directory.getEntry( "maintenance-sprite-sheet", Texture.class);
                    maintenance.createAnimators(maintenanceTexture);
                    maintenance.setFilter();
                    maintenance.createSensor();
                    enemies.add(maintenance);
                    aiCManager.register(maintenance);
                }
            }
        }

        // dream shard creation from tiled layer
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
                    shardTexture = directory.getEntry("shard-sprite", Texture.class);
                    Shard goalShard = new Shard(units, goal, worldX, worldY, shardID, shardTexture);
                    shardPos.add(shardID, new Vector2(worldX, worldY));
                    goalShard.setTexture(shardTexture);
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
                float x = o.getProperties().get("x", Float.class);
                float y = o.getProperties().get("y", Float.class);
                float height = o.getProperties().get("height", Float.class);
                float width = o.getProperties().get("width", Float.class);

                float worldX = x / units;
                float worldWidth = width/units;

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
                            DayglowSpike spike = new DayglowSpike(units, worldX, worldY, worldWidth, worldHeight, o.getProperties().get("laser", Boolean.class), dir);
                            spike.setTexture(spikeTexture);
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

        addSprite(avatar);
        dreamwalkerTexture = directory.getEntry("player-sprite-sheet", Texture.class);
        absorbTexture = directory.getEntry("absorb-animation", Texture.class);
        avatar.createAnimators(dreamwalkerTexture, absorbTexture);
        avatar.setFilter();
        avatar.createSensor();
        aiManager.setPlayer(avatar);

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
                position.set(position.x , position.y);
                JsonValue bulletjv = constants.get("bullet");
                ShieldWall wall = new ShieldWall(units, bulletjv, position, direction, wallTravelTex, wallEndTex);
                shieldWalls.add(wall);
                addQueuedObject(wall);
            }
            else if (e instanceof DreamDweller && ((DreamDweller) e).isShooting()) {
                units = TiledMapInfo.PIXELS_PER_WORLD_METER;
                Vector2 position = e.getObstacle().getPosition();
                Vector2 playerPos = avatar.getObstacle().getPosition();
                JsonValue spearjv = constants.get("spear");

                float[] angleOffsets = new float[] { -8f, -3f, 3f, 8f };

                pendingSpears.clear();
                spearTimer = 0f;
                spearIndex = 0;

                for (int i = 0; i < angleOffsets.length; i++) {
                    Vector2 spawnPos = new Vector2(position.x, position.y + 1);
                    Vector2 toPlayer = new Vector2(playerPos.x - spawnPos.x, playerPos.y - spawnPos.y).nor();
                    toPlayer.rotateDeg(angleOffsets[i]);
                    Spear spear = new Spear(units, spearjv, spawnPos, toPlayer, spearTravelTex, spearEndTex);
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
                    }
                    removeHarvestedEnemy(harvest_enemy);
                }
            }
        }

        if (avatar.isStunning() && avatar.getFearMeter() > STUN_COST) {
            createBullet();
            if(!isLevelSelect) {
                avatar.setFearMeter(avatar.getFearMeter() - STUN_COST);
            }
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
                    // fully replenish fear when player picks up shard
                    avatar.rechargeFearMeter();

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

        if (isTeleporting) {
            teleportAnimationTime += dt;
            // I dont like the last frames so I'm cutting it. Also this is bad hardcoding but the
            //Animator function currently doens't work
            if (teleportAnimationTime >= 0.06f * 10) {
                isTeleporting = false;
                teleportAnimationTime = 0f;
            }
        }

        if (avatar.isTeleporting() && avatar.getFearMeter() > TELEPORT_COST) {
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
        teleportTexture = directory.getEntry("teleport", Texture.class);
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

        Vector2 delta = new Vector2(crosshairWorld).sub(playerPosition);
        // Convert teleport range from screen units to world units.
        float teleportRangeWorld = avatar.getTeleportRangeRadius() / units;
        if (delta.len() > teleportRangeWorld) {
            delta.nor().scl(teleportRangeWorld);
            crosshairWorld = new Vector2(playerPosition).add(delta);
        }

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

        Bullet bullet = new Bullet(units, bulletjv, player.getPosition(), shootAngle.nor(), stunProjectileTexture);
        addQueuedObject(bullet);
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
            obj.draw(batch);
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

        batch.begin();
        batch.draw(abstractGlassyTexture, cx - w/2, cy - h/2, w, h);
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

        abstractGlassyTexture.bind(0);

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

        swirlFrame = swirlSprite.getCurrentFrame(dt);
        batch.draw(
            swirlFrame,
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
            currentTeleportFrame = teleportAnimator.getCurrentFrame(Gdx.graphics.getDeltaTime());
            teleportFrameToDraw = new TextureRegion(currentTeleportFrame);

            float width = currentTeleportFrame.getRegionWidth();
            float height = currentTeleportFrame.getRegionHeight();

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

            batch.setColor(Color.WHITE);


            Sprite sprite = new Sprite(teleportFrameToDraw);
            sprite.setSize(scaledWidth, scaledHeight);
            sprite.setOriginCenter();
            sprite.setRotation(angleToTeleport); // Now consistent
            sprite.setPosition(x, y);
            sprite.draw(batch);

            // Draw at teleport origin
            float cx = preTeleportPosition.x * units + xOffset;
            float cy = preTeleportPosition.y * units - scaledHeight * 3/5;

            sprite = new Sprite(teleportFrameToDraw);
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

            meterFrame = fearMeterSprite.getKeyFrame(frameIndex);

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
                    float x = startX + i * (indicatorWidth + 10);
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
                camera.update();
            }
            else {
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
                camera.zoom = 0.6f;
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

            float parallaxFactor = 0.9f;
            float scale = 0.7f;
            float scaledWidth = backgroundTexture.getWidth() * scale;
            float scaledHeight = backgroundTexture.getHeight() * scale;

            float bgX = camera.position.x * parallaxFactor - scaledWidth / 2f;
            float bgY = camera.position.y * parallaxFactor - scaledHeight / 2f;

            batch.begin(camera);
            batch.draw(backgroundTexture, bgX, bgY, scaledWidth, scaledHeight);

            batch.end();


            if (miniMapActive) {
                sceneBuffer.begin();
                ScreenUtils.clear(0.9f, 0.9f, 0.93f, 1.0f);
                tiledRenderer.renderAllLayers(camera);
                draw(delta);
                sceneBuffer.end();
                miniMapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                ShaderProgram prevShader = batch.getShader();
                Matrix4        prevProj  = batch.getProjectionMatrix().cpy();
                Matrix4        prevTrans = batch.getTransformMatrix();

                Matrix4 screenProj = new Matrix4().setToOrtho2D(
                    0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight());

                batch.setProjectionMatrix(screenProj);
                batch.setTransformMatrix(new Matrix4());

                batch.setShader(blurShader);
                blurShader.bind();
                blurShader.setUniformi("u_scene", 0);
                blurShader.setUniformf("u_texel",
                    1f / miniMapTexture.getWidth(), 1f / miniMapTexture.getHeight());
                blurShader.setUniformf("u_strength",
                    Math.min(miniMapTime / 0.75f, 1f));

                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                miniMapTexture.bind();

                batch.begin();
                batch.draw(miniMapTexture,
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
            }
            else {
                tiledRenderer.renderAllLayers(camera);
                draw(delta);
                drawUI();
            }
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
            s.id,
            shardTexture
        );

        // set up the sprite
        newShard.setTexture(shardTexture);
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

