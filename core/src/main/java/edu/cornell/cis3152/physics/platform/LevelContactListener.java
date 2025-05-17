package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class LevelContactListener implements ContactListener {

    private final PlatformScene dreamWalkerScene;
    private boolean playerHitShieldWall = false;

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


        try {


            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();

            handleShieldWallContact(bd1, bd2);
            handleDoorContact(bd1, bd2);
            handleDialougeContact(bd1, bd2);
            handleDreamShardContact(bd1, bd2, fd1, fd2);

            handleWalkSensorContact(bd1, bd2, fd1, fd2);
            handleBulletCollision(bd1, bd2, fd1, fd2);
            handleSpearHitPlayer(bd1, bd2);
            handleSpearHitSurface(bd1,bd2);
            handleGroundContact(bd1, bd2, fd1, fd2, fix1, fix2);
            handleHarvestingCollision(bd1, bd2, fd1, fd2);
            handleFallSensorContact(bd1, bd2, fd1, fd2);
            handleSpikeContact(bd1, bd2, fd2, fd2);
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

        handleHarvestingEndContact(bd1, bd2, fd1, fd2);
        handleGroundEndContact(bd1, bd2, fd1, fd2, fix1, fix2);
        handleShieldWallEndContact((ObstacleSprite) bd1, (ObstacleSprite) bd2);
        handleFallSensorEndContact((ObstacleSprite) bd1, (ObstacleSprite) bd2, fd1, fd2);
        if (((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Shard) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Shard)) &&
            (!("fall_sensor".equals(fd1) || "fall_sensor".equals(fd2))) &&
            (!(dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) || dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2))) &&
            (!("player_sensor".equals(fd1) || "player_sensor".equals(fd2)))) {
            Shard shard = (Shard)( bd1 instanceof Shard ? bd1 : bd2 );
            System.out.println("ended contact with shard");
            dreamWalkerScene.cancelShardPickup(shard);
            dreamWalkerScene.getAvatar().setHoverInteract(false);
            dreamWalkerScene.currentInteractingShard = null;
        }
        handleDoorEndContact((ObstacleSprite) bd1, (ObstacleSprite) bd2);
        handleDialougeEndContact((ObstacleSprite) bd1, (ObstacleSprite) bd2);
    }

    /** Handle collision between ShieldWall and Player */
    private void handleShieldWallContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 instanceof ShieldWall || bd2 instanceof ShieldWall) &&
            (bd1 instanceof Player || bd2 instanceof Player)){
            if(!playerHitShieldWall) {
                Player player = dreamWalkerScene.getAvatar();
                player.setFearMeter(Math.max(0, player.getFearMeter() - 1));
                playerHitShieldWall = true;
                System.out.println("ShieldWall hit Player: DAMAGE TAKEN");
            }
        }
    }
    /** Handle collision between ShieldWall and Player */
    private void handleShieldWallEndContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 instanceof ShieldWall || bd2 instanceof ShieldWall) &&
            (bd1 instanceof Player || bd2 instanceof Player)){
            playerHitShieldWall = false;
        }
    }


    /** Set door as active when contact between player and door is detected */
    private void handleDoorContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Door) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Door)) {
            if(bd1 instanceof Door){
                ((Door) bd1).setActive();
            }
            else{
                ((Door) bd2).setActive();
            }
        }
    }

    /** Set door as inactive when contact between player and door ends */
    private void handleDoorEndContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Door) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Door)) {
            if(bd1 instanceof Door){
                ((Door) bd1).setInactive();
            }
            else{
                ((Door) bd2).setInactive();
            }
        }
    }

    /** Set door as active when contact between player and door is detected */
    private void handleDialougeContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Door) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Door)) {
            if(bd1 instanceof Door){
                ((Door) bd1).setActive();
            }
            else{
                ((Door) bd2).setActive();
            }
        }
    }

    /** Set door as inactive when contact between player and door ends */
    private void handleDialougeEndContact(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Door) ||
            (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Door)) {
            if(bd1 instanceof Door){
                ((Door) bd1).setInactive();
            }
            else{
                ((Door) bd2).setInactive();
            }
        }
    }

    private void handleDreamShardContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2)
    {
        if (bd1 instanceof CuriosityCritter && bd2 instanceof Shard) {
            CuriosityCritter c = (CuriosityCritter)bd1;
            System.out.println("critter contact shard");
            if (c.inMoveTask) {
                Shard s = (Shard)bd2;
                c.giveShard(s);
                s.getObstacle().markRemoved(true);
                dreamWalkerScene.markShardRemoved(s.id);
            }
        }
        if (bd2 instanceof CuriosityCritter && bd1 instanceof Shard) {
            CuriosityCritter c = (CuriosityCritter)bd2;
            System.out.println("critter contact shard");
            if (c.inMoveTask) {
                Shard s = (Shard)bd1;
                c.giveShard(s);
                s.getObstacle().markRemoved(true);
                dreamWalkerScene.markShardRemoved(s.id);
            }
        }
        if ((bd1 == dreamWalkerScene.getAvatar() && bd2 instanceof Shard)
            || (bd2 == dreamWalkerScene.getAvatar() && bd1 instanceof Shard)
            && (!(dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) ||
            dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2))) &&
            (!("fall_sensor".equals(fd1) || "fall_sensor".equals(fd2)) ) &&
            !("player_sensor".equals(fd1) || "player_sensor".equals(fd2))) {

            Shard collectedShard = (bd1 instanceof Shard) ? (Shard) bd1 : (Shard) bd2;

            if (!collectedShard.getObstacle().isRemoved()) {
                dreamWalkerScene.currentInteractingShard = collectedShard;
                dreamWalkerScene.getAvatar().setHoverInteract(true);
                dreamWalkerScene.registerShardForPickup(collectedShard);
            }
        }
    }

    private void handleWalkSensorContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        if (("walk_sensor".equals(fd1) && (bd2 instanceof Surface || bd2 instanceof Enemy)) ||
            ("walk_sensor".equals(fd2) && (bd2 instanceof Surface || bd2 instanceof Enemy))) {

            // Ensure the Enemy reference is correctly retrieved
            Enemy e = (bd1 instanceof Enemy) ? (Enemy) bd1
                : (bd2 instanceof Enemy) ? (Enemy) bd2
                    : null;

            if (e != null) {
                e.setSeesWall(true);
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
        if (bd1 instanceof Enemy && bd2 instanceof Surface || bd2 instanceof Enemy && bd1 instanceof Surface) {
            if ("ground_sensor".equals(fd2) || "ground_sensor".equals(fd1)) {
                Enemy enemy;
                if (bd1 instanceof Enemy) {
                    enemy = (Enemy) bd1;
                } else {
                    enemy = (Enemy) bd2;
                }
                System.out.println("Enemy hit ground sensor");
                enemy.setGrounded(true);
            }
        }

    }

    private void handleHarvestingCollision(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        // if there is a collision between an enemy and the player's scare sensor
        if(( (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) && bd2 instanceof Enemy)
            || (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) && bd1 instanceof Enemy) )){
            System.out.println("FD1: " + fd1 + " FD2: " + fd2);
            Enemy harvestedEnemy;
                if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1)){
                    harvestedEnemy = (Enemy) bd2;
                    if (harvestedEnemy instanceof CuriosityCritter) {
                        CuriosityCritter critter = (CuriosityCritter) harvestedEnemy;
                        if (critter.heldShard != null) {
                            dreamWalkerScene.queueShardSpawn(
                                critter.getObstacle().getPosition(),
                                critter.dropShard()
                            );
                        }
                        dreamWalkerScene.performHarvest(harvestedEnemy);

                    }
                    else {
                        dreamWalkerScene.performHarvest(harvestedEnemy);
                    }

                }
                else if (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2)) {
                    harvestedEnemy = (Enemy) bd1;
                    if (harvestedEnemy instanceof CuriosityCritter) {
                        CuriosityCritter critter = (CuriosityCritter) harvestedEnemy;
                        if (critter.heldShard != null) {
                            dreamWalkerScene.queueShardSpawn(
                                critter.getObstacle().getPosition(),
                                critter.dropShard()
                            );
                        }
                        dreamWalkerScene.performHarvest(harvestedEnemy);

                    }
                    else {
                        dreamWalkerScene.performHarvest(harvestedEnemy);
                    }

                }
        }
    }

    private void handleHarvestingEndContact(Object bd1, Object bd2, Object fd1, Object fd2) {
        if ((dreamWalkerScene.getAvatar().getScareSensorName().equals(fd1) && bd2 instanceof Enemy) ||
            (dreamWalkerScene.getAvatar().getScareSensorName().equals(fd2) && bd1 instanceof Enemy)) {

            Enemy harvestedEnemy;
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
        if (bd1 instanceof Enemy && bd2 instanceof Surface || bd2 instanceof Enemy && bd1 instanceof Surface) {
            if ("ground_sensor".equals(fd2) || "ground_sensor".equals(fd1)) {
                Enemy enemy;
                if (bd1 instanceof Enemy) {
                    enemy = (Enemy) bd1;
                } else {
                    enemy = (Enemy) bd2;
                }
                enemy.setGrounded(false);
            }
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    private void handleFallSensorContact(ObstacleSprite bd1,
                                         ObstacleSprite bd2,
                                         Object fd1,
                                         Object fd2) {
        // Look for the fall_sensor on the player hitting any Surface
        if (("fall_sensor".equals(fd1) && bd2 instanceof Surface) ||
            ("fall_sensor".equals(fd2) && bd1 instanceof Surface)) {
            Player player = dreamWalkerScene.getAvatar();
            player.setFallSensorContact(true);
        }
    }

    /**
     * Called when the player's fall_sensor stops touching the ground.
     */
    private void handleFallSensorEndContact(ObstacleSprite bd1,
                                            ObstacleSprite bd2,
                                            Object fd1,
                                            Object fd2) {
        if (("fall_sensor".equals(fd1) && bd2 instanceof Surface) ||
            ("fall_sensor".equals(fd2) && bd1 instanceof Surface)) {
            Player player = dreamWalkerScene.getAvatar();
            player.setFallSensorContact(false);

        }
    }

    private void handleSpearHitPlayer(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 instanceof Spear && bd2 instanceof Player) || (bd2 instanceof Spear && bd1 instanceof Player)) {
            Spear spear = (bd1 instanceof Spear) ? (Spear) bd1 : (Spear) bd2;
            Player player = (bd1 instanceof Player) ? (Player) bd1 : (Player) bd2;

            player.setBlinded(true);
            player.setBlindTimer(0);

            player.setFearMeter(Math.max(0,player.getFearMeter() - 1));
            System.out.println("Spear hit Player: DAMAGE TAKEN");

            spear.getObstacle().markRemoved(true);
            spear.getObstacle().setVX(0);
            spear.getObstacle().setVY(0);
        }
    }
    private void handleSpearHitSurface(ObstacleSprite bd1, ObstacleSprite bd2) {
        if ((bd1 instanceof Spear && bd2 instanceof Surface) || (bd2 instanceof Spear && bd1 instanceof Surface)) {
            Spear spear = (bd1 instanceof Spear) ? (Spear) bd1 : (Spear) bd2;

            spear.getObstacle().markRemoved(true);

            spear.getObstacle().setVX(0);
            spear.getObstacle().setVY(0);

            System.out.println("Spear hit Surface: removed spear.");
        }
    }

    private void handleSpikeContact(ObstacleSprite bd1, ObstacleSprite bd2, Object fd1, Object fd2) {
        if ((bd1 instanceof DayglowSpike && bd2 instanceof Player && !dreamWalkerScene.getAvatar().getSensorName().equals(fd2)) ||
            (bd2 instanceof DayglowSpike && bd1 instanceof Player && !dreamWalkerScene.getAvatar().getSensorName().equals(fd1))){

            System.out.println(fd1 + " " + fd2);
            DayglowSpike spike = (bd1 instanceof DayglowSpike) ? (DayglowSpike) bd1 : (DayglowSpike) bd2;
            Player player = (bd1 instanceof Player) ? (Player) bd1 : (Player) bd2;

            player.setKnockingBack(true, spike.getObstacle().getPosition());
        }
    }
}
