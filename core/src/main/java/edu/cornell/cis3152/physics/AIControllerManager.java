package edu.cornell.cis3152.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sun.tools.javac.Main;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;
import edu.cornell.cis3152.physics.platform.MindMaintenance;
import edu.cornell.cis3152.physics.platform.Player;

import java.util.*;

public class AIControllerManager {


    // unless we have a very large amount of enemies on screen, O(n) for checks is fine. but if its slow change (don't use List).
    private List<EnemyAI> entities;
    private Player player;
    private Random random;

    public AIControllerManager(Player player) {
        entities = new ArrayList<>();
        random = new Random();
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
        STUNNED
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



    // internal data for each critter state
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


    /** we need to make a wrapper class eventually for all ai-controlled enemies */
    public void register(CuriosityCritter entity) {
        entities.add(new CritterAI(entity));
    }
    public void register(MindMaintenance entity) {
        entities.add(new MaintenanceAI(entity));
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
            transitionCritterState(data, CritterFSM.STUNNED);
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
                data.stateDuration = 10.0f;
                data.horizontal = 0;
        }
    }
    private void updateMaintenance(MaintenanceAI data, float dt) {

    }

    private void transitionMaintenanceState(MaintenanceAI data, MaintenanceFSM newState) {

    }



}
