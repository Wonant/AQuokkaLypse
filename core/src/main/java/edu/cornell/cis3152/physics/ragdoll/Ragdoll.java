/*
 * Ragdoll.java
 *
 * This class stores the joints of the ragdoll. We did not really need a
 * separate class for this, as it has no update. Like our other model classes,
 * it is solely for organizational purposes. It is a subclass of ObstacleGroup
 * because the primary purpose of this class is to initialize the joints
 * between obstacles.
 *
 * This is one of the files that you are expected to modify. Please limit
 * changes to the regions that say INSERT CODE HERE.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.ragdoll;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import edu.cornell.cis3152.physics.ObstacleGroup;

/**
 * A ragdoll whose body parts are boxes connected by joints
 *
 * This class has several bodies connected by joints. For information on how
 * the joints fit together, see the ragdoll diagram at the start of the class.
 * The position of the ragdoll is the position of the torso obstacle. Every
 * other body part is defined as an OFFSET of that position. See the associated
 * JSON constants for more information.
 */
public class Ragdoll extends ObstacleGroup {
    /** Files for the body textures */
    public static final String[] BODY_PARTS = { "torso", "head", "arm", "forearm", "thigh", "shin" };

    // Layout of ragdoll
    //
    // o = joint
    //                   ___
    //                  |   |
    //                  |_ _|
    //   ______ ______ ___o___ ______ ______
    //  |______o______o       o______o______|
    //                |       |
    //                |       |
    //                |_______|
    //                | o | o |
    //                |   |   |
    //                |___|___|
    //                | o | o |
    //                |   |   |
    //                |   |   |
    //                |___|___|
    //

    /** Indices for the body parts in the bodies array */
    private static final int PART_NONE = -1;
    private static final int PART_BODY = 0;
    private static final int PART_HEAD = 1;
    private static final int PART_LEFT_ARM  = 2;
    private static final int PART_RIGHT_ARM = 3;
    private static final int PART_LEFT_FOREARM  = 4;
    private static final int PART_RIGHT_FOREARM = 5;
    private static final int PART_LEFT_THIGH  = 6;
    private static final int PART_RIGHT_THIGH = 7;
    private static final int PART_LEFT_SHIN  = 8;
    private static final int PART_RIGHT_SHIN = 9;

    /**
     * Returns the texture index for the given body part
     *
     * As some body parts are symmetrical, we reuse textures.
     *
     * @return the texture index for the given body part
     */
    private static int partToAsset(int part) {
        switch (part) {
        case PART_BODY:
            return 0;
        case PART_HEAD:
            return 1;
        case PART_LEFT_ARM:
        case PART_RIGHT_ARM:
            return 2;
        case PART_LEFT_FOREARM:
        case PART_RIGHT_FOREARM:
            return 3;
        case PART_LEFT_THIGH:
        case PART_RIGHT_THIGH:
            return 4;
        case PART_LEFT_SHIN:
        case PART_RIGHT_SHIN:
            return 5;
        default:
            return -1;
        }
    }

    /**
     * Returns true if the body part is on the right side
     *
     * @return true if the body part is on the right side
     */
    private static boolean partOnLeft(int part) {
        switch (part) {
            case PART_LEFT_ARM:
            case PART_LEFT_FOREARM:
            case PART_LEFT_THIGH:
            case PART_LEFT_SHIN:
                return true;
            default:
                return false;
        }
    }

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    private final float units;

    /** Bubble generator to glue to snorkler. */
    private final BubbleGenerator bubbler;

    /** Texture assets for the body parts */
    private Texture[] partTextures;

    /** Cache vector for organizing body parts */
    private final Vector2 partCache = new Vector2();

    /**
     * Returns the bubble generator welded to the mask
     *
     * @return the bubble generator welded to the mask
     */
    public BubbleGenerator getBubbleGenerator() {
        return bubbler;
    }

    /**
     * Creates a new spinner with the given physics data.
     *
     * The physics units are used to size the meshes for each body part relative
     * to the physics bodies. All other attributes are defined by the JSON file.
     *
     * @param units     The physics units
     * @param data      The physics constants for the ragdoll
     */
    public Ragdoll(float units, JsonValue data) {
        super();
        this.data = data.get("doll");
        this.units = units;

        // We do not do anything yet.
        BoxObstacle part;

        // TORSO
        part = makePart(PART_BODY, PART_NONE);
        part.setFixedRotation(true);

        // HEAD
        makePart(PART_HEAD, PART_BODY);

        // ARMS
        makePart(PART_LEFT_ARM, PART_BODY);
        part = makePart(PART_RIGHT_ARM, PART_BODY);
        part.setAngle((float)Math.PI);

        // FOREARMS
        makePart(PART_LEFT_FOREARM, PART_LEFT_ARM);
        part = makePart(PART_RIGHT_FOREARM, PART_RIGHT_ARM);
        part.setAngle((float)Math.PI);

        // THIGHS
        makePart(PART_LEFT_THIGH,  PART_BODY);
        makePart(PART_RIGHT_THIGH, PART_BODY);

        // SHINS
        makePart(PART_LEFT_SHIN,  PART_LEFT_THIGH);
        makePart(PART_RIGHT_SHIN, PART_RIGHT_THIGH);

        bubbler = new BubbleGenerator(units, data);
        sprites.add(bubbler);
    }

    /**
     * Sets the assets for this ragdoll.
     *
     * The ragdoll has multiple textures, one for each body part. Instead of
     * making a setter for each body part, we just extract them all at once
     * from the asset directory.
     *
     * @param directory The directory of loaded assets
     */
    public void setAssets(AssetDirectory directory) {
        Texture[] partTextures = new Texture[BODY_PARTS.length];

        for(int ii = 0; ii < BODY_PARTS.length; ii++) {
            partTextures[ii] = directory.getEntry("ragdoll-"+BODY_PARTS[ii],Texture.class);
        }
        for(int ii = 0; ii <= PART_RIGHT_SHIN; ii++) {
            (sprites.get(ii)).setTexture(partTextures[partToAsset(ii)]);
        }
    }

    /**
     * Helper method to make a single body part
     *
     * While it looks like this method "connects" the pieces, it does not really.
     * It puts them in position to be connected by joints, but they will fall
     * apart unless you make the joints.
     *
     * @param part      The part to make
     * @param connect   The part to connect to
     *
     * @return the newly created part
     */
    private BoxObstacle makePart(int part, int connect) {
        String name = BODY_PARTS[partToAsset( part )];
        JsonValue pos  = part == PART_BODY ? data.get(name).get("position") : data.get(name).get("offset");
        JsonValue size = data.get(name).get("size");

        float x = pos.getFloat( 0 );
        float y = pos.getFloat( 1 );
        if (partOnLeft( part )) {
            x = -x;
        }

        partCache.set(x,y);
        if (connect != PART_NONE) {
            partCache.add(sprites.get(connect).getObstacle().getPosition());
        }

        float w  = size.getFloat(0);
        float h  = size.getFloat(1);

        BoxObstacle obstacle = new BoxObstacle(partCache.x, partCache.y, w, h);
        obstacle.setDensity(data.getFloat( "density", 0.0f ));
        obstacle.setPhysicsUnits( units );

        ObstacleSprite sprite = new ObstacleSprite(obstacle);
        sprites.add(sprite);

        return obstacle;
    }

    /**
     * Returns the box2d body for the given part.
     *
     * Our class suffers a little bit from "ravioli code". Bodies are stored in
     * obstacles and obstacles are stored in obstacle sprites. While it is
     * easy to get an obstacle sprite from a part index, we actually want the
     * body in {@link #createJoints}. This is a helper to make our lives a
     * little easier.
     *
     * @param part  The body part
     *
     * @return the box2d body for the given part.
     */
    private Body getBody(int part) {
        return sprites.get(part).getObstacle().getBody();
    }

    /**
     * Helper method to get offset data from JSON. Do not call with Torso
     *
     * @param bodyPart Corresponding body part
     *
     * @return Vector of offsets (x,y)
     */
    private Vector2 getOffsets(int bodyPart)
    {

        String name = BODY_PARTS[partToAsset(bodyPart)];
        float x = data.get(name).get("offset").getFloat(0);
        float y = data.get(name).get("offset").getFloat(1);
        return new Vector2(x,y);
    }

    /**
     * Helper method to get size data from JSON.
     *
     * @param bodyPart Corresponding body part
     *
     * @return Vector of offsets (x,y)
     */
    private Vector2 getSize(int bodyPart)
    {
        String name = BODY_PARTS[partToAsset(bodyPart)];
        float width = data.get(name).get("size").getFloat(0);
        float height = data.get(name).get("size").getFloat(1);
        return new Vector2(width,height);
    }

    /**
     * This method does the actual creation and connecting of joints between body parts
     *
     * @param part1 Part with the offset
     * @param toPart2 "Source" part
     * @param direction Vertical or Horizontal connection
     * @param world Box2D world to store joints
     */
    private void connectParts(int part1, int toPart2, int direction, World world)
    {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        Vector2 offset = getOffsets(part1);
        Vector2 size = getSize(part1);
        Vector2 size2 = getSize(toPart2);

        jointDef.bodyA = getBody(toPart2);
        jointDef.bodyB = getBody(part1);
        if (direction == 1) {
            if (part1 == PART_HEAD) {

                jointDef.localAnchorA.set(offset.x, offset.y - size.y / 2);
                jointDef.localAnchorB.set(0, -size.y / 2);
            } else {
                if(partOnLeft(part1))
                {
                    offset.x = -offset.x;
                }
                jointDef.localAnchorA.set(offset.x, offset.y + size.y / 2);
                jointDef.localAnchorB.set(0, size.y / 2);
            }
        } else
        {
             if(partOnLeft(part1) || part1 == PART_RIGHT_FOREARM)
            {
                jointDef.localAnchorA.set(-offset.x + size.x / 2, offset.y);
                jointDef.localAnchorB.set(size.x / 2,0);


            } else
            {
                jointDef.localAnchorA.set(offset.x - size.x / 2, offset.y);
                jointDef.localAnchorB.set(size.x / 2,0);
            }

        }

        jointDef.enableLimit = true;
        if(part1 == PART_HEAD)
        {
            jointDef.lowerAngle = (float) Math.toRadians(-45);
            jointDef.upperAngle = (float) Math.toRadians(45);

        } else if (part1 == PART_RIGHT_ARM)
        {
            jointDef.lowerAngle = (float) Math.toRadians(-270);
            jointDef.upperAngle = (float) Math.toRadians(270);
        }
        else {
            jointDef.lowerAngle = (float) Math.toRadians(-90);
            jointDef.upperAngle = (float) Math.toRadians(90);
        }
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);
    }


    /**
     * Creates the joints for this object.
     *
     * We implement our custom logic here.
     *
     * @param world Box2D world to store joints
     *
     * @return true if object allocation succeeded
     */
    protected boolean createJoints(World world) {
        assert sprites.size > 0;

        //#region INSERT CODE HERE
        // Implement all of the Ragdoll Joints here
        // You may add additional methods if you find them useful

        connectParts(PART_HEAD, PART_BODY, 1, world);
        connectParts(PART_LEFT_ARM, PART_BODY,2, world);
        connectParts(PART_RIGHT_ARM, PART_BODY,2, world);
        connectParts(PART_LEFT_FOREARM, PART_LEFT_ARM, 2, world);
        connectParts(PART_RIGHT_FOREARM, PART_RIGHT_ARM, 2, world);
        connectParts(PART_RIGHT_THIGH, PART_BODY, 1, world);
        connectParts(PART_LEFT_THIGH, PART_BODY, 1, world);
        connectParts(PART_LEFT_SHIN, PART_LEFT_THIGH, 1, world);
        connectParts(PART_RIGHT_SHIN, PART_RIGHT_THIGH, 1 ,world);



        //#endregion

        // Weld the bubbler to this mask
        WeldJointDef weldDef = new WeldJointDef();
        weldDef.bodyA = getBody(PART_HEAD);
        weldDef.bodyB = bubbler.getObstacle().getBody();
        weldDef.localAnchorA.set(bubbler.getOffset());
        weldDef.localAnchorB.set(0,0);
        Joint wjoint = world.createJoint(weldDef);
        joints.add(wjoint);

        return true;
    }
}
