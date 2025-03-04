package edu.cornell.cis3152.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Player;

import java.util.*;

public class AIControllerManager {
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
    }

    // internal data for each critter state - may need to refactor this when adding otehr enemies
    private class CritterAI {
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
            this.critter = critter;
            this.state = CritterFSM.START;
            this.stateDuration = 0;
            this.stateTimer = 0;
            this.horizontal = 0;
            this.jump = false;
            this.targetPosition = null;
            this.movingRight = true;
        }
    }

    // unless we have a shit ton of enemies on screen, O(n) for checks is fine. but if its slow change(dont use List).
    private List<CritterAI> entities;
    private Player player;
    private Random random;

    public AIControllerManager(Player player) {
        entities = new ArrayList<>();
        random = new Random();
        this.player = player;
    }

    /** we need to make a wrapper class eventually for all ai-controlled enemies */
    public void register(CuriosityCritter entity) {
        entities.add(new CritterAI(entity));
    }

    public void unregister(CuriosityCritter entity) {
        entities.removeIf(data -> data.critter == entity);
    }

    public void update(float dt) {
        for (CritterAI data : entities) {
            updateCritter(data, dt);
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private Vector2 getPlayerPosition(Player player) {
        return new Vector2(player.getObstacle().getX(), player.getObstacle().getY());
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

        if (player != null) {
            Vector2 playerPos = getPlayerPosition(player);
            float distanceToPlayer = critterPos.dst(playerPos); // maybe needing for how long the critter should flee
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

                    float angleToPlayer = MathUtils.atan2(playerPos.y, playerPos.x) - MathUtils.atan2(visionRef.y, visionRef.x);
                    angleToPlayer *= MathUtils.radiansToDegrees;

                    System.out.println("in stare mode, angle:" + angleToPlayer);
                    data.critter.setVisionAngle(angleToPlayer);
                    return;
                }

                if (data.state == CritterFSM.IDLE_LOOK || data.state == CritterFSM.IDLE_WALK) {
                    System.out.println("Critter sees player, alerted");
                    transitionState(data, CritterFSM.ALERTED);
                    return;
                }
                if (data.state == CritterFSM.ALERTED) {
                    // change for testing
                    System.out.println(player.getFearMeter());
                    if (player.getFearMeter() < 0.75 * player.getMaxFearMeter()) {

                        transitionState(data, CritterFSM.AWARE_STARE);
                    } else {
                        transitionState(data, CritterFSM.AWARE_FEAR);
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

                if (data.stateTimer > 3.0f) {
                    transitionState(data, CritterFSM.IDLE_LOOK);
                }
                //System.out.println("Scared!");
                return;
            }
        }

        if (data.state == CritterFSM.AWARE_STARE) {
            if (data.stateTimer > data.stateDuration) {
                transitionState(data, CritterFSM.IDLE_LOOK);
            }
        }

        if (data.state == CritterFSM.START) {
            transitionState(data, random.nextBoolean() ? CritterFSM.IDLE_LOOK : CritterFSM.IDLE_WALK);
        }

        if (data.state == CritterFSM.IDLE_LOOK) {
            System.out.println("in idle");
            if (data.stateTimer > data.stateDuration) {
                transitionState(data, CritterFSM.IDLE_WALK);
            } else {
                // Look straight for now
                data.critter.setVisionAngle(data.movingRight ? 270 : 90);
                data.critter.setMovement(0);
                data.critter.applyForce();
            }
        }

        if (data.state == CritterFSM.IDLE_WALK) {
            if (data.stateTimer > data.stateDuration) {
                transitionState(data, CritterFSM.IDLE_LOOK);
            } else if (data.critter.isSeesWall()) {
                data.critter.setMovement(0);
                data.critter.applyForce();
                transitionState(data, CritterFSM.IDLE_LOOK);
            } else {
                // Walk in a direction, will ahve already known if wall is infront

                data.critter.setVisionAngle(data.movingRight ? 270 : 90);
                data.critter.setMovement(data.horizontal);
                data.critter.applyForce();
            }
        }
        System.out.println(data.state);

    }

    private void transitionState(CritterAI data, CritterFSM newState) {
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case IDLE_LOOK:
                data.stateDuration = random.nextFloat() * 3.0f + 2.0f; // 2-5 seconds
                data.horizontal = 0;
                break;

            case IDLE_WALK:
                data.stateDuration = random.nextFloat() * 2.0f + 1.0f;
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
        }
    }
}
