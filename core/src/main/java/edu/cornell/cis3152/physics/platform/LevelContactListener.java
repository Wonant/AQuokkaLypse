package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class LevelContactListener implements ContactListener {

    private final PlatformScene dreamWalkerScene;


    private boolean playerSlowed = false;
    private float originalPlayerMovement;

    public LevelContactListener(PlatformScene scene) {
        this.dreamWalkerScene = scene;
    }



    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.
     * We use this method to test if it is the "right" kind of collision. In
     * particular, we use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bodyDataA = fix1.getBody().getUserData();
        Object bodyDataB = fix2.getBody().getUserData();

        try {

            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();

            handleShieldWallContact(bd1, bd2);
            handleDoorContact(bd1, bd2);
            handleDreamShardContact(bd1, bd2, fd1, fd2);
            //handleFollowSensorContact(bd1, bd2, fd1, fd2);
            handleWalkSensorContact(bd1, bd2, fd1, fd2);
            handleVisionSensorContact(bd1, bd2, fd1, fd2);
            handleBulletCollision(bd1, bd2, fd1, fd2);
            handleGroundContact(bd1, bd2, fd1, fd2, fix1, fix2);
            handleHarvestingCollision(bd1, bd2, fd1, fd2);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch. The main use of
     * this method is to determine when the character is NOT on the ground. This
     * is how we prevent double jumping.
     */
    @Override
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        //Object bodyDataA = fix1.getBody().getUserData();
        //Object bodyDataB = fix2.getBody().getUserData();

        handleWalkSensorEndContact(bd1, bd2, fd1, fd2);
        handleFollowSensorEndContact(fix1, fix2, fd1, fd2);
        handleHarvestingEndContact(bd1, bd2, fd1, fd2);
        handleTeleporterEndContact(bd1, bd2);
        handleVisionSensorEndContact(fd1, fd2, fix1, fix2);
        handleGroundEndContact(bd1, bd2, fd1, fd2, fix1, fix2);
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}


    private void handleShieldWallContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if (bd1 instanceof ShieldWall || bd2 instanceof ShieldWall){
            System.out.println("Contact detected with Shield Wall");
            System.out.println(bd1.getClass());
            System.out.println(bd2.getClass());
            System.out.println(bd1.getObstacle().getPosition());
            System.out.println(bd2.getObstacle().getPosition());

        }
    }

    private void handleDoorContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Door) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Door)) {
            if (dreamWalkerScene.checkCollectedAllGoals()) {
                dreamWalkerScene.setComplete(true);
            }
        }
    }

    private void handleDreamShardContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2)
    {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Shard)
            || (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Shard
            && !(dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) ||
            dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2)) )) {

            Shard collectedShard = (bd1 instanceof Shard) ? (Shard) bd1 : (Shard) bd2;

            if (!collectedShard.getObstacle().isRemoved()) {
                collectedShard.getObstacle().markRemoved(true);
                dreamWalkerScene.incrementGoal();
            }
        }
    }

    private void handleFollowSensorContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        if (("follow_sensor".equals(fd1) && (bd2 instanceof Player)) ||
            ("follow_sensor".equals(fd2) && (bd1 instanceof Player))) {
            if (bd1 instanceof CuriosityCritter) {
                ((CuriosityCritter) bd1).playerInFollowRange = true;
            } else if (bd2 instanceof CuriosityCritter){
                ((CuriosityCritter) bd2).playerInFollowRange = true;
            }
            // This should be handled in update???
            if (!playerSlowed) {
                Player player = dreamWalkerScene.getAvatar();
                originalPlayerMovement = player.getMovement(); // store whatever it is now
                float newMovement = originalPlayerMovement * 0.5f;
                player.setMovement(newMovement);
                playerSlowed = true;
                System.out.println("Player movement slowed by critter follow sensor");
            }
        }
    }

    private void handleWalkSensorContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        if (("walk_sensor".equals(fd1) && (bd2 instanceof Surface || bd2 instanceof Enemy)) ||
            ("walk_sensor".equals(fd2) && (bd2 instanceof Surface || bd2 instanceof Enemy))) {
            System.out.println("walk_sensor collision detected with: " + bd1 + " and " + bd2);


            // Ensure the Enemy reference is correctly retrieved
            Enemy e = (bd1 instanceof Enemy) ? (Enemy) bd1
                : (bd2 instanceof Enemy) ? (Enemy) bd2
                    : null;

            if (e != null) {
                e.setSeesWall(true);
                System.out.println("Enemy sees wall");
            } else {
                System.out.println("WARNING: Walk sensor collision detected but Enemy reference is null.");
            }
        }
    }

    private void handleVisionSensorContact(ObstacleSprite bodyDataA, ObstacleSprite bodyDataB, Object fd1, Object fd2) {
        // If there is a collision between a vision sensor and the player
        if ( ("vision_sensor".equals(fd1) || "vision_sensor".equals(fd2))
            && (bodyDataA instanceof Player || bodyDataB instanceof Player)
            && !(dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) ||
            dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2))) {

            System.out.println("ENEMY SEES PLAYER !!!!!!");

            // Check if the vision sensor belongs to an "un-stunned" enemy, and if
            // so update the enemy's awareness and apply damage to player
            if ( bodyDataA instanceof Enemy && !((Enemy) bodyDataA).isStunned() ) {
                ((Enemy) bodyDataA).setAwareOfPlayer(true);
                System.out.println(bodyDataA.getClass() + " saw player!");
                dreamWalkerScene.getAvatar().setTakingDamage(true);
            }
            else if ( bodyDataB instanceof Enemy && !((Enemy) bodyDataB).isStunned() )  {
                System.out.println(bodyDataB.getClass() + " saw player!");
                ((Enemy) bodyDataB).setAwareOfPlayer(true);
                dreamWalkerScene.getAvatar().setTakingDamage(true);
            }
        }
    }

    private void handleBulletCollision(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        handleBulletHit(bd1, bd2, fd2);
        handleBulletHit(bd2, bd1, fd1);
    }

    private void handleBulletHit(ObstacleSprite bullet, ObstacleSprite target, Object targetFixtureData) {
        if (!bullet.getName().equals("bullet") || target == dreamWalkerScene.getAvatar() || target instanceof Shard) {
            return;
        }

        if (target instanceof Enemy) {
            if (targetFixtureData != "walk_sensor" && targetFixtureData != "vision_sensor" &&
                targetFixtureData != "follow_sensor" && targetFixtureData != "alert_sensor") {

                dreamWalkerScene.removeBullet(bullet);
                applyStunEffect(target);
            }
        } else {
            dreamWalkerScene.removeBullet(bullet);
        }
    }

    private void applyStunEffect(ObstacleSprite enemy) {
        if (enemy instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) enemy;
            critter.setStunned(true);
            critter.setStunTexture(dreamWalkerScene.directory);
            System.out.println("Critter is stunned");
        } else if (enemy instanceof MindMaintenance) {
            MindMaintenance maintenance = (MindMaintenance) enemy;
            maintenance.setStunned(true);
            maintenance.setStunTexture(dreamWalkerScene.directory);
            System.out.println("Maintenance is stunned");
        } else if (enemy instanceof DreamDweller) {
            DreamDweller dweller = (DreamDweller) enemy;
            dweller.setStunned(true);
            dweller.setStunTexture(dreamWalkerScene.directory);
            System.out.println("Dweller is stunned");
        } else {
            System.out.println("WARNING: Bullet stun collision detected but Enemy reference is null.");
        }
    }

    private void handleGroundContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2, Fixture fix1, Fixture fix2) {
        if ((dreamWalkerScene.getAvatar().getSensorName().equals(fd2) && bd1 instanceof Surface) ||
            (dreamWalkerScene.getAvatar().getSensorName().equals(fd1) && bd2 instanceof Surface)) {

            dreamWalkerScene.getAvatar().setGrounded(true);
            dreamWalkerScene.sensorFixtures.add(dreamWalkerScene.getAvatar() == bd1 ? fix2 : fix1);

            Surface currentSurface;
            if (bd1 instanceof Surface) {
                currentSurface = (Surface) bd1;
            } else {
                currentSurface = (Surface) bd2;
            }

            if (currentSurface.isShadowed()) {
                dreamWalkerScene.getAvatar().setIsShadow(true);
                dreamWalkerScene.shadowSensorFixtures.add(dreamWalkerScene.getAvatar() == bd1 ? fix2 : fix1);
            }
        }

    }

    private void handleHarvestingCollision(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        // if there is a collision between an enemy and the player's scare sensor
        if(( (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) && bd2 instanceof Enemy)
            || (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) && bd1 instanceof Enemy) )){
            Enemy harvestedEnemy;
            // if the enemy is not a Mind Maintenance
            if (!(bd2 instanceof MindMaintenance) && !(bd1 instanceof MindMaintenance)) {
                if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) && fd2 != "walk_sensor" && fd2 != "follow_sensor" && fd2 != "vision_sensor" )
                {
                    harvestedEnemy = (Enemy) bd2;
                    dreamWalkerScene.performHarvest(harvestedEnemy);

                } else if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) && fd1 != "walk_sensor" && fd1 != "follow_sensor" && fd1 != "vision_sensor")
                {
                    harvestedEnemy = (Enemy) bd1;
                    dreamWalkerScene.performHarvest(harvestedEnemy);

                }
            }
            //dreamWalkerScene.getAvatar().setHarvesting(true);

        }
    }

    private void handleWalkSensorEndContact(Object bd1, Object bd2, Object fd1, Object fd2) {
        if (("walk_sensor".equals(fd1) && bd2 instanceof Surface) ||
            ("walk_sensor".equals(fd2) && bd1 instanceof Surface)) {

            CuriosityCritter critter = (bd1 instanceof CuriosityCritter) ? (CuriosityCritter) bd1
                : (bd2 instanceof CuriosityCritter) ? (CuriosityCritter) bd2
                    : null;

            if (critter != null) {
                critter.setSeesWall(false);
                System.out.println("Critter stopped seeing wall");
            } else {
                System.out.println("WARNING: Walk sensor end contact detected but Critter reference is null.");
            }
        }
    }

    private void handleFollowSensorEndContact(Fixture fix1, Fixture fix2, Object fd1, Object fd2) {
        Object bodyDataA = fix1.getBody().getUserData();
        Object bodyDataB = fix2.getBody().getUserData();

        if (("follow_sensor".equals(fd1) || "follow_sensor".equals(fd2)) &&
            (bodyDataA instanceof Player || bodyDataB instanceof Player)) {

            if (bodyDataA instanceof Enemy) {
                ((Enemy) bodyDataA).setAwareOfPlayer(false);
            } else {
                ((Enemy) bodyDataB).setAwareOfPlayer(false);
            }

            if (playerSlowed) {
                Player player = dreamWalkerScene.getAvatar();
                player.setMovement(originalPlayerMovement); // revert
                playerSlowed = false;
                System.out.println("Player movement restored after follow sensor separation");
            }

            dreamWalkerScene.getAvatar().setTakingDamage(false);
            System.out.println("Enemy stopped seeing player");
        }
    }

    private void handleHarvestingEndContact(Object bd1, Object bd2, Object fd1, Object fd2) {
        if ((dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) && bd2 instanceof Enemy) ||
            (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) && bd1 instanceof Enemy)) {

            Enemy harvestedEnemy;
            // if the enemy is not a Mind Maintenance
            if (!(bd2 instanceof MindMaintenance) && !(bd1 instanceof MindMaintenance)) {
                if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) &&
                    fd2 != "walk_sensor" && fd2 != "follow_sensor" && fd2 != "vision_sensor") {

                    harvestedEnemy = (Enemy) bd2;
                    dreamWalkerScene.removeHarvestedEnemy(harvestedEnemy);
                } else if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) &&
                    fd1 != "walk_sensor" && fd1 != "follow_sensor" && fd1 != "vision_sensor") {

                    harvestedEnemy = (Enemy) bd1;
                    dreamWalkerScene.removeHarvestedEnemy(harvestedEnemy);
                }
            }
        }
    }

    private void handleTeleporterEndContact(Object bd1, Object bd2) {
        if ((bd1 instanceof Teleporter && bd2 == dreamWalkerScene.getAvatar()) ||
            (bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Teleporter)) {

            dreamWalkerScene.setCurrentTeleporter(null);
            System.out.println("Player moved away from teleporter");
        }
    }

    private void handleVisionSensorEndContact(Object fd1, Object fd2, Fixture fix1, Fixture fix2) {
        if ("dweller_vision_sensor".equals(fd1) || "dweller_vision_sensor".equals(fd2)) {
            Object bodyDataA = fix1.getBody().getUserData();
            Object bodyDataB = fix2.getBody().getUserData();

            DreamDweller dweller = null;
            Player playerObj = null;

            if (bodyDataA instanceof DreamDweller && bodyDataB instanceof Player) {
                dweller = (DreamDweller) bodyDataA;
                playerObj = (Player) bodyDataB;
            } else if (bodyDataA instanceof Player && bodyDataB instanceof DreamDweller) {
                dweller = (DreamDweller) bodyDataB;
                playerObj = (Player) bodyDataA;
            }

            if (dweller != null) {
                dweller.setAwareOfPlayer(true);
                dreamWalkerScene.getAvatar().setTakingDamage(false);
                System.out.println("Dream Dweller lost sight of player");
            }
        }
    }

    private void handleGroundEndContact(Object bd1, Object bd2, Object fd1, Object fd2,
        Fixture fix1, Fixture fix2) {
        if ((dreamWalkerScene.getAvatar().getSensorName().equals(fd2) && bd1 instanceof Surface) ||
            (dreamWalkerScene.getAvatar().getSensorName().equals(fd1) && bd2 instanceof Surface)) {

            dreamWalkerScene.sensorFixtures.remove(dreamWalkerScene.getAvatar() == bd1 ? fix2 : fix1);
            if (dreamWalkerScene.sensorFixtures.size == 0) {
                dreamWalkerScene.getAvatar().setGrounded(false);
            }

            dreamWalkerScene.shadowSensorFixtures.remove(dreamWalkerScene.getAvatar() == bd1 ? fix2 : fix1);
            if (dreamWalkerScene.shadowSensorFixtures.size == 0) {
                dreamWalkerScene.getAvatar().setIsShadow(false);
            }
        }
    }
}
