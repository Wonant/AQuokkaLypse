package edu.cornell.cis3152.physics.platform;

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

import javax.swing.*;

public class Enemy extends ObstacleSprite {
    private boolean awareOfPlayer;
    private boolean stunned;
    private boolean seesWall;
    protected Body headBody;
    protected RevoluteJoint headJoint;



    public Enemy(){
        this.awareOfPlayer = false;
        this.stunned = false;
        this.seesWall = false;
    }
    public boolean isAwareOfPlayer(){
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



    public Body getHeadBody() {
        return headBody;
    }
}
