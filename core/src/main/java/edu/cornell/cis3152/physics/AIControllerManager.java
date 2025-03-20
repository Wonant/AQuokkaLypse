
package edu.cornell.cis3152.physics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sun.tools.javac.Main;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;
import edu.cornell.cis3152.physics.platform.MindMaintenance;
import edu.cornell.cis3152.physics.platform.Player;
import edu.cornell.cis3152.physics.platform.DreamDweller;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import java.util.*;

public class AIControllerManager {

    // unless we have a very large amount of enemies on screen, O(n) for checks is fine. but if its slow change (don't use List).
    private List<EnemyAI> entities;
    private Player player;
    private Random random;
    private AssetDirectory asset_directory;

    public AIControllerManager(Player player, AssetDirectory directory) {
        entities = new ArrayList<>();
        random = new Random();
        asset_directory = directory;
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private Vector2 getPlayerPosition(Player player) {
        return new Vector2(player.getObstacle().getX(), player.getObstacle().getY());
    }


    /**  Enemy FSM states */
    private enum CritterFSM {
        // yes these are my comments -_- - andrew
        // here for convenience understanding critter behavior
        /**curiosity critter just spawned - will often immediately go to next state*/
        START,
        /**curiosity critter is idly looking around, but not moving*/
        IDLE_LOOK,
        /**curiosity critter is moving to a short location nearby idly, distance randomly from set interval*/
        IDLE_WALK,
        /**curiosity critter was previously in idle state, now alerted*/
        ALERTED,
        /**
         * curiosity critter was previously alerted, now aware of player location,
         * the player's fear meter is low so critter will lock in place and its vision follows player
         * until player exits vision range.
         */
        AWARE_STARE,
        /**
         * curiosity critter was previously alerted, now aware of player location,
         * the player's fear meter is high so critter will flee in terror
         */
        AWARE_FEAR,
        /** curiosity critter is stunned, cannot move or see (do damage to player) */
        STUNNED
    }
    private enum MaintenanceFSM {
        /**mind maintenance just spawned - will often immediately go to next state*/
        START,
        /**mind maintenance is idly looking around, but not moving*/
        IDLE_LOOK,
        /**mind maintenance is moving to a short location nearby idly, distance randomly from set interval*/
        IDLE_WALK,
        /**mind maintenance is now alerted to player's presence, will follow and stare at them*/
        ALERTED,
        /** mind maintenance is stunned, cannot move or see (do damage to player) */
        STUNNED,
        CHASING
    }

    private enum DwellerFSM{
        /**dream dweller just spawned - will often immediately go to next state*/
        START,
        /**dream dweller is idly looking around, but not moving*/
        IDLE_LOOK,
        /**dream dweller is now alerted to player's presence,will alert nearby enemies*/
        ALERT,
        /** dream dweller is stunned, cannot move or do damage to player */
        STUNNED     // Temporarily immobilized by player's stun
    }


    private class EnemyAI {
        float stateTimer;
        float stateDuration;
        float horizontal;
        // critters should only jump if their pathing requires it
        boolean jump;
        Vector2 targetPosition;
        boolean movingRight;
        Enemy enemy;

        public EnemyAI(Enemy enemy) {
            this.stateDuration = 0;
            this.stateTimer = 0;
            this.horizontal = 0;
            this.jump = false;
            this.targetPosition = null;
            this.movingRight = true;
            this.enemy = enemy;
        }
    }

    private class CritterAI extends EnemyAI{
        CuriosityCritter critter;
        CritterFSM state;
        float stateTimer;
        float stateDuration;
        float horizontal;
        // critters should only jump if their pathing requires it
        boolean jump;
        Vector2 targetPosition;
        boolean movingRight;

        public CritterAI(CuriosityCritter critter) {
            super(critter);
            this.critter = critter;
            this.state = CritterFSM.START;
        }
    }
    // internal data for each maintenance state
    private class MaintenanceAI extends EnemyAI{
        MindMaintenance maintenance;
        MaintenanceFSM state;
        float stateTimer;
        float stateDuration;
        float horizontal;
        // critters should only jump if their pathing requires it
        boolean jump;
        Vector2 targetPosition;
        boolean movingRight;

        public MaintenanceAI(MindMaintenance maintenance) {
            super(maintenance);
            this.maintenance = maintenance;
            this.state = MaintenanceFSM.START;
        }
    }

    private class DwellerAI extends EnemyAI {
        DreamDweller dweller;
        DwellerFSM state;
        float stateTimer;
        float stateDuration;
        boolean movingRight;
        Vector2 targetPosition;
        boolean jump;

        public DwellerAI(DreamDweller dweller) {
            super(dweller);
            this.dweller = dweller;
            this.state = DwellerFSM.START;
        }
    }


    /** we need to make a wrapper class eventually for all ai-controlled enemies */
    public void register(CuriosityCritter entity) {
        entities.add(new CritterAI(entity));
    }
    public void register(MindMaintenance entity) {
        entities.add(new MaintenanceAI(entity));
    }
    public void register(DreamDweller entity) {
        entities.add(new DwellerAI(entity));
    }


    public void unregister(CuriosityCritter entity) {
        entities.removeIf(data -> data.enemy == entity);
    }

    /**
     * update every enemy in entities according to their type
     * @param dt: the time step
     * */
    public void update(float dt) {
        // iterate through the enemies
        for (EnemyAI enemy : entities) {
            if (enemy.enemy.getClass() == CuriosityCritter.class){
                CritterAI critter = (CritterAI) enemy;
                updateCritter(critter, dt);

            }
            else if (enemy.enemy.getClass() == MindMaintenance.class){
                MaintenanceAI maintenance = (MaintenanceAI) enemy;
                updateMaintenance(maintenance, dt);
            }
            else if (enemy.enemy.getClass() == DreamDweller.class) {
                DwellerAI dweller = (DwellerAI) enemy;
                updateDweller(dweller, dt);
            }
        }
    }


    private Vector2 getCritterPosition(CuriosityCritter critter) {
        return new Vector2(critter.getObstacle().getX(), critter.getObstacle().getY());
    }

    private void updateCritter(CritterAI data, float dt) {
        data.stateTimer += dt;

        // quick comment on how the angle works:
        // 0 is straight above the critter. so that means 180 means its look straight down
        Vector2 critterPos = getCritterPosition(data.critter);
        Vector2 visionRef = new Vector2(critterPos.x, critterPos.y + 10.0f);
        boolean seesPlayer = data.critter.isAwareOfPlayer();
        boolean isStunned = data.critter.isStunned();

        // if the enemy is stunned, transition to the stunned state
        if (isStunned) {
            if (data.state != CritterFSM.STUNNED) {
                transitionCritterState(data, CritterFSM.STUNNED);
            }
            data.critter.getObstacle().setVX(0);

            if (data.stateTimer > data.stateDuration) {
                System.out.println("Critter stun wears off");
                data.critter.setActiveTexture(asset_directory);
                data.critter.setStunned(false);
                transitionCritterState(data, CritterFSM.IDLE_LOOK);
            }
        }
        else{
            if (player != null) {
                Vector2 playerPos = getPlayerPosition(player);
                float distanceToPlayer = critterPos.dst(
                    playerPos); // maybe needing for how long the critter should flee
                //            System.out.println(playerPos.x + " " + critterPos.x);
                //d If the critter sees the player, transition to ALERTED
                if (seesPlayer) {
                    data.stateTimer = 0; //reset for all states
                    if (data.state == CritterFSM.AWARE_STARE) {

                        // ig we stay in it for now
                        // this might not work
                        data.critter.setMovement(0);
                        data.critter.applyForce();

                        // math - with critter as center
                        // arctan point 1 - center normalized - arctan point 2 - center normalized = angle in radians
                        visionRef.sub(critterPos).nor();
                        playerPos.sub(critterPos).nor();

                        float angleToPlayer =
                            MathUtils.atan2(playerPos.y, playerPos.x) - MathUtils.atan2(visionRef.y,
                                visionRef.x);
                        angleToPlayer *= MathUtils.radiansToDegrees;

                        //System.out.println("in stare mode, angle:" + angleToPlayer);
                        data.critter.setVisionAngle(angleToPlayer);
                        return;
                    }

                    if (data.state == CritterFSM.IDLE_LOOK || data.state == CritterFSM.IDLE_WALK) {
                        //System.out.println("Critter sees player, alerted");
                        transitionCritterState(data, CritterFSM.ALERTED);
                        return;
                    }
                    if (data.state == CritterFSM.ALERTED) {
                        // change for testing
                        data.critter.setMovement(0);
                        data.critter.applyForce();
                        //System.out.println(player.getFearMeter());
                        if (player.getFearMeter() < 0.75 * player.getMaxFearMeter()) {

                            transitionCritterState(data, CritterFSM.AWARE_STARE);
                        } else {
                            transitionCritterState(data, CritterFSM.AWARE_FEAR);
                        }
                        return;
                    }
                    if (data.state == CritterFSM.AWARE_FEAR) {

                        float fleeSpeed = 1.5f;
                        if (playerPos.x < critterPos.x) {
                            data.horizontal = fleeSpeed;
                            data.movingRight = !data.movingRight;
                            data.critter.setMovement(data.horizontal);
                            data.critter.applyForce();
                        } else {
                            data.horizontal = -fleeSpeed;
                            data.movingRight = !data.movingRight;
                            data.critter.setMovement(data.horizontal);
                            data.critter.applyForce();
                        }
                        //System.out.println("Scared!");
                        return;
                    }

                }

                if (data.state == CritterFSM.AWARE_FEAR) {

                    float fleeSpeed = 1.5f;
                    if (playerPos.x < critterPos.x) {
                        data.horizontal = fleeSpeed;
                        data.movingRight = !data.movingRight;
                        data.critter.setMovement(data.horizontal);
                        data.critter.applyForce();
                    } else {
                        data.horizontal = -fleeSpeed;
                        data.movingRight = !data.movingRight;
                        data.critter.setMovement(data.horizontal);
                        data.critter.applyForce();
                    }

                    if (data.stateTimer > data.stateDuration) {
                        transitionCritterState(data, CritterFSM.IDLE_LOOK);
                    }
                    //System.out.println("Scared!");
                    return;
                }
            }
        }

        if (data.state == CritterFSM.AWARE_STARE) {
            if (data.stateTimer > data.stateDuration) {
                transitionCritterState(data, CritterFSM.IDLE_LOOK);
            }
        }

        if (data.state == CritterFSM.START) {
            transitionCritterState(data, random.nextBoolean() ? CritterFSM.IDLE_LOOK : CritterFSM.IDLE_WALK);
        }

        if (data.state == CritterFSM.IDLE_LOOK) {
            //System.out.println("in idle");
            if (data.stateTimer > data.stateDuration) {
                transitionCritterState(data, CritterFSM.IDLE_WALK);
            } else {
                // Look straight for now
                data.critter.setVisionAngle(data.movingRight ? 270 : 90);
                data.critter.setMovement(0);
                data.critter.applyForce();
            }
        }

        if (data.state == CritterFSM.IDLE_WALK) {
            if (data.stateTimer > data.stateDuration) {
                transitionCritterState(data, CritterFSM.IDLE_LOOK);
            } else if (data.critter.isSeesWall()) {
                data.critter.setMovement(0);
                data.critter.applyForce();
                transitionCritterState(data, CritterFSM.IDLE_LOOK);
            } else {
                // Walk in a direction, will have already known if wall is in front
                data.critter.setVisionAngle(data.movingRight ? 270 : 90);
                data.critter.setMovement(data.horizontal);
                data.critter.applyForce();
            }
        }
        //System.out.println(data.state);

    }

    private void transitionCritterState(CritterAI data, CritterFSM newState) {
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case IDLE_LOOK:
                data.stateDuration = random.nextFloat() * 2.0f + 1.0f; // 1-3 seconds
                data.horizontal = 0;
                break;

            case IDLE_WALK:
                data.stateDuration = random.nextFloat() + 1.0f; // 1-2 seconds
                if (data.critter.isSeesWall()) {
                    data.movingRight = !data.movingRight;
                    data.critter.setSeesWall(false);
                } else {
                    data.movingRight = random.nextBoolean();
                }
                data.horizontal = data.movingRight ? 1.0f : -1.0f;
                break;

            case ALERTED:
                data.stateDuration = 1.0f;
                data.horizontal = 0;
                break;

            case AWARE_STARE:
                data.stateDuration = 2.0f;
                data.horizontal = 0;
                break;

            case AWARE_FEAR:
                data.stateDuration = 1.8f;
                data.horizontal = 0;
                break;

            case STUNNED:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;
        }
    }

    private Vector2 getMaintenancePosition(MindMaintenance maintenance) {
        return new Vector2(maintenance.getObstacle().getX(), maintenance.getObstacle().getY());
    }

    private void updateMaintenance(MaintenanceAI data, float dt) {
        data.stateTimer += dt;

        // quick comment on how the angle works:
        // 0 is straight above the critter. so that means 180 means its look straight down
        Vector2 maintenancePos = getMaintenancePosition(data.maintenance);
        Vector2 visionRef = new Vector2(maintenancePos.x, maintenancePos.y + 10.0f);
        boolean seesPlayer = data.maintenance.isAwareOfPlayer();
        boolean isStunned = data.maintenance.isStunned();

        // if the enemy is stunned, transition to the stunned state
        if (isStunned) {
            if (data.state != MaintenanceFSM.STUNNED) {
                transitionMaintenanceState(data, MaintenanceFSM.STUNNED);
            }
            data.maintenance.getObstacle().setVX(0);

            if (data.stateTimer > data.stateDuration) {
                System.out.println("Maintenance stun wears off");
                data.maintenance.setActiveTexture(asset_directory);
                data.maintenance.setStunned(false);
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_LOOK);
            }
        }
        else{
            if (player != null) {
                Vector2 playerPos = getPlayerPosition(player);
                float distanceToPlayer = maintenancePos.dst(
                    playerPos); // maybe needing for how long the critter should flee
                //            System.out.println(playerPos.x + " " + critterPos.x);
                //d If the critter sees the player, transition to ALERTED
                if (seesPlayer) {
                    data.stateTimer = 0; //reset for all states
                    if (data.state == MaintenanceFSM.CHASING) {

                        // ig we stay in it for now
                        // this might not work
                        data.maintenance.setMovement(0);
                        data.maintenance.applyForce();

                        // math - with critter as center
                        // arctan point 1 - center normalized - arctan point 2 - center normalized = angle in radians
                        visionRef.sub(maintenancePos).nor();
                        playerPos.sub(maintenancePos).nor();

                        float angleToPlayer =
                            MathUtils.atan2(playerPos.y, playerPos.x) - MathUtils.atan2(visionRef.y,
                                visionRef.x);
                        angleToPlayer *= MathUtils.radiansToDegrees;

                        //System.out.println("in stare mode, angle:" + angleToPlayer);
                        data.maintenance.setVisionAngle(angleToPlayer);
                    }

                    else if (data.state == MaintenanceFSM.IDLE_LOOK || data.state == MaintenanceFSM.IDLE_WALK) {
                        System.out.println("Maintenance sees player, alerted");
                        transitionMaintenanceState(data, MaintenanceFSM.ALERTED);
                    }
                    else if (data.state == MaintenanceFSM.ALERTED) {
                        // change for testing
                        data.maintenance.setMovement(0);
                        data.maintenance.applyForce();
                        transitionMaintenanceState(data, MaintenanceFSM.CHASING);

                    }

                }
            }
        }

        Vector2 playerPos = getPlayerPosition(player);
        if (data.state == MaintenanceFSM.CHASING) {
            if (data.stateTimer > data.stateDuration) {
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_LOOK);
            }
            else{
                float chaseSpeed = 10f;
                if (playerPos.x < maintenancePos.x) {
                    data.horizontal = -chaseSpeed;
                    data.movingRight = !data.movingRight;
                    data.maintenance.setMovement(data.horizontal);
                    data.maintenance.applyForce();
                } else {
                    data.horizontal = chaseSpeed;
                    data.movingRight = !data.movingRight;
                    data.maintenance.setMovement(data.horizontal);
                    data.maintenance.applyForce();
                }
            }
        }

        if (data.state == MaintenanceFSM.START) {
            transitionMaintenanceState(data, random.nextBoolean() ? MaintenanceFSM.IDLE_LOOK : MaintenanceFSM.IDLE_WALK);
        }

        if (data.state == MaintenanceFSM.IDLE_LOOK) {
            if (data.stateTimer > data.stateDuration) {
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
            } else {
                // Look straight for now
                data.maintenance.setVisionAngle(data.movingRight ? 270 : 90);
                data.maintenance.setMovement(0);
                data.maintenance.applyForce();
            }
        }

        if (data.state == MaintenanceFSM.IDLE_WALK) {
            if (data.stateTimer > data.stateDuration) {
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_LOOK);
            } else if (data.maintenance.isSeesWall()) {
                data.maintenance.setMovement(0);
                data.maintenance.applyForce();
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_LOOK);
            } else {
                // Walk in a direction, will have already known if wall is in front
                data.maintenance.setVisionAngle(data.movingRight ? 270 : 90);
                data.maintenance.setMovement(data.horizontal);
                data.maintenance.applyForce();
            }
        }
    }

    private void transitionMaintenanceState(MaintenanceAI data, MaintenanceFSM newState) {
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case IDLE_LOOK:
                data.stateDuration = random.nextFloat() * 2.0f + 1.0f; // 1-3 seconds
                data.horizontal = 0;
                break;

            case IDLE_WALK:
                data.stateDuration = random.nextFloat() + 1.0f; // 1-2 seconds
                if (data.maintenance.isSeesWall()) {
                    data.movingRight = !data.movingRight;
                    data.maintenance.setSeesWall(false);
                } else {
                    data.movingRight = random.nextBoolean();
                }
                data.horizontal = data.movingRight ? 1.0f : -1.0f;
                break;

            case ALERTED:
                data.stateDuration = 1.0f;
                data.horizontal = 10f;
                break;

            case CHASING:
                data.stateDuration = 4.0f;
                data.horizontal = 10f;
                break;

            case STUNNED:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;
        }
    }


    private Vector2 getDwellerPosition(DreamDweller dweller) {
        return new Vector2(dweller.getObstacle().getX(), dweller.getObstacle().getY());
    }

    private void updateDweller(DwellerAI data, float dt) {
        data.stateTimer += dt;

        Vector2 dwellerPos = getDwellerPosition(data.dweller);
        boolean seesPlayer = data.dweller.isAwareOfPlayer();
        boolean isStunned = data.dweller.isStunned();

        data.dweller.setMovement(0);
        data.dweller.getObstacle().setVX(0);
        data.dweller.getObstacle().setVY(0);

        if (isStunned) {
            if (data.state != DwellerFSM.STUNNED) {
                transitionDwellerState(data, DwellerFSM.STUNNED);
            }
            data.dweller.getObstacle().setVX(0);

            if (data.stateTimer > data.stateDuration) {
                System.out.println("Dream Dweller recovers from stun.");
                data.dweller.setActiveTexture(asset_directory);
                data.dweller.setStunned(false);
                transitionDwellerState(data, DwellerFSM.IDLE_LOOK);
            }
            return;
        }

        if (seesPlayer) {
            if (data.state == DwellerFSM.IDLE_LOOK) {
                transitionDwellerState(data, DwellerFSM.ALERT);
            }

            if (player != null) {
                Vector2 playerPos = getPlayerPosition(player);
                float dx = playerPos.x - dwellerPos.x;
                float dy = playerPos.y - dwellerPos.y;

                float angleToPlayer = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

                angleToPlayer -= 90;

                data.dweller.setVisionAngle(angleToPlayer);

                System.out.println("Dream Dweller tracking player at angle: " + angleToPlayer);
            }
        } else {
            if (data.state == DwellerFSM.IDLE_LOOK) {
                if (data.stateTimer > data.stateDuration) {
                    transitionDwellerState(data, DwellerFSM.IDLE_LOOK);
                } else {
                    float time = data.stateTimer;
                    float swingRange = 60.0f;
                    float swingSpeed = 1.0f;
                    float newAngle = swingRange * MathUtils.sin(time * swingSpeed);

                    data.dweller.setVisionAngle(newAngle);
                }
            }
        }

        if (data.state == DwellerFSM.ALERT) {
            alertNearbyEnemies(data.dweller);
            transitionDwellerState(data, DwellerFSM.IDLE_LOOK);
        }

        data.dweller.applyForce();

    }
    private void transitionDwellerState (DwellerAI data, DwellerFSM newState) {
        data.state = newState;
        data.stateTimer = 0;
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case IDLE_LOOK:
                break;

            case ALERT:
                data.stateDuration = 5.0f;
                break;

            case STUNNED:
                data.stateDuration = 4.0f; // Harder to stun than other enemies
                break;
        }
    }
    private void alertNearbyEnemies(DreamDweller dweller) {
        Vector2 dwellerPos = getDwellerPosition(dweller);
        float alertRadius = dweller.getAlertRadius(); // 从 DreamDweller 获取警报半径

        System.out.println("Dream Dweller alerting nearby enemies within radius: " + alertRadius);

        for (EnemyAI enemyAI : entities) {
            if (enemyAI.enemy == dweller) {
                continue;
            }

            Vector2 enemyPos = new Vector2(
                enemyAI.enemy.getObstacle().getX(),
                enemyAI.enemy.getObstacle().getY()
            );

            float distance = dwellerPos.dst(enemyPos);

            if (distance <= alertRadius) {
                if (enemyAI instanceof CritterAI) {
                    CritterAI critterAI = (CritterAI) enemyAI;


                    if (critterAI.state != CritterFSM.STUNNED) {
                        critterAI.critter.setAwareOfPlayer(true);
                        transitionCritterState(critterAI, CritterFSM.ALERTED);
                        System.out.println("Alerted a CuriosityCritter at distance: " + distance);
                    }
                } else if (enemyAI instanceof MaintenanceAI) {
                    MaintenanceAI maintenanceAI = (MaintenanceAI) enemyAI;

                    if (maintenanceAI.state != MaintenanceFSM.STUNNED) {
                        maintenanceAI.maintenance.setAwareOfPlayer(true);
                        transitionMaintenanceState(maintenanceAI, MaintenanceFSM.CHASING);
                        System.out.println("Alerted a MindMaintenance at distance: " + distance);
                    }
                }

            }
        }
    }
}
