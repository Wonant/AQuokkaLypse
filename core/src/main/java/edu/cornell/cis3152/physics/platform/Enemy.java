package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;
import jdk.jshell.spi.ExecutionControl;

import javax.swing.*;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

public class Enemy extends ObstacleSprite {
    // for easy reference to some world objects
    protected PlatformScene scene;

    private boolean awareOfPlayer;
    protected boolean wasAware;
    private boolean stunned;
    private boolean seesWall;
    protected Body headBody;
    protected RevoluteJoint headJoint;
    protected float movement;
    protected boolean facingRight;

    protected float stepRayLength;
    protected EnemyVisionRaycast enemyVisionRaycast;
    protected Vector2 debugRayStart;
    protected Vector2 debugRayEnd;

    protected Vector2 shardTarget;

    protected float shardAwareness;

    /** event dispatcher */
    protected final MessageDispatcher dispatcher;



    public Enemy(MessageDispatcher dispatcher){
        shardAwareness = 4.0f;
        this.awareOfPlayer = false;
        this.stunned = false;
        this.seesWall = false;
        this.dispatcher = dispatcher;
    }
    public boolean isAwareOfPlayer(){
        System.out.println("seesPlayer"+awareOfPlayer);
        return awareOfPlayer;
    }
    public void setAwareOfPlayer(boolean awareness){
        this.awareOfPlayer = awareness;
    }
    public void setStunned(boolean value) {
        stunned = value;
    }
    public boolean isStunned() {
        return stunned;
    }
    public boolean isSeesWall() {
        return seesWall;
    }
    public void setSeesWall(boolean b) {
        seesWall = b;
    }

    public float getMovement() {
        return movement;
    }

    public void setMovement(float value) {
        movement = value;
        // Change facing direction based on input
        if (movement < 0) {
            facingRight = false;
        } else if (movement > 0) {
            facingRight = true;
        }
    }

    public void setFilter() {
        for (Fixture fixture : obstacle.getBody().getFixtureList()) {
            Filter filter = fixture.getFilterData();
            filter.categoryBits = CATEGORY_ENEMY;
            // Only collide with scenery. (Assuming scenery fixtures are set to CATEGORY_SCENERY.)
            filter.maskBits = CATEGORY_SCENERY | CATEGORY_BULLET;
            fixture.setFilterData(filter);
        }
    }

    public boolean isDreamShardNear() {
        System.out.println("Checking for nearby shards to enemy " + this);
        for (int i = 0; i < scene.getTotalShards(); i++) {
            if (getObstacle().getPosition().dst(scene.getShardPos(i)) < shardAwareness) {
                shardTarget = scene.getShardPos(i);
                return true;
            }
        }
        return false;
    }

    public void removeFromWorld(World world) {
        // If we have a headBody, destroy it
        if (headBody != null) {
            // Also destroy the joint if it exists
            if (headJoint != null) {
                world.destroyJoint(headJoint);
                headJoint = null;
            }
            world.destroyBody(headBody);
            headBody = null;
        }
    }

    public PlatformScene getScene() {
        return scene;
    }

    public void dispatchHarvest() {
        if (dispatcher != null) {
            dispatcher.dispatchMessage(null, scene, MessageType.ENEMY_LOST_PLAYER);
        }
    }



    public Body getHeadBody() {
        return headBody;
    }
}
