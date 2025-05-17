package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private int shootLimit = 100;
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

    private float susCooldown = 20;
    private float susCountdown = susCooldown;

    private Animator idleSprite;
    private Animator turnSprite;
    private Animator shootSprite;
    private Animator stunSprite;
    private Animator floatSprite;
    private AnimationState animationState;

    private float turnTimer = 0f;
    private float turnCooldown = 3.0f;
    private boolean inTurnAnimation = false;
    private int turnFrameCounter = 0;

    private boolean lastFacingRight;

    private float facingDirectionHoldTimer = 0f;
    private final float facingHoldDuration = 3.0f;

    private boolean shouldFlipAfterTurn = false;
    private boolean inShootAnimation = false;
    private boolean inStunAnimation = false;
    private boolean onPlatform = isGrounded;
    private boolean attackFacingRight;
    private boolean lockFacing = false;




    private enum AnimationState {
        IDLE, TURN,FLOAT,SHOOT, STUN
    }

    public void createAnimators(Texture attack, Texture hover, Texture stunned, Texture turn) {
        shootSprite = new Animator(attack, 4, 8, 0.06f, 28, 0, 27, false);
        floatSprite = new Animator(hover, 8, 8, 0.06f, 39, 16, 38);
        stunSprite = new Animator(stunned, 2, 8, 0.06f, 15, 0, 14, false);
        turnSprite = new Animator(turn, 4, 8, 0.06f, 31, 0, 30, false);

    }


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
        obstacle.setName("dweller");

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
        sensorName = "dweller_sensor";
        sensorFixture.setUserData(sensorName);

        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
        sensorShape.dispose();
    }

    public void lookForPlayer() {
        World world = obstacle.getBody().getWorld();
        Player player = scene.getAvatar();
        Vector2 pos = obstacle.getPosition();
        float rayLength = 20f;

        Vector2 start = new Vector2(pos.x + (facingRight ? width / 2 : -width / 2), pos.y + height / 8);

        Vector2 playerPos = player.getObstacle().getPosition();
        float pHeight = player.getHeight();
        Vector2[] targets = new Vector2[] {
            new Vector2(playerPos.x, playerPos.y),
            new Vector2(playerPos.x, playerPos.y + pHeight * 0.3f),
            new Vector2(playerPos.x, playerPos.y - pHeight * 0.3f)
        };

        boolean seen = false;
        Vector2 chosenEnd = null;

        for (Vector2 target : targets) {
            float angle = MathUtils.atan2(target.y - start.y, target.x - start.x);
            Vector2 end = new Vector2(start.x + rayLength * MathUtils.cos(angle), start.y + rayLength * MathUtils.sin(angle));

            // 1. Player check
            EnemyVisionRaycast playerRay = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.PLAYER_CHECK, 0f);
            world.rayCast(playerRay, start, end);
            boolean hitPlayer = playerRay.getHitPlayer() != null;
            float playerFraction = playerRay.getClosestFraction();

            if (!hitPlayer) continue;

            // 2. Wall check
            EnemyVisionRaycast wallRay = new EnemyVisionRaycast(EnemyVisionRaycast.VisionMode.WALL_CHECK, 0f);
            world.rayCast(wallRay, start, end);
            Fixture wallHit = wallRay.getHitFixture();

            boolean blocked = false;
            if (wallHit != null) {
                float wallFraction = wallRay.getClosestFraction();
                if (wallFraction + 0.01f < playerFraction) {
                    blocked = true;
                }
            }

            if (!blocked) {
                seen = true;
                chosenEnd = playerRay.getHitPoint().cpy();
                break;
            }
        }

        debugLookStart.set(start);
        debugLookEnd.set(seen && chosenEnd != null ? chosenEnd : start.cpy().add(10f, 0f));

        setAwareOfPlayer(seen);
        if (seen) {
            System.out.println("Seen the player");
        }
}


    @Override
    public void update(float dt) {
        lookForPlayer();
        System.out.println(animationState);
        if (obstacle != null && obstacle.getBody() != null) {
            obstacle.getBody().setGravityScale(0); // always floating
        }
        float playerX = scene.getAvatar().getObstacle().getPosition().x;
        if (playerX < obstacle.getX() && facingRight || playerX > obstacle.getX() && !facingRight ){
            inTurnAnimation = true;
        }

        if (isStunned()) {
            isShooting = false;
            inStunAnimation = true;
            animationState = AnimationState.STUN;
        }
        else if (inStunAnimation) {
            if (stunSprite.isAnimationFinished()) {
                animationState = AnimationState.FLOAT;
                inStunAnimation = false;
                stunSprite.reset();
            } else {
                animationState = AnimationState.STUN;
            }
        }

        if (inStunAnimation) {
            super.update(dt);
            return;
        }

        // Handle shooting cooldown logic
        if (isShooting()) {
            shootCooldown = shootLimit;
            inShootAnimation = true;
            animationState = AnimationState.SHOOT;
            shootSprite.reset();
            attackFacingRight = facingRight; // lock the direction for the shoot
            lockFacing = true;
            isShooting = false; // one-time trigger
        } else {
            shootCooldown = Math.max(0, shootCooldown - 1);
        }

        // Handle shoot animation finishing
        if (inShootAnimation) {
            // flip the direction so it doesn't look wierd shooting backwards
            if (playerX < obstacle.getX() && facingRight || playerX > obstacle.getX() && !facingRight ){
                facingRight = !facingRight;
            }
            animationState = AnimationState.SHOOT;
            if (shootSprite.isAnimationFinished()) {
                System.out.println("FINISHED SHOOTING");
                shootSprite.reset();
                inShootAnimation = false;
            }
        }
        // Handle turn animation
        else if (inTurnAnimation) {
            animationState = AnimationState.TURN;
            if (turnSprite.isAnimationFinished()) {
                System.out.println("FINISHED TURNING");
                inTurnAnimation = false;
                turnSprite.reset();
                animationState = AnimationState.FLOAT;
                facingRight = !facingRight;

            }
        }

        // Default floating state
        else {
            animationState = AnimationState.FLOAT;
        }

        super.update(dt);
    }



    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion frame = null;
        if (animationState == null)
        {
            return;
        }
        switch (animationState) {
            case IDLE:
                frame = idleSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case FLOAT:
                frame = floatSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case TURN:
                frame = turnSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case SHOOT:
                frame = shootSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
            case STUN:
                frame = stunSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());
                break;
        }


        if (facingRight) {
            frame.flip(true, false);
        }


        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = width * u * 1.5f;
        float drawHeight = height * u * 1.15f;
        float originX = drawWidth / 2f;
        float originY = drawHeight / 2f;

        batch.draw(frame, posX - originX, posY - originY, originX, originY, drawWidth, drawHeight, 1f, 1f, 0f);
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

        if (isShooting() && !inShootAnimation) {
            inShootAnimation = true;
            shootSprite.reset();
            attackFacingRight = facingRight;
            lockFacing = true;
        }
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
