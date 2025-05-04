package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;

public class DreamDweller extends Enemy {
    private final JsonValue data;

    private float width;
    private float height;

    private float force;
    private float damping;
    private float max_speed;
    private float jump_force;
    private int jumpCooldown;
    private int shootCooldown;
    private int shootLimit = 80;
    private boolean isGrounded;
    private boolean isJumping;
    private boolean isShooting;
    private boolean facingRight;

    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;
    private Path2 harvestOutline;

    private final Vector2 debugLookStart = new Vector2();
    private final Vector2 debugLookEnd = new Vector2();

    private float susCooldown = 40;
    private float susCountdown = susCooldown;

    public DreamDweller(float units, JsonValue data, float[] points, PlatformScene scene) {
        super(null);
        this.data = data;
        this.scene = scene;

        float x = points[0];
        float y = points[1];
        float s = data.getFloat("size");

        float size = s * units;

        width = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);

        float drawWidth = (float) (size / 1.5);
        float drawHeight = size;

        obstacle = new CapsuleObstacle(x, y, width, height);
        JsonValue debugInfo = data.get("debug");
        ((CapsuleObstacle) obstacle).setTolerance(debugInfo.getFloat("tolerance", 0.5f));

        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("maintenance");

        debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
        sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);

        max_speed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        jump_force = data.getFloat("jump_force", 0);

        isGrounded = false;
        isShooting = false;
        isJumping = false;
        facingRight = true;
        jumpCooldown = 0;
        shootCooldown = 0;

        mesh.set(-drawWidth / 1.5f, -drawHeight / 1.6f, drawWidth * 1.5f, drawHeight * 1.5f);
    }

    public void setActiveTexture(AssetDirectory directory) {
        Texture texture = directory.getEntry("dream-dweller-active", Texture.class);
        this.setTexture(texture);
    }

    public void setStunTexture(AssetDirectory directory) {
        Texture texture = directory.getEntry("dream-dweller-inactive", Texture.class);
        this.setTexture(texture);
    }

    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, -height / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density", 0);
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("shrink", 0) * width / 2.0f;
        float h = sensorjv.getFloat("height", 0);
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorName = "maintenance_sensor";
        sensorFixture.setUserData(sensorName);

        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
        sensorShape.dispose();

        createHarvestSensor(width, height);
    }

    public void createHarvestSensor(float harvestWidth, float harvestHeight) {
        float u = obstacle.getPhysicsUnits();
        float maxSafeRadius = Math.min(harvestWidth, harvestHeight) / 2 * u;

        PathFactory factory = new PathFactory();
        harvestOutline = new Path2();
        factory.makeRoundedRect(-harvestWidth / 2 * u, -harvestHeight / 2 * u,
            harvestWidth * u, harvestHeight * u,
            maxSafeRadius, harvestOutline);

        PolygonShape harvestShape = new PolygonShape();
        harvestShape.setAsBox(harvestWidth / 2, harvestHeight / 2);

        FixtureDef harvestDef = new FixtureDef();
        harvestDef.shape = harvestShape;
        harvestDef.isSensor = true;

        Body body = obstacle.getBody();
        Fixture harvestFixture = body.createFixture(harvestDef);
        harvestFixture.setUserData("harvest_sensor");

        harvestShape.dispose();
    }

    public void lookForPlayer() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();

        Vector2 pos = obstacle.getPosition();
        float rayLength = 10f;
        float angle = MathUtils.atan2(
            player.getObstacle().getY() - pos.y,
            player.getObstacle().getX() - pos.x
        );

        Vector2 start = new Vector2(pos.x + (facingRight ? width / 2 : -width / 2), pos.y + height / 4);
        Vector2 end = new Vector2(
            pos.x + rayLength * MathUtils.cos(angle),
            pos.y + rayLength * MathUtils.sin(angle)
        );

        // Step 1: Check if we hit the player
        EnemyVisionRaycast playerRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.PLAYER_CHECK, 4f);
        world.rayCast(playerRaycast, start, end);
        boolean hitPlayer = playerRaycast.getHitPlayer() != null;

        // Step 2: Check if there's a wall in the way
        boolean blocked = false;
        if (hitPlayer) {
            EnemyVisionRaycast wallRaycast = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.WALL_CHECK, 0f);
            world.rayCast(wallRaycast, start, end);
            blocked = wallRaycast.getHitFixture() != null;
        }

        debugLookStart.set(start);
        debugLookEnd.set(hitPlayer ? playerRaycast.getHitPoint() : end);

        if (hitPlayer && !blocked) {
            setAwareOfPlayer(true);
            System.out.println("Seen the player");
        } else {
            setAwareOfPlayer(false);
        }

        playerRaycast.reset();
    }


    @Override
    public void update(float dt) {
        lookForPlayer();

        // 设置 sprite 朝向
        Vector2 playerPos = scene.getAvatar().getObstacle().getPosition();
        Vector2 selfPos = obstacle.getPosition();
        facingRight = playerPos.x >= selfPos.x;

        if (isAwareOfPlayer() && !scene.isFailure()) {
            setShooting(true);
            susCountdown = susCooldown;
        } else {
            susCountdown--;
            setShooting(false);
        }

        if (isShooting()) {
            shootCooldown = shootLimit;
        } else {
            shootCooldown = Math.max(0, shootCooldown - 1);
        }

        super.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (obstacle != null && mesh != null) {
            float x = obstacle.getX();
            float y = obstacle.getY();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preScale(facingRight ? -1.4f : 1.4f, 1.4f);
            transform.preRotate((float) (a * MathUtils.radiansToDegrees));
            transform.preTranslate(x * u, (y - height / 12f) * u);

            batch.setTextureRegion(sprite);
            batch.drawMesh(mesh, transform, false);
            batch.setTexture((Texture) null);
        }
    }

    @Override
    public void drawDebug(SpriteBatch batch) {
        super.drawDebug(batch);
        if (sensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(sensorColor);

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            batch.outline(sensorOutline, transform);
        }
        if (harvestOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(Color.BLUE);

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            transform.idt();
            transform.preRotate((float)(a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            batch.outline(harvestOutline, transform);
            batch.setColor(Color.WHITE);
        }
        drawRayDebug(batch);
    }

    public void drawRayDebug(SpriteBatch batch) {
        float u = obstacle.getPhysicsUnits();
        Vector2 localStart = new Vector2(debugLookStart).sub(obstacle.getPosition());
        Vector2 localEnd = new Vector2(debugLookEnd).sub(obstacle.getPosition());

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

    public boolean isShooting() {
        return isShooting && shootCooldown <= 0 && !isStunned();
    }

    public void setShooting(boolean value) {
        isShooting = value;
    }
    public void applyForce() {
        setMovement(0);

        if (obstacle.isActive()) {
            obstacle.setVX(0);
            obstacle.setVY(0);
        }

        Body body = obstacle.getBody();
        if (body != null) {
            body.setLinearVelocity(0, 0);
            body.setAngularVelocity(0);
        }
    }
}




