package edu.cornell.cis3152.physics.platform;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * Perhaps not named that cleverly - these spikes are environmental hazards to the Dreamwalker
 * with particular capabilities
 */
public class DayglowSpike extends ObstacleSprite {

    public enum Direction {
        UP, RIGHT, DOWN, LEFT
    }

    private boolean isLaser;
    private boolean active;
    private Direction facing;

    private boolean laserCharging;
    private boolean laserFiring;
    private int chargeCounter;
    private int firingCounter;
    private static final int LASER_CHARGE_FRAMES = 60;
    private static final int LASER_DURATION_FRAMES = 120;
    private Fixture laserBeam;
    private static final float LASER_BEAM_LENGTH = 30f; // Length of the laser beam
    private static final Color LASER_COLOR = Color.RED;
    private static final Color LASER_CHARGING_COLOR = Color.YELLOW;
    private static final Color LASER_DEBUG_RAY_COLOR = Color.MAGENTA;
    private Vector2 debugRayStart, debugRayEnd;
    private Vector2 rayStart, rayEnd;

    private float units;
    private World world;

    public DayglowSpike(float units, float x, float y, float width, float height, boolean isLaser, Direction facing) {
        super();
        active = true;

        this.facing = facing;
        this.units = units;

        obstacle = new BoxObstacle(x + width / 2f, y + height / 2f, width, height);
        obstacle.setPhysicsUnits(units);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setUserData(this);
        obstacle.setName("spike");
        obstacle.setSensor(true);

        debug = Color.LIME;
        mesh.set(0, 0, width * units, height * units);
        this.isLaser = isLaser;
        this.laserCharging = false;
        this.laserFiring = false;
        this.chargeCounter = 0;
        this.firingCounter = 0;
    }

    public boolean isActive() {
        return active;
    }

    public Direction getFacing() {
        return facing;
    }

    /**
     * Configure collision filtering so only the player category
     * will ever trigger this sensor.
     */
    public void setFilter() {
        for (Fixture f : getObstacle().getBody().getFixtureList()) {
            Filter filter = f.getFilterData();
            filter.categoryBits = CollisionFiltering.CATEGORY_SCENERY;
            filter.maskBits     = CollisionFiltering.CATEGORY_PLAYER;
            f.setFilterData(filter);
        }
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    /** Set the world reference for laser functionality */
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public void update(float dt) {
        if (!isLaser || !active || world == null) return;

        if (!laserCharging && !laserFiring && detectPlayer()) {
            // Start charging the laser
            laserCharging = true;
            chargeCounter = 0;
        }

        if (laserCharging) {
            chargeCounter++;
            if (chargeCounter >= LASER_CHARGE_FRAMES) {
                // Charge complete, fire the laser
                laserCharging = false;
                fireLaser();
                firingCounter = 0;
            }
        }

        if (laserFiring) {
            firingCounter++;
            if (firingCounter >= LASER_DURATION_FRAMES) {
                // Laser firing complete
                deactivateLaser();
            }
        }
        super.update(dt);
    }

    /**
     * Creates the laser beam fixture
     */
    private void fireLaser() {
        laserFiring = true;

        // Create the laser beam as a sensor fixture
        if (laserBeam != null) {
            obstacle.getBody().destroyFixture(laserBeam);
        }

        Vector2 position = obstacle.getPosition();
        PolygonShape shape = new PolygonShape();

        float laserWidth = 0.5f; // Width of the laser beam
        float halfLaserWidth = laserWidth / 2;

        // Calculate the laser beam vertices based on direction
        Vector2[] vertices = new Vector2[4];

        switch (facing) {
            case UP:
                vertices[0] = new Vector2(-halfLaserWidth, 0);
                vertices[1] = new Vector2(halfLaserWidth, 0);
                vertices[2] = new Vector2(halfLaserWidth, LASER_BEAM_LENGTH);
                vertices[3] = new Vector2(-halfLaserWidth, LASER_BEAM_LENGTH);
                break;
            case DOWN:
                vertices[0] = new Vector2(-halfLaserWidth, 0);
                vertices[1] = new Vector2(halfLaserWidth, 0);
                vertices[2] = new Vector2(halfLaserWidth, -LASER_BEAM_LENGTH);
                vertices[3] = new Vector2(-halfLaserWidth, -LASER_BEAM_LENGTH);
                break;
            case RIGHT:
                vertices[0] = new Vector2(0, -halfLaserWidth);
                vertices[1] = new Vector2(0, halfLaserWidth);
                vertices[2] = new Vector2(LASER_BEAM_LENGTH, halfLaserWidth);
                vertices[3] = new Vector2(LASER_BEAM_LENGTH, -halfLaserWidth);
                break;
            case LEFT:
                vertices[0] = new Vector2(0, -halfLaserWidth);
                vertices[1] = new Vector2(0, halfLaserWidth);
                vertices[2] = new Vector2(-LASER_BEAM_LENGTH, halfLaserWidth);
                vertices[3] = new Vector2(-LASER_BEAM_LENGTH, -halfLaserWidth);
                break;
        }

        shape.set(vertices);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;

        // Set collision filtering
        Filter filter = new Filter();
        filter.categoryBits = CollisionFiltering.CATEGORY_SCENERY;
        filter.maskBits = CollisionFiltering.CATEGORY_PLAYER;
        fixtureDef.filter.set(filter);

        laserBeam = obstacle.getBody().createFixture(fixtureDef);
        laserBeam.setUserData(this); // So we can identify this as a laser beam in collisions

        shape.dispose();
    }

    /**
     * Deactivates the laser beam by removing the fixture
     */
    private void deactivateLaser() {
        laserCharging = false;
        laserFiring = false;
        chargeCounter = 0;
        firingCounter = 0;

        if (laserBeam != null && obstacle.getBody() != null) {
            obstacle.getBody().destroyFixture(laserBeam);
            laserBeam = null;
        }
    }

    private boolean detectPlayer() {
        if (world == null) return false;

        final boolean[] playerDetected = {false};

        Vector2 position = obstacle.getPosition();
        debugRayStart = position;
        Vector2 rayEnd = new Vector2();


        // Calculate ray direction based on facing
        switch (facing) {
            case UP:
                rayEnd.set(position.x, position.y + LASER_BEAM_LENGTH);
                break;
            case DOWN:
                rayEnd.set(position.x, position.y - LASER_BEAM_LENGTH);
                break;
            case RIGHT:
                rayEnd.set(position.x + LASER_BEAM_LENGTH, position.y);
                break;
            case LEFT:
                rayEnd.set(position.x - LASER_BEAM_LENGTH, position.y);
                break;
        }

        debugRayEnd = rayEnd;

        // Perform raycast to check for player
        world.rayCast(new RayCastCallback() {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
                if (fixture.isSensor()) return 1;
                Object userData = fixture.getBody().getUserData();
                if (fixture.getBody().getUserData() instanceof Surface) {
                    //stop looking, don't really need to do much with the surface
                    return 0;
                } else if (fixture.getBody().getUserData() instanceof Player) {
                    Player player = (Player) userData;
                    playerDetected[0] = true;
                    return 0;
                }
                return fraction; // Continue the raycast
            }
        }, position, rayEnd);

        return playerDetected[0];
    }

    public boolean isLaserFiring() {
        return isLaser && laserFiring;
    }

    /**
     * Check if this is currently charging a laser
     */
    public boolean isLaserCharging() {
        return isLaser && laserCharging;
    }

    @Override
    public void drawDebug(SpriteBatch batch) {
        super.drawDebug(batch);
        drawRayDebug(batch);
        if (!isLaser) { return; }

        batch.setTexture(Texture2D.getBlank());
        float u = obstacle.getPhysicsUnits();

        // Build world transform
        float angleDeg = (float)(obstacle.getAngle() * 180.0 / Math.PI);
        transform.idt();
        transform.preRotate(angleDeg);
        transform.preTranslate(obstacle.getPosition().x * u,
            obstacle.getPosition().y * u);

        PathFactory factory = new PathFactory();
        Path2 shape = new Path2();

        // Charging: single line
        if (laserCharging) {
            batch.setColor(LASER_CHARGING_COLOR);
            float dx = 0, dy = 0;
            switch (facing) {
                case UP:    dy = LASER_BEAM_LENGTH * u; break;
                case DOWN:  dy = -LASER_BEAM_LENGTH * u; break;
                case RIGHT: dx = LASER_BEAM_LENGTH * u; break;
                case LEFT:  dx = -LASER_BEAM_LENGTH * u; break;
            }
            factory.makeLine(0, 0, dx, dy, shape);
            batch.outline(shape, transform);
        }

        // Firing: full rectangle via makeRect
        if (laserFiring) {
            batch.setColor(LASER_COLOR);
            float beamW = 0.5f * u;                // width in world units
            float beamL = LASER_BEAM_LENGTH * u;   // length in world units

            // clear and build the rectangle in local coords
            shape.clear();
            switch (facing) {
                case UP:
                    factory.makeRect(-beamW/2, 0, beamW, beamL, shape);
                    break;
                case DOWN:
                    factory.makeRect(-beamW/2, -beamL, beamW, beamL, shape);
                    break;
                case RIGHT:
                    factory.makeRect(0, -beamW/2, beamL, beamW, shape);
                    break;
                case LEFT:
                    factory.makeRect(-beamL, -beamW/2, beamL, beamW, shape);
                    break;
            }
            batch.outline(shape, transform);
        }
    }

    public void drawRayDebug(SpriteBatch batch) {
        if (debugRayStart != null && debugRayEnd != null) {
            float u = obstacle.getPhysicsUnits();
            Vector2 localStart = new Vector2(debugRayStart).sub(obstacle.getPosition());
            Vector2 localEnd = new Vector2(debugRayEnd).sub(obstacle.getPosition());

            PathFactory factory = new PathFactory();
            Path2 rayOutline = new Path2();
            factory.makeLine(localStart.x * u, localStart.y * u,
                localEnd.x * u, localEnd.y * u, rayOutline);

            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.PURPLE);
            transform.idt();
            float a = obstacle.getAngle();
            Vector2 p = obstacle.getPosition();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);
            batch.outline(rayOutline, transform);


        }
    }
}
