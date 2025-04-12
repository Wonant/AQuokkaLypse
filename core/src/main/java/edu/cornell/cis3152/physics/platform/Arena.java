///*
// * PlatformScene.java
// *
// * This is the game scene (player mode) specific to the platforming mini-game.
// * You SHOULD NOT need to modify this file. However, you may learn valuable
// * lessons for the rest of the lab by looking at it.
// *
// * Based on the original PhysicsDemo Lab by Don Holden, 2007
// *
// * Author:  Walker M. White
// * Version: 2/8/2025
// */
//package edu.cornell.cis3152.physics.platform;
//
//import com.badlogic.gdx.graphics.g2d.Sprite;
//import edu.cornell.cis3152.physics.AIControllerManager;
//import edu.cornell.cis3152.physics.ObstacleGroup;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import com.badlogic.gdx.*;
//import com.badlogic.gdx.graphics.*;
//import com.badlogic.gdx.math.Rectangle;
//import com.badlogic.gdx.math.Vector2;
//import com.badlogic.gdx.physics.box2d.*;
//import com.badlogic.gdx.utils.JsonValue;
//import com.badlogic.gdx.utils.ScreenUtils;
//import edu.cornell.gdiac.assets.AssetDirectory;
//import edu.cornell.gdiac.util.*;
//import edu.cornell.gdiac.graphics.*;
//import edu.cornell.gdiac.physics2.*;
//import com.badlogic.gdx.graphics.g2d.TextureRegion;
//import com.badlogic.gdx.math.*;
//import com.badlogic.gdx.utils.*;
//
//import edu.cornell.cis3152.physics.InputController;
////import edu.cornell.cis3152.physics.rocket.Box;
////import edu.cornell.cis3152.physics.PhysicsScene;
//import edu.cornell.gdiac.audio.SoundEffectManager;
//
//import static edu.cornell.cis3152.physics.platform.PlatformScene.STUN_COST;
//import static edu.cornell.cis3152.physics.platform.PlatformScene.TELEPORT_COST;
//
//public class Arena implements ContactListener, Screen{
//    /** Exit code for quitting the game */
//    public static final int EXIT_QUIT = 0;
//    /** Exit code for advancing to next level */
//    public static final int EXIT_NEXT = 1;
//    /** Exit code for jumping back to previous level */
//    public static final int EXIT_PREV = 2;
//    /** How many frames after winning/losing do we continue? */
//    public static final int EXIT_COUNT = 180;
//
//    public static final int EXIT_ARENA = 444;
//
////    protected AssetDirectory directory;
////    protected OrthographicCamera camera;
////    protected OrthographicCamera uiCamera;
////    protected SpriteBatch batch;
////    protected float width, height;
////    protected JsonValue constants;
////    protected World world;
////    protected Rectangle bounds;
////    protected Vector2 scale;
////    private int collectedGoals;
////
////    public static final int WORLD_VELOC = 6;
////    public static final int WORLD_POSIT = 2;
////
////    /**all sprites*/
////    protected PooledList<ObstacleSprite> sprites  = new PooledList<ObstacleSprite>();
////    protected PooledList<ObstacleSprite> addQueue = new PooledList<ObstacleSprite>();
////    private ScreenListener listener;
////
////    private int countdown = -1;
////    private int max_enemies = 5;
////    private int totalEnemies = 0;
////    // This variable holds the type of enemy pending to be added via mouse click.
////    // It can be "critter", "guard", or "dweller", or null if none is pending.
////    private String pendingEnemyType = null;
////
////
////    /** Lists of the current enemies in world */
////
////
////    private HashMap<Teleporter, Float> teleporterCreationTimes = new HashMap<>();
////
////    private Texture background;
////
////    /** Whether this is an active controller */
////    protected boolean active;
////    /** Whether we have completed this level */
////    protected boolean complete;
////    /** Whether we have failed at this world (and need a reset) */
////    protected boolean failed;
////    /** Whether debug mode is active */
////    protected boolean debug;
////
////
////    /** Texture asset for character avatar */
////    private TextureRegion avatarTexture;
////    /** Texture asset for the spinning barrier */
////    private TextureRegion barrierTexture;
////    /** Texture asset for the bullet */
////    private TextureRegion bulletTexture;
////    /** Texture asset for the bridge plank */
////    private TextureRegion bridgeTexture;
////
////
////    /** The default sound volume */
////    private float volume;
////
////    /** Reference to the character avatar */
////    private Player avatar;
////    private CuriosityCritter critter;
////    private MindMaintenance maintenance;
////    private TextureRegion visionConeRegion;
////    private Texture vision;
////    private int prev_debug;
////    private Sprite visionCone;
////    private float timeElapsed = 0f;
////
////    private HashMap<CuriosityCritter, Sprite> visionCones;
////    private AIControllerManager aiManager;
////    /** Reference to the goalDoor (for collision detection) */
////    private Shard goalShard;
////
////    /** Mark set to handle more sophisticated collision callbacks */
////    protected ObjectSet<Fixture> sensorFixtures;
////
////    /** Texture for fear meter**/
////    private Texture fearMeterTexture;
////    /** Texture asset for mouse crosshairs */
////    private TextureRegion crosshairTexture;
////
////    private TextureRegion scareEffectTexture;
////    private int drawScareLimit;
////    private int drawScareCooldown;
////    private boolean drawScareEffect = false;
////
////    private Vector2 queuedTeleportPosition = null;
////    private String mapkey;
////
////    private boolean placementMode = false;
////
////    private CuriosityCritter queuedHarvestedEnemy = null;
////    //private CuriosityCritter queuedHarvestedEnemy = null;
////    private DreamDweller queuedHarvestedEnemyD = null;
////
////
////    private boolean invulnerable = false;
////    private int totalGoals;
////
////    private Teleporter currentTeleporter = null;
////    // sophisticated shadow collisions for teleport
////    protected ObjectSet<Fixture> shadowSensorFixtures;
////
////
////    /**
////     *
////     */
////    public Arena(AssetDirectory directory,String mapkey) {
////
////        this.directory = directory;
////        this.mapkey = mapkey;
////        constants = directory.getEntry(mapkey,JsonValue.class);
////        JsonValue config = constants.get("world");
////
////        crosshairTexture  = new TextureRegion(directory.getEntry( "ragdoll-crosshair", Texture.class ));
////        scareEffectTexture = new TextureRegion(directory.getEntry("platform-scare-effect", Texture.class));
////        fearMeterTexture = directory.getEntry("fear-meter", Texture.class);
////
////        batch = new SpriteBatch();
////        scale = new Vector2();
////        bounds = new Rectangle(0,0, config.get("bounds").getFloat( 0 ), config.get("bounds").getFloat( 1 ));
////        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
////
////        complete = false;
////        failed = false;
////        debug  = false;
////        active = false;
////
////        world.setContactListener(this);
////        sensorFixtures = new ObjectSet<Fixture>();
////
////        // Pull out sounds
//////        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
//////        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
//////        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
////        volume = constants.getFloat("volume", 1.0f);
////
////        shadowSensorFixtures = new ObjectSet<Fixture>();
////
////        drawScareLimit = 60;
////        drawScareCooldown = 0;
////
////        uiCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
////        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
////        uiCamera.update();
////    }
////
////
////
////    /**
////     * Returns true if debug mode is active.
////     *
////     * If true, all objects will display their physics bodies.
////     *
////     * @return true if debug mode is active.
////     */
////    public boolean isDebug( ) {
////        return debug;
////    }
////
////    /**
////     * Sets whether debug mode is active.
////     *
////     * If true, all objects will display their physics bodies.
////     *
////     * @param value whether debug mode is active.
////     */
////    public void setDebug(boolean value) {
////        debug = value;
////    }
////
////
////    public boolean isComplete( ) {
////        return complete;
////    }
////
////
////    public void setComplete(boolean value) {
////        if (value) {
////            countdown = EXIT_COUNT;
////        }
////        complete = value;
////    }
////
////    public boolean isFailure( ) {
////        return failed;
////    }
////
////    public void setFailure(boolean value) {
////        if (value) {
////            countdown = EXIT_COUNT;
////        }
////        failed = value;
////    }
////
////    /**
////     * Returns true if this is the active screen
////     *
////     * @return true if this is the active screen
////     */
////    public boolean isActive( ) {
////        return active;
////    }
////
////    /**
////     * Returns the sprite batch associated with this scene
////     *
////     * The canvas is shared across all scenes.
////     *
////     * @return the sprite batch associated with this scene
////     */
////    public SpriteBatch getSpriteBatch() {
////        return batch;
////    }
////
////    /**
////     * Sets the sprite batch associated with this scene
////     *
////     * The sprite batch is shared across all scenes.
////     *
////     * @param batch the sprite batch associated with this scene
////     */
////    public void setSpriteBatch(SpriteBatch batch) {
////        this.batch = batch;
////    }
////
////    /**
////     * Disposes of all (non-static) resources allocated to this mode.
////     */
////    public void dispose() {
////        if (world != null) {
////            for(ObstacleSprite sprite : sprites) {
////                Obstacle obj = sprite.getObstacle();
////                obj.deactivatePhysics(world);
////            }
////        }
////        sprites.clear();
////        addQueue.clear();
////        world.dispose();
////        addQueue = null;
////        sprites = null;
////        bounds = null;
////        scale = null;
////        world = null;
////        batch = null;
////        fearMeterTexture.dispose();
////        background.dispose();
////    }
////
////    /**
////     *
////     * Adds a physics sprite in to the insertion queue.
////     *
////     * Objects on the queue are added just before collision processing. We do
////     * this to control object creation.
////     *
////     * param sprite The sprite to add
////     */
////    public void addQueuedObject(ObstacleSprite sprite) {
////        assert inBounds(sprite) : "Object is not in bounds";
////        addQueue.add(sprite);
////    }
////
////    /**
////     * Immediately adds a physics sprite to the physics world
////     *
////     * param sprite The sprite to add
////     */
////    protected void addSprite(ObstacleSprite sprite) {
////        assert inBounds(sprite) : "Sprite is not in bounds";
////        sprites.add(sprite);
////        sprite.getObstacle().activatePhysics(world);
////    }
////
////    /**
////     * Immediately adds a sprite group to the physics world
////     * param group  The sprite group to add
////     */
////    protected void addSpriteGroup(ObstacleGroup group) {
////        for(ObstacleSprite sprite : group.getSprites()) {
////            assert inBounds( sprite ) : "Sprite is not in bounds";
////            sprites.add( sprite );
////        }
////        group.activatePhysics(world);
////    }
////
////    /**
////     * Returns true if the sprite is in bounds.
////     *
////     * This assertion is useful for debugging the physics.
////     *
////     * @param sprite    The sprite to check.
////     *
////     * @return true if the sprite is in bounds.
////     */
////    public boolean inBounds(ObstacleSprite sprite) {
////        Obstacle obj = sprite.getObstacle();
////        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
////        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
////        return horiz && vert;
////    }
////
////
////    /**
////     * Processes the physics for this frame
////     *
////     * Once the update phase is over, but before we draw, we are ready to
////     * process physics. The primary method is the step() method in world. This
////     * implementation works for all applications and should not need to be
////     * overwritten.
////     *
////     * @param dt    Number of seconds since last animation frame
////     */
////    public void postUpdate(float dt) {
////        // Add any objects created by actions
////        while (!addQueue.isEmpty()) {
////            addSprite(addQueue.poll());
////        }
////
////        // Turn the physics engine crank.
////        // NORMALLY we would use a fixed step, not dt
////        // But that is harder and a topic of the advanced class
////        world.step(dt,WORLD_VELOC,WORLD_POSIT);
////
////        // Garbage collect the deleted objects.
////        // Note how we use the linked list nodes to delete O(1) in place.
////        // This is O(n) without copying.
////        Iterator<PooledList<ObstacleSprite>.Entry> iterator = sprites.entryIterator();
////        while (iterator.hasNext()) {
////            PooledList<ObstacleSprite>.Entry entry = iterator.next();
////            ObstacleSprite sprite = entry.getValue();
////            Obstacle obj = sprite.getObstacle();
////            if (obj.isRemoved()) {
////                obj.deactivatePhysics(world);
////                entry.remove();
////            } else {
////                // Note that update is called last!
////                obj.update(dt);
////            }
////        }
////    }
////
////    /**
////     * Draws the physics objects to the screen
////     *
////     * For simple worlds, this method is enough by itself. It will need to be
////     * overriden if the world needs fancy backgrounds or the like.
////     *
////     * The method draws all objects in the order that they were added.
////     *
////     * @param dt    Number of seconds since last animation frame
////     */
////    public void draw(float dt) {
////        // Clear the screen (color is homage to the XNA years)
////        ScreenUtils.clear(0.39f, 0.58f, 0.93f, 1.0f);
////
////        // This shows off how powerful our new SpriteBatch is
////        batch.begin(camera);
////        background = directory.getEntry("background-proto", Texture.class);
////        batch.draw(background, 0, 0);
////
////        if (drawScareEffect)
////        {
////            if(drawScareCooldown <= drawScareLimit)
////            {
////                drawScareEffect();
////                drawScareCooldown++;
////            } else {
////                drawScareCooldown = 0;
////                drawScareEffect = false;
////            }
////        }
////
////        // Draw the meshes (images)
////        for(ObstacleSprite obj : sprites) {
////            obj.draw(batch);
////        }
////
////
////
////
////        drawFearMeter();
////
////
////        if (debug) {
////            // Draw the outlines
////            for (ObstacleSprite obj : sprites) {
////                obj.drawDebug( batch );
////            }
////        }
////
////
////
////        //batch.setColor(foregroundColor);
////        //batch.draw(foregroundTexture, 0, 0, width, height);
////
////
////        batch.end();
////    }
////
////    private void drawUI() {
////        uiCamera.update();
////        batch.setProjectionMatrix(uiCamera.combined);
////        batch.begin();
////        float scaleFactor = 0.5f;
////        float originalWidth = crosshairTexture.getRegionWidth();
////        float originalHeight = crosshairTexture.getRegionHeight();
////        float scaledWidth = originalWidth * scaleFactor;
////        float scaledHeight = originalHeight * scaleFactor;
////
////        float crossX = Gdx.input.getX() - scaledWidth / 2;
////        float crossY = Gdx.graphics.getHeight() - Gdx.input.getY() - scaledHeight / 2;
////
////        batch.draw(crosshairTexture, crossX, crossY, scaledWidth, scaledHeight);
////
////        if (fearMeterTexture != null && avatar != null) {
////            int fearLevel = avatar.getFearMeter();
////            int maxFear = avatar.getMaxFearMeter();
////            float meterWidth = 150 * ((float) fearLevel / maxFear);
////            float meterHeight = 20;
////            float meterX = 20;
////            float meterY = 20;
////
////            float outlineThickness = 2;
////            batch.setColor(Color.BLACK);
////            batch.draw(fearMeterTexture, meterX - outlineThickness, meterY - outlineThickness,
////                    meterWidth + 2 * outlineThickness, meterHeight + 2 * outlineThickness);
////            if (invulnerable) {
////                batch.setColor(Color.GREEN);
////            } else {
////                batch.setColor(Color.RED);
////            }
////
////            batch.draw(fearMeterTexture, meterX, meterY, meterWidth, meterHeight);
////
////            batch.setColor(Color.WHITE);
////
////        }
////
////
////        batch.end();
////
////        // Restore the batch's projection matrix to the main camera (if needed later).
////        batch.setProjectionMatrix(camera.combined);
////    }
////
////    /** Draws Simple fear meter bar */
////    private void drawFearMeter() {
////        int fearLevel = avatar.getFearMeter();
////        int maxFear = avatar.getMaxFearMeter();
////        float meterWidth = 150 * ((float) fearLevel / maxFear);
////        float meterHeight = 20;
////
////        // Use screen coordinates directly.
////        float x = 20; // Offset from the left
////        float y = Gdx.graphics.getHeight() - meterHeight - 20;
////
////        float outlineThickness = 2;
////
////        batch.setColor(Color.BLACK);
////        batch.draw(fearMeterTexture, x - outlineThickness, y - outlineThickness,
////                meterWidth + 2 * outlineThickness, meterHeight + 2 * outlineThickness);
////
////        batch.setColor(Color.RED);
////        batch.draw(fearMeterTexture, x, y, meterWidth, meterHeight);
////        batch.setColor(Color.WHITE);
////    }
////
////    private void drawScareEffect(){
////        float u = avatar.getObstacle().getPhysicsUnits();
////        float size = scareEffectTexture.getRegionWidth() * 0.5f;
////        float size2 = scareEffectTexture.getRegionHeight() * 0.5f;
////
////
////        batch.draw(scareEffectTexture, avatar.getObstacle().getX() * u - size * 0.42f, avatar.getObstacle().getY() * u - size2 *0.57f, size, size2);
////    }
////
////    /**
////     * Called when the Screen is resized.
////     *
////     * This can happen at any point during a non-paused state but will never
////     * happen before a call to show().
////     *
////     * @param width  The new width in pixels
////     * @param height The new height in pixels
////     */
////    public void resize(int width, int height) {
////        this.width  = width;
////        this.height = height;
////        if (camera == null) {
////            camera = new OrthographicCamera();
////        }
////        camera.setToOrtho( false, width, height );
////        scale.x = width/bounds.width;
////        scale.y = height/bounds.height;
////        reset();
////    }
////
////    /**
////     * Called when the Screen should render itself.
////     *
////     * We defer to the other methods update() and draw().  However, it is VERY
////     * important that we only quit AFTER a draw.
////     *
////     * @param delta Number of seconds since last animation frame
////     */
////    public void render(float delta) {
////        if (active) {
////            if (preUpdate(delta)) {
////                update(delta);
////                postUpdate(delta);
////            }
////            draw(delta);
////            if (placementMode) {
////                Vector3 worldMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
////                camera.unproject(worldMouse);
////
////                // Compute the difference between the mouse and camera center.
////                float dx = worldMouse.x - camera.position.x;
////                float dy = worldMouse.y - camera.position.y;
////
////                // Define a dead zone threshold in world units.
////                // For example, 25% of the viewport width/height.
////                float thresholdX = camera.viewportWidth * 0.25f;
////                float thresholdY = camera.viewportHeight * 0.25f;
////
////                float moveX = 0, moveY = 0;
////                // Only move if the mouse is outside the dead zone.
////                if (Math.abs(dx) > thresholdX) {
////                    // Move by the amount beyond the threshold.
////                    moveX = dx - Math.signum(dx) * thresholdX;
////                }
////                if (Math.abs(dy) > thresholdY) {
////                    moveY = dy - Math.signum(dy) * thresholdY;
////                }
////
////                // Apply a speed factor to smooth the movement.
////                float speed = 0.1f;
////                camera.position.x += moveX * speed;
////                camera.position.y += moveY * speed;
////                clampCamera();
////                camera.update();
////            } else {
////                float units = height / bounds.height;
////                // TA feedback was to make it a little less smooth
////                // if the camera is far from being directly over the player, we can try to increase the change
////
////                // 2 / (1 + e^(-5(x-0.5)))
////
////
////
////                Vector3 position = this.camera.position;
////                Vector3 playerPosition = new Vector3(this.avatar.getObstacle().getX() * units, this.avatar.getObstacle().getY() * units, 0);
////
////
////                Vector3 diff = new Vector3(position).sub(playerPosition);
////                float dis = diff.len();
////                if (dis > 220.0) {
////                    diff.nor().scl(250);
////                    position.lerp(new Vector3((playerPosition).add(diff)), 0.1f);
////                }
////
////                float lerp = 3.0f;
////                position.x += (this.avatar.getObstacle().getX() * units - position.x) * lerp * delta;
////                position.y += (this.avatar.getObstacle().getY() * units - position.y) * lerp * delta;
////                camera.position.set(position);
////                camera.zoom = 0.8f;
////                clampCamera();
////                camera.update();
////            }
////            drawUI();
////        }
////    }
////
////    /**
////     * Called when the Screen is resumed from a paused state.
////     *
////     * This is usually when it regains focus.
////     */
////    public void resume() {
////        // TODO Auto-generated method stub
////    }
////
////    /**
////     * Called when this screen becomes the current screen for a Game.
////     */
////    public void show() {
////        active = true;
////    }
////
////    /**
////     * Called when this screen is no longer the current screen for a Game.
////     */
////    public void hide() {
////        active = false;
////    }
////
////    /**
////     * The ScreenListener will respond to requests to quit.
////     */
////    public void setScreenListener(ScreenListener listener) {
////        this.listener = listener;
////    }
////
////    private void toggleInvulnerability() {
////        this.invulnerable = !this.invulnerable;
////    }
////
////    private void createEnemy(String type, Vector2 position) {
////        if (totalEnemies == max_enemies) return;
////        float units = height/bounds.height;
////        System.out.println(units);
////        Texture texture;
////        if (type.equals("critter")) {
////            texture = directory.getEntry("curiosity-critter-active", Texture.class);
////            CuriosityCritter enemy = new CuriosityCritter(units, constants.get("curiosity-critter"), new float[]{position.x, position.y});
////            enemy.setTexture(texture);
////            addSprite(enemy);
////            enemy.createSensor();
////            enemy.createVisionSensor();
////
////            aiManager.register(enemy);
////        } else if (type.equals("maintenance")) {
////            texture = directory.getEntry("mind-maintenance-active", Texture.class);
////            MindMaintenance enemy = new MindMaintenance(units, constants.get("mind-maintenance"), new float[]{position.x, position.y});
////            enemy.setTexture(texture);
////            addSprite(enemy);
////            enemy.createSensor();
////            enemy.createVisionSensor();
////
////            aiManager.register(enemy);
////
////        } else if (type.equals("dreamdweller")) {
////            texture = directory.getEntry("dream-dweller-active", Texture.class);
////            DreamDweller enemy = new DreamDweller(units, constants.get("dream-dweller"), new float[]{position.x, position.y});
////            enemy.setTexture(texture);
////            addSprite(enemy);
////            enemy.createSensor();
////            enemy.createVisionSensor();
////
////            aiManager.register(enemy);
////        }
////        totalEnemies++;
////        System.out.println("Enemy created: " + type + " at " + position);
////    }
////
////
////    public void reset() {
////        JsonValue values = constants.get("world");
////        Vector2 gravity = new Vector2(0, values.getFloat( "gravity" ));
////
////        for(ObstacleSprite sprite : sprites) {
////            sprite.getObstacle().deactivatePhysics(world);
////        }
////        sprites.clear();
////        addQueue.clear();
////        if (world != null) {
////            world.dispose();
////        }
////        totalEnemies = 0;
////
////
////        world = new World(gravity,false);
////        world.setContactListener(this);
////        setComplete(false);
////        setFailure(false);
////        populateLevel();
////    }
////
////    private void populateLevel() {
////        float units = height/bounds.height;
////
////        Texture texture;
////
////
////        texture = directory.getEntry("shared-goal", Texture.class);
////        JsonValue goal = constants.get("goal");
////        JsonValue goalpos = goal.get("pos");
////        totalGoals = goalpos.size;
////        collectedGoals = 0;
////        for (int i = 0; i < goalpos.size; i++) {
////
////            System.out.println("Fetching Goal Positions.");
////            float x = goalpos.get(i).getFloat(0);
////            System.out.println("X.");
////            float y = goalpos.get(i).getFloat(1);
////            System.out.println("Y.");
////
////            Shard goalShard = new Shard(units, goal, x, y, i);
////            goalShard.setTexture(texture);
////            goalShard.getObstacle().setName("goal_" + i);
////            addSprite(goalShard);
////        }
////
////        texture = directory.getEntry( "shared-test", Texture.class );
////        aiManager = new AIControllerManager(avatar, directory, world);
////        Surface wall;
////        String wname = "wall";
////        JsonValue walls = constants.get("walls");
////        JsonValue walljv = walls.get("positions");
////        for (int ii = 0; ii < walljv.size; ii++) {
////            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls, false);
////            wall.getObstacle().setName(wname+ii);
////            wall.setTexture( texture );
////            addSprite(wall);
////        }
////
////
////        Surface platform;
////        String pname = "platform";
////        JsonValue plats = constants.get("platforms");
////        JsonValue platjv = plats.get("positions");
////        for (int ii = 0; ii < platjv.size; ii++) {
////            platform = new Surface(platjv.get(ii).asFloatArray(), units, plats, true);
////            platform.getObstacle().setName(pname+ii);
////            platform.setTexture( texture );
////            addSprite(platform);
////        }
////
////        // Create Player
////        texture = directory.getEntry( "platform-playerSprite", Texture.class );
////        avatar = new Player(units, constants.get("dreamwalker"));
////        avatar.setTexture(texture);
////        addSprite(avatar);
////        // Have to do after body is created
////        avatar.createSensor();
////
////        avatar.createScareSensor();
////
////        aiManager.setPlayer(avatar);
////
////        if (avatar != null && avatar.getObstacle() != null) {
////            camera.position.set(avatar.getObstacle().getX(), avatar.getObstacle().getY(), 0);
////            camera.update();
////        }
////
////    }
////
////    /**
////     * Returns whether to process the update loop
////     *
////     * At the start of the update loop, we check if it is time
////     * to switch to a new game mode. If not, the update proceeds
////     * normally.
////     *
////     * @param dt    Number of seconds since last animation frame
////     *
////     * @return whether to process the update loop
////     */
////    public boolean preUpdate(float dt) {
////
////        InputController input = InputController.getInstance();
////
////        input.setArenaMode(true);
////        input.sync(bounds, scale);
////        if (listener == null) {
////            return true;
////        }
////
////        if (input.didDebug()) {
////            debug = !debug;
////        }
////        if (input.didReset()) {
////            reset();
////        }
////
////        if (input.didExit()) {
////            pause();
////            listener.exitScreen(this, EXIT_QUIT);
////            return false;
////        } else if (input.didAdvance()) {
////            pause();
////            listener.exitScreen(this, EXIT_NEXT);
////            return false;
////        } else if (input.didRetreat()) {
////            pause();
////            listener.exitScreen(this, EXIT_PREV);
////            return false;
////        } else if (countdown > 0) {
////            countdown--;
////        } else if (countdown == 0) {
////            if (failed) {
////                reset();
////            } else if (complete) {
////                pause();
////                listener.exitScreen(this, EXIT_NEXT);
////                return false;
////            }
////        }
////
////
////        if (!isFailure() && (avatar.getObstacle().getY() < -1 || avatar.getFearMeter() == 0)) {
////            setFailure(true);
////            return false;
////        }
////
////        return true;
////    }
////
////    /**
////     * Advances the core gameplay loop of this world.
////     *
////     * This method contains the specific update code for this mini-game. It
////     * does not handle collisions, as those are managed by the parent class
////     * PhysicsScene. This method is called after input is synced to the current
////     * frame, but before collisions are resolved. The very last thing that it
////     * should do is apply forces to the appropriate objects.
////     *
////     * @param dt    Number of seconds since last animation frame
////     */
////    public void update(float dt) {
////        InputController input = InputController.getInstance();
////        timeElapsed += dt;
////        for (Teleporter tp : new ArrayList<>(teleporterCreationTimes.keySet())) {
////            float creationTime = teleporterCreationTimes.get(tp);
////            if(timeElapsed - creationTime >= 2.0f) {
////                tp.getObstacle().markRemoved(true);
////                teleporterCreationTimes.remove(tp);
////            }
////        }
////        if (input.didPressInvulnerability()) toggleInvulnerability();
////        if (input.isSpawningCritter()) {
////            pendingEnemyType = "critter";
////        } else if (input.isSpawningDweller()) {
////            pendingEnemyType = "dreamdweller";
////        } else if (input.isSpawningGuard()) {
////            pendingEnemyType = "maintenance";
////        }
////        if (pendingEnemyType != null) {
////            placementMode = true;
////        }
////        if (placementMode) {
////            System.out.println("placing enemy...");
////            updatePlacementMode();
////            return;
////        }
////
////
////        if (avatar.isGrounded()) {
////            avatar.setJumping(input.didPrimary());
////            avatar.setMovement(input.getHorizontal() *avatar.getForce());
////        } else {
////            avatar.setMovement(input.getHorizontal() *avatar.getForce() * 0.8f);
////        }
////
////        avatar.setStunning(input.didStun());
////        avatar.setHarvesting(input.didSecondary());
////        //avatar.setTeleporting(input.didTeleport());
////        avatar.setTeleporting(input.didM1());
////
////
////        if (avatar.isHarvesting())
////        {
////            drawScareEffect = true;
////            //keeping the one at the time scare
////            if (queuedHarvestedEnemy != null)
////            {
////                if (!queuedHarvestedEnemy.getObstacle().isRemoved()) {
////                    queuedHarvestedEnemy.getObstacle().markRemoved(true);
////                    queuedHarvestedEnemy = null;
////                    avatar.setFearMeter(avatar.getFearMeter() + 3);
////                }
////            } else if (queuedHarvestedEnemyD != null) {
////                if (!queuedHarvestedEnemyD.getObstacle().isRemoved()) {
////                    queuedHarvestedEnemyD.getObstacle().markRemoved(true);
////                    queuedHarvestedEnemyD = null;
////                    avatar.setFearMeter(avatar.getFearMeter() + 5);
////                }
////            }
////        }
////
////
////
////        // Add a bullet if we fire
////        if (avatar.isStunning() && avatar.getFearMeter() > STUN_COST) {
////            createBullet();
////            avatar.setFearMeter(Math.max(0,avatar.getFearMeter() - 1));
////        }
////
////        if (avatar.isTeleporting())
////        {
////            System.out.println("hi");
////            createTeleporter();
////        }
////        if (input.didTakeTeleport() && currentTeleporter != null && avatar.getFearMeter() > TELEPORT_COST) {
////            takeTeleporter(currentTeleporter);
////            currentTeleporter = null;
////        }
////
////
////        if (queuedTeleportPosition != null) {
////            avatar.getObstacle().setPosition(queuedTeleportPosition);
////            avatar.setFearMeter(Math.max(0,avatar.getFearMeter() - 1));
////            queuedTeleportPosition = null; // Clear after applying
////        }
////
//////        System.out.println("critter pos" + critter.getObstacle().getPosition());
//////        System.out.println("avatar pos" + avatar.getObstacle().getPosition());
////        aiManager.update(dt);
////
////
////        avatar.applyForce(world);
////        if (avatar.isJumping()) {
////            SoundEffectManager sounds = SoundEffectManager.getInstance();
////            //sounds.play("jump", jumpSound, volume);
////        }
////        if (invulnerable) avatar.setFearMeter(avatar.getMaxFearMeter());
////    }
////
////    private void updatePlacementMode() {
////        avatar.setTeleporting(false);
////        InputController input = InputController.getInstance();
////        if (input.didM1()) {
////            float units = height / bounds.height;
////            System.out.println("Mouse position: " + input.getMouse());
////            Vector3 temp = camera.unproject(new Vector3(input.getMouse().x, input.getMouse().y, 0));
////            Vector2 spawnPos = new Vector2(temp.x / units, temp.y / units);
////            System.out.println("Unprojected position: " + spawnPos);
////            if (isValidSpawnPosition(spawnPos)) {
////                createEnemy(pendingEnemyType, spawnPos);
////            } else {
////                System.out.println("Invalid spawn position: can not be outside bounds or inside physical platforms");
////            }
////            pendingEnemyType = null;
////            placementMode = false;
////        }
////    }
////    private boolean isValidSpawnPosition(Vector2 pos) {
////
////        return true;
////    }
////
////    private void clampCamera() {
////        float units = height / bounds.height; // conversion factor: pixels per physics unit
////        float halfViewportWidth = camera.viewportWidth / 2;
////        float halfViewportHeight = camera.viewportHeight / 2;
////
////        // Convert world bounds to screen coordinates
////        float minX = bounds.x * units + halfViewportWidth;
////        float maxX = (bounds.x + bounds.width) * units - halfViewportWidth;
////        float minY = bounds.y * units + halfViewportHeight;
////        float maxY = (bounds.y + bounds.height) * units - halfViewportHeight;
////
////        camera.position.x = MathUtils.clamp(camera.position.x, minX-100, maxX+100);
////        camera.position.y = MathUtils.clamp(camera.position.y, minY-50, maxY+50);
////    }
////
////
////    private void createTeleporter() {
////        InputController input = InputController.getInstance();
////        float units = height / bounds.height;
////
////        Vector2 playerPosition = avatar.getObstacle().getPosition();
////
////        // Get crosshair position in screen coordinates
////        Vector2 crosshairScreen = input.getMouse();
////
////        // Unproject the crosshair screen position to get world coordinates
////        Vector3 crosshairTemp = new Vector3(
////                crosshairScreen.x,
////                crosshairScreen.y,
////                0
////        );
////        camera.unproject(crosshairTemp);
////        Vector2 crosshairWorld = new Vector2(crosshairTemp.x / units, crosshairTemp.y / units);
////
////        // Check if mouse is on a surface (from second function)
////        final boolean[] isOnSurface = {false};
////        world.QueryAABB(new QueryCallback() {
////                            @Override
////                            public boolean reportFixture(Fixture fixture) {
////                                Object userData = fixture.getBody().getUserData();
////                                if (userData instanceof Surface) {
////                                    if (fixture.testPoint(crosshairWorld)) {
////                                        isOnSurface[0] = true;
////                                        return false; // Stop the query
////                                    }
////                                }
////                                return true; // Continue the query
////                            }
////                        }, crosshairWorld.x - 0.1f, crosshairWorld.y - 0.1f,
////                crosshairWorld.x + 0.1f, crosshairWorld.y + 0.1f);
////
////        if (isOnSurface[0]) {
////            System.out.println("Cannot place teleporter: Mouse is directly on a surface!");
////            return;
////        }
////
////        // Use raycast to find platform below cursor (from second function)
////        Vector2 rayStart = new Vector2(crosshairWorld.x, crosshairWorld.y);
////        Vector2 rayEnd = new Vector2(crosshairWorld.x, 0);
////
////
////        PlatformRayCast callback = new PlatformRayCast();
////        world.rayCast(callback, rayStart, rayEnd);
////
////        if (callback.getPlatformFixture() == null) {
////            System.out.println("Platform not found below cursor");
////            return;
////        }
////
////        Surface platform = (Surface)callback.getPlatformFixture().getBody().getUserData();
////        Vector2 hitPoint = callback.getHitPoint();
////        Vector2 initialPosition = new Vector2(crosshairWorld.x, hitPoint.y + 0.75f);
////
////        // Position the teleporter above the platform
////        Vector2 teleporterPosition = new Vector2(crosshairWorld.x, hitPoint.y + 0.75f);
////
////        // Calculate distance in world coordinates
////        float worldDistance = playerPosition.dst(teleporterPosition);
////        float maxDistance = avatar.getTeleportRangeRadius() / units; // In world units
////
////        // Check if destination is within range
////        if (worldDistance > maxDistance) {
////            Vector2 direction = new Vector2(
////                    initialPosition.x - playerPosition.x,
////                    initialPosition.y - playerPosition.y
////            ).nor();
////
////            // Clamp position to max distance
////            teleporterPosition = new Vector2(
////                    playerPosition.x + direction.x * maxDistance,
////                    playerPosition.y + direction.y * maxDistance
////            );
////
////            // Use new raycast to find platform below the clamped position
////            rayStart = new Vector2(teleporterPosition.x, teleporterPosition.y);
////            rayEnd = new Vector2(teleporterPosition.x, 0);
////
////            PlatformRayCast clampedCallback = new PlatformRayCast();
////            world.rayCast(clampedCallback, rayStart, rayEnd);
////
////            if (clampedCallback.getPlatformFixture() != null) {
////                Vector2 clampedHitPoint = clampedCallback.getHitPoint();
////                teleporterPosition.y = clampedHitPoint.y + 0.75f;
////            } else {
////                System.out.println("No platform found at clamped position");
////                return;
////            }
////        }
////
////        // Create the teleporters at the correct world positions
////        Texture texture = directory.getEntry("platform-teleporter", Texture.class);
////        JsonValue teleporter = constants.get("teleporter");
////
////        Teleporter originTeleporter = new Teleporter(units, teleporter, playerPosition);
////        originTeleporter.setTexture(texture);
////        originTeleporter.getObstacle().setName("origin_teleporter");
////
////        Teleporter exitTeleporter = new Teleporter(units, teleporter, teleporterPosition);
////        exitTeleporter.setTexture(texture);
////        exitTeleporter.getObstacle().setName("exit_teleporter");
////
////        originTeleporter.setLinkedTeleporter(exitTeleporter);
////        exitTeleporter.setLinkedTeleporter(originTeleporter);
////
////        teleporterCreationTimes.put(originTeleporter, timeElapsed);
////        teleporterCreationTimes.put(exitTeleporter, timeElapsed);
////
////        addSprite(originTeleporter);
////        addSprite(exitTeleporter);
////
////        // Reduce fear meter after placing the teleporter
////        avatar.setFearMeter(Math.max(0, avatar.getFearMeter() - 2));
////    }
////
////    private void takeTeleporter(Teleporter tp)
////    {
////        System.out.println(tp.getLinkedTeleporter().getPosition());
////        //avatar.getObstacle().setPosition(tp.getLinkedTeleporter().getPosition());
////        queuedTeleportPosition = tp.getLinkedTeleporter().getPosition().cpy();
////
////    }
////
////
////    private void performHarvest(CuriosityCritter enemy)
////    {
////        queuedHarvestedEnemy = enemy;
////    }
////
////    private void performHarvestD(DreamDweller enemy)
////    {
////        queuedHarvestedEnemyD = enemy;
////    }
////
////
////
////
////    /**
////     * Adds a new bullet to the world and send it in the right direction.
////     */
////    private void createBullet() {
////        InputController input = InputController.getInstance();
////
////        float units = height/bounds.height;
////        Vector2 mousePosition = input.getCrossHair();
////        JsonValue bulletjv = constants.get("bullet");
////        Obstacle player = avatar.getObstacle();
////        Vector2 shootAngle = mousePosition.sub(player.getPosition());
////        shootAngle.nor();
////        Texture texture = directory.getEntry("platform-bullet", Texture.class);
////        Bullet bullet = new Bullet(units, bulletjv, player.getPosition(), shootAngle.nor());
////        bullet.setTexture(texture);
////        addQueuedObject(bullet);
////
////        SoundEffectManager sounds = SoundEffectManager.getInstance();
////
////    }
////
////    /**
////     * Removes a new bullet from the world.
////     *
////     * @param  bullet   the bullet to remove
////     */
////    public void removeBullet(ObstacleSprite bullet) {
////        bullet.getObstacle().markRemoved(true);
////        SoundEffectManager sounds = SoundEffectManager.getInstance();
////
////    }
////
////
////    /**
////     * Callback method for the start of a collision
////     *
////     * This method is called when we first get a collision between two objects.
////     * We use this method to test if it is the "right" kind of collision. In
////     * particular, we use it to test if we made it to the win door.
////     *
////     * @param contact The two bodies that collided
////     */
////    public void beginContact(Contact contact) {
////        Fixture fix1 = contact.getFixtureA();
////        Fixture fix2 = contact.getFixtureB();
////
////        Body body1 = fix1.getBody();
////        Body body2 = fix2.getBody();
////
////        Object fd1 = fix1.getUserData();
////        Object fd2 = fix2.getUserData();
////
////        Object bodyDataA = fix1.getBody().getUserData();
////        Object bodyDataB = fix2.getBody().getUserData();
////
////        try {
////            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
////            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();
////            // Check for win condition
////            if ((bd1 == avatar && bd2 instanceof Shard) || (bd2 == avatar && bd1 instanceof Shard)) {
////                Shard collectedShard = (bd1 instanceof Shard) ? (Shard) bd1 : (Shard) bd2;
////
////                if (!collectedShard.getObstacle().isRemoved()) {
////                    collectedShard.getObstacle().markRemoved(true);
////                    collectedGoals++;
////
////
////                    if (collectedGoals == totalGoals) {
////                        setComplete(true);
////                    }
////                }
////            }
////
////            // Check if an enemy's walk sensor detects a wall or another enemy
////            if (("walk_sensor".equals(fd1) && (bd2 instanceof Surface || bd2 instanceof Enemy)) ||
////                    ("walk_sensor".equals(fd2) && (bd2 instanceof Surface || bd2 instanceof Enemy))) {
////                System.out.println("walk_sensor collision detected with: " + bd1 + " and " + bd2);
////
////
////                // Ensure the Enemy reference is correctly retrieved
////                Enemy e = (bd1 instanceof Enemy) ? (Enemy) bd1
////                        : (bd2 instanceof Enemy) ? (Enemy) bd2
////                        : null;
////
////                if (e != null) {
////                    e.setSeesWall(true);
////                    System.out.println("Enemy sees wall");
////                } else {
////                    System.out.println("WARNING: Walk sensor collision detected but Enemy reference is null.");
////                }
////            }
////
////            // If there is a collision between a vision sensor and the player
////            if ( ("vision_sensor".equals(fd1) || "vision_sensor".equals(fd2))
////                    && (bodyDataA instanceof Player || bodyDataB instanceof Player) ) {
////
////                // Check if the vision sensor belongs to an "un-stunned" enemy, and if
////                // so update the enemy's awareness and apply damage to player
////                if (bodyDataA instanceof Enemy && !((Enemy) bodyDataA).isStunned() ) {
////                    ((Enemy) bodyDataA).setAwareOfPlayer(true);
////                    System.out.println(bodyDataA.getClass() + " saw player!");
////                    avatar.setTakingDamage(true);
////                } else if ( bodyDataB instanceof Enemy && !((Enemy) bodyDataB).isStunned() )  {
////                    System.out.println(bodyDataB.getClass() + " saw player!");
////                    ((Enemy) bodyDataB).setAwareOfPlayer(true);
////                    avatar.setTakingDamage(true);
////
////                }
////                // The player should always take damage when they are detected by a vision sensor
////                // not associated with an enemy (e.g. lamp)
////                else{
////                    avatar.setTakingDamage(true);
////
////                }
////
////            }
////            if ("dweller_vision_sensor".equals(fd1) || "dweller_vision_sensor".equals(fd2)) {
////                bodyDataA = fix1.getBody().getUserData();
////                bodyDataB = fix2.getBody().getUserData();
////
////                DreamDweller dweller = null;
////                Player playerObj = null;
////
////                if (bodyDataA instanceof DreamDweller && bodyDataB instanceof Player) {
////                    dweller = (DreamDweller) bodyDataA;
////                    playerObj = (Player) bodyDataB;
////                } else if (bodyDataA instanceof Player && bodyDataB instanceof DreamDweller) {
////                    dweller = (DreamDweller) bodyDataB;
////                    playerObj = (Player) bodyDataA;
////                }
////
////                if (dweller != null) {
////                    dweller.setAwareOfPlayer(true);
////                    System.out.println("Dream Dweller saw player");
////                    avatar.setTakingDamage(true);
////
////                }
////
////
////
////            }
////
////
////            // Test bullet collision with world
////            if (bd1.getName().equals("bullet") && bd2 != avatar && !(bd2 instanceof Shard)) {
////                // if it hits a curiosity critter
////                if (bd2 instanceof CuriosityCritter){
////                    // make sure it hits the body of the critter
////                    if(fd2 != "walk_sensor" && fd2 != "vision_sensor" && fd2 != "follow_sensor") {
////                        removeBullet(bd1);
////                        CuriosityCritter critter =
////                                (bd1 instanceof CuriosityCritter) ? (CuriosityCritter) bd1
////                                        : (bd2 instanceof CuriosityCritter) ? (CuriosityCritter) bd2
////                                        : null;
////                        System.out.println("Obstacle type: " + critter.getObstacle().getName());
////                        if (critter != null) {
////                            critter.setStunned(true);
////                            Texture texture = directory.getEntry( "curiosity-critter-inactive", Texture.class );
////                            critter.setTexture(texture);
////                            System.out.println("Critter is stunned");
////                        } else {
////                            System.out.println(
////                                    "WARNING: Bullet stun collision detected but Critter reference is null.");
////                        }
////                    }
////                    else if (bd2 instanceof MindMaintenance) {
////                        // make sure it hits the body of the critter
////                        if (fd2 != "walk_sensor" && fd2 != "vision_sensor"
////                                && fd2 != "follow_sensor") {
////                            removeBullet(bd1);
////                            MindMaintenance maintenance =
////                                    (bd1 instanceof MindMaintenance) ? (MindMaintenance) bd1
////                                            : (bd2 instanceof MindMaintenance) ? (MindMaintenance) bd2
////                                            : null;
////
////                            if (maintenance != null) {
////                                maintenance.setStunned(true);
////                                Texture texture = directory.getEntry("mind-maintenance-inactive",
////                                        Texture.class);
////                                maintenance.setTexture(texture);
////                                System.out.println("Maintenance is stunned");
////                            } else {
////                                System.out.println(
////                                        "WARNING: Bullet stun collision detected but Maintenance reference is null.");
////                            }
////                        }
////                    }
////                }
////                else if (bd2 instanceof DreamDweller) {
////                    // make sure it hits the body of the critter
////                    if (fd2 != "walk_sensor" && fd2 != "dweller_vision_sensor"
////                            && fd2 != "dweller_alert_sensor") {
////                        removeBullet(bd1);
////                        DreamDweller dweller =
////                                (bd1 instanceof DreamDweller) ? (DreamDweller) bd1
////                                        : (bd2 instanceof DreamDweller) ? (DreamDweller) bd2
////                                        : null;
////                        if (dweller != null) {
////                            dweller.setStunned(true);
////                            Texture texture = directory.getEntry("dream-dweller-inactive",
////                                    Texture.class);
////                            dweller.setTexture(texture);
////                            System.out.println("Dreamdweller is stunned");
////                        } else {
////                            System.out.println(
////                                    "WARNING: Bullet stun collision detected but Dweller reference is null.");
////                        }
////                    }
////
////                }
////                //otherwise the bullet hits a non-enemy and should be removed
////                else {
////                    removeBullet(bd1);
////                }
////
////            }
////
////            if (bd2.getName().equals("bullet") && bd1 != avatar && !(bd1 instanceof Shard)) {
////                // if it hits a curiosity critter
////                if (bd1 instanceof CuriosityCritter){
////                    // make sure it hits the body of the critter
////                    if(fd1 != "walk_sensor" && fd1 != "vision_sensor" && fd1 != "follow_sensor") {
////                        removeBullet(bd2);
////                        CuriosityCritter critter =
////                                (bd2 instanceof CuriosityCritter) ? (CuriosityCritter) bd2
////                                        : (bd1 instanceof CuriosityCritter) ? (CuriosityCritter) bd1
////                                        : null;
////                        System.out.println("Obstacle type: " + critter.getObstacle().getName());
////                        if (critter != null) {
////                            critter.setStunned(true);
////                            Texture texture = directory.getEntry( "curiosity-critter-inactive", Texture.class );
////                            critter.setTexture(texture);
////                            System.out.println("Critter is stunned");
////                        } else {
////                            System.out.println(
////                                    "WARNING: Bullet stun collision detected but Critter reference is null.");
////                        }
////                    }
////                }
////                else if (bd1 instanceof MindMaintenance) {
////                    // make sure it hits the body of the critter
////                    if (fd1 != "walk_sensor" && fd1 != "vision_sensor"
////                            && fd1 != "follow_sensor") {
////                        removeBullet(bd2);
////                        MindMaintenance maintenance =
////                                (bd2 instanceof MindMaintenance) ? (MindMaintenance) bd2
////                                        : (bd1 instanceof MindMaintenance) ? (MindMaintenance) bd1
////                                        : null;
////                        if (maintenance != null) {
////                            maintenance.setStunned(true);
////                            Texture texture = directory.getEntry("mind-maintenance-inactive",
////                                    Texture.class);
////                            maintenance.setTexture(texture);
////                            System.out.println("Maintenance is stunned");
////                        } else {
////                            System.out.println(
////                                    "WARNING: Bullet stun collision detected but Maintenance reference is null.");
////                        }
////                    }
////                }
////                else if (bd1 instanceof DreamDweller) {
////                    // make sure it hits the body of the critter
////                    if (fd1 != "walk_sensor" && fd1 != "dweller_vision_sensor"
////                            && fd1 != "dweller_alert_sensor") {
////                        removeBullet(bd2);
////                        DreamDweller dweller =
////                                (bd2 instanceof DreamDweller) ? (DreamDweller) bd2
////                                        : (bd1 instanceof DreamDweller) ? (DreamDweller) bd1
////                                        : null;
////                        if (dweller != null) {
////                            dweller.setStunned(true);
////                            Texture texture = directory.getEntry("dream-dweller-inactive",
////                                    Texture.class);
////                            dweller.setTexture(texture);
////                            System.out.println("Dweller is stunned");
////                        } else {
////                            System.out.println(
////                                    "WARNING: Bullet stun collision detected but Dweller reference is null.");
////                        }
////                    }
////                }
////                //otherwise the bullet hits a non-enemy and should be removed
////                else {
////                    removeBullet(bd2);
////                }
////
////            }
////
////
////            // See if we have landed on the ground.
////            if ((avatar.getSensorName().equals(fd2) && bd1 instanceof Surface) ||
////                    (avatar.getSensorName().equals(fd1) && bd2 instanceof Surface)) {
////                avatar.setGrounded(true);
////                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
////
////                Surface currentSurface;
////                if (bd1 instanceof Surface)
////                {
////                    currentSurface = (Surface) bd1;
////                } else {
////                    currentSurface = (Surface) bd2;
////                }
////
////                if (currentSurface.isShadowed())
////                {
////                    avatar.setIsShadow(true);
////                    shadowSensorFixtures.add(avatar == bd1 ? fix2 : fix1);
////                }
////            }
////
////            System.out.println(fd1 + " " +  fd2);
////
////
////            if((avatar.getScareSensorName().equals(fd1) && fd2.equals("harvest_sensor") ||
////                    (avatar.getScareSensorName().equals(fd2) && fd1.equals("harvest_sensor"))))
////            {
////                CuriosityCritter harvestedCC;
////                if (avatar.getScareSensorName().equals(fd1))
////                {
////                    harvestedCC = (CuriosityCritter) bd2;
////                    performHarvest(harvestedCC);
////                } else if (avatar.getScareSensorName().equals(fd2))
////                {
////                    harvestedCC = (CuriosityCritter) bd1;
////                    performHarvest(harvestedCC);
////                }
////                avatar.setHarvesting(true);
////
////            }
////
////            if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof DreamDweller)) ||
////                    (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof DreamDweller)))
////            {
////                DreamDweller harvested;
////                if (avatar.getScareSensorName().equals(fd1))
////                {
////                    harvested = (DreamDweller) bd2;
////                    performHarvestD(harvested);
////                } else if (avatar.getScareSensorName().equals(fd2))
////                {
////                    harvested = (DreamDweller) bd1;
////                    performHarvestD(harvested);
////                }
////                avatar.setHarvesting(true);
////            }
////
////            if( !avatar.getScareSensorName().equals(fd1) && bd1 == avatar && bd2.getName().equals("origin_teleporter"))
////            {
////                if (!(bd2 instanceof Teleporter)) {
////                    System.out.println("Error: bd2 is not a Teleporter!");
////                } else {
////
////                    currentTeleporter = (Teleporter) bd2;
////                    takeTeleporter(currentTeleporter);
////                }
////            } else if (bd1.getName().equals("origin_teleporter") && bd2 == avatar){
////                if (!(bd1 instanceof Teleporter)) {
////                    System.out.println("Error: bd2 is not a Teleporter!");
////                } else {
////                    currentTeleporter = (Teleporter) bd1;
////                    takeTeleporter(currentTeleporter);
////                }
////            }
////
////
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////
////    }
////
////    /**
////     * Callback method for the start of a collision
////     *
////     * This method is called when two objects cease to touch. The main use of
////     * this method is to determine when the characer is NOT on the ground. This
////     * is how we prevent double jumping.
////     */
////    public void endContact(Contact contact) {
////        Fixture fix1 = contact.getFixtureA();
////        Fixture fix2 = contact.getFixtureB();
////
////        Body body1 = fix1.getBody();
////        Body body2 = fix2.getBody();
////
////        Object fd1 = fix1.getUserData();
////        Object fd2 = fix2.getUserData();
////
////        Object bd1 = body1.getUserData();
////        Object bd2 = body2.getUserData();
////
////        Object bodyDataA = fix1.getBody().getUserData();
////        Object bodyDataB = fix2.getBody().getUserData();
////
////        if (("walk_sensor".equals(fd1) && bd2 instanceof Surface) ||
////                ("walk_sensor".equals(fd2) && bd1 instanceof Surface)) {
////
////
////            CuriosityCritter critter = (bd1 instanceof CuriosityCritter) ? (CuriosityCritter) bd1
////                    : (bd2 instanceof CuriosityCritter) ? (CuriosityCritter) bd2
////                    : null;
////
////            if (critter != null) {
////                critter.setSeesWall(false);
////                System.out.println("Critter stopped seeing wall");
////            } else {
////                System.out.println("WARNING: Walk sensor end contact detected but Critter reference is null.");
////            }
////        }
////
////
////        if (("follow_sensor".equals(fd1) || "follow_sensor".equals(fd2)) && (bodyDataA instanceof Player || bodyDataB instanceof Player)) {
////
////
////            if (bodyDataA instanceof Enemy) {
////                ((Enemy) bodyDataA).setAwareOfPlayer(false);
////            } else if (bodyDataA instanceof Player && bodyDataB instanceof CuriosityCritter) {
////                ((Enemy) bodyDataB).setAwareOfPlayer(false);
////            }
////            else if (bodyDataA instanceof Player && bodyDataB instanceof DreamDweller) {
////                ((Enemy) bodyDataB).setAwareOfPlayer(false);
////            }
////
////            avatar.setTakingDamage(false);
////            System.out.println("Enemy stopped seeing player");
////        }
////        /*
////        if (("follow_sensor".equals(fd1) || "follow_sensor".equals(fd2))) {
////
////            CuriosityCritter critter = null;
////            Player playerObj = null;
////            if (bodyDataA instanceof CuriosityCritter && bodyDataB instanceof Player) {
////                critter = (CuriosityCritter) bodyDataA;
////                playerObj = (Player) bodyDataB;
////            } else if (bodyDataA instanceof Player && bodyDataB instanceof CuriosityCritter) {
////                critter = (CuriosityCritter) bodyDataB;
////                playerObj = (Player) bodyDataA;
////            }
////
////            if (critter != null) {
////                 critter.setAwareOfPlayer(false);
////                 avatar.setTakingDamage(false);
////                 System.out.println("Critter stopped seeing player");
////            }
////        }*/
////
////        if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof CuriosityCritter)) ||
////                (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof CuriosityCritter)))
////        {
////
////
////        }
////
////
////        if ((bd1 instanceof Teleporter && bd2 == avatar) || (bd1 == avatar && bd2 instanceof Teleporter)) {
////            currentTeleporter = null;
////            System.out.println("Player moved away from teleporter");
////        }
////
////        if ("dweller_vision_sensor".equals(fd1) || "dweller_vision_sensor".equals(fd2)) {
////            bodyDataA = fix1.getBody().getUserData();
////            bodyDataB = fix2.getBody().getUserData();
////
////            DreamDweller dweller = null;
////            Player playerObj = null;
////
////            if (bodyDataA instanceof DreamDweller && bodyDataB instanceof Player) {
////                dweller = (DreamDweller) bodyDataA;
////                playerObj = (Player) bodyDataB;
////            } else if (bodyDataA instanceof Player && bodyDataB instanceof DreamDweller) {
////                dweller = (DreamDweller) bodyDataB;
////                playerObj = (Player) bodyDataA;
////            }
////
////            if (dweller != null) {
////
////                avatar.setTakingDamage(false);
////                System.out.println("Dream Dweller lost sight of player");
////            }
////        }
////
////        if((avatar.getScareSensorName().equals(fd1) && (bd2 instanceof DreamDweller)) ||
////                (avatar.getScareSensorName().equals(fd2) && (bd1 instanceof DreamDweller)))
////        {
////
////        }
////
////
////
////
////        if ((avatar.getSensorName().equals(fd2) && bd1 instanceof Surface) ||
////                (avatar.getSensorName().equals(fd1) && bd2 instanceof Surface)) {
////            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
////            if (sensorFixtures.size == 0) {
////                avatar.setGrounded(false);
////            }
////            shadowSensorFixtures.remove(avatar == bd1? fix2 : fix1);
////            if (shadowSensorFixtures.size == 0){
////                avatar.setIsShadow(false);
////            }
////        }
////    }
////
////    /** Unused ContactListener method */
////    public void postSolve(Contact contact, ContactImpulse impulse) {}
////    /** Unused ContactListener method */
////    public void preSolve(Contact contact, Manifold oldManifold) {}
////
////    /**
////     * Called when the Screen is paused.
////     *
////     * We need this method to stop all sounds when we pause.
////     * Pausing happens when we switch game modes.
////     */
////    public void pause() {
////        SoundEffectManager sounds = SoundEffectManager.getInstance();
////        sounds.stop("plop");
////        sounds.stop("fire");
////        sounds.stop("jump");
////    }
//}
