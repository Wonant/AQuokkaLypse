package edu.cornell.cis3152.physics;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Player;

import java.util.*;

public class AIControllerManager {
    private enum CritterFSM {
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

    public AIControllerManager() {
        entities = new ArrayList<>();
        random = new Random();
    }

    /** we need to make a wrapper class eventually for all ai-controlled enemies */
    public void register(CuriosityCritter entity) {
        entities.add(new CritterAI(entity));
    }

    public void unregister(CuriosityCritter entity) {
        entities.removeIf(data -> data.critter == entity);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void update(float dt) {
        for (CritterAI data : entities) {
            updateCritter(data, dt);
        }
    }

    private Vector2 getCritterPosition(CuriosityCritter critter) {
        return new Vector2(critter.getObstacle().getX(), critter.getObstacle().getY());
    }

    private void updateCritter(CritterAI data, float dt) {
        data.stateTimer += dt;
        Vector2 critterPos = getCritterPosition(data.critter);
        boolean seesPlayer = data.critter.isAwareOfPlayer();

        if (player != null) {
            Vector2 playerPos = player.getObstacle().getPosition();
            float distanceToPlayer = critterPos.dst(playerPos);

            // If the critter sees the player, transition to ALERTED
            if (seesPlayer && data.state != CritterFSM.AWARE_STARE && data.state != CritterFSM.AWARE_FEAR) {
                if (data.state != CritterFSM.ALERTED) {
                    System.out.println("Critter sees player! Transitioning to ALERTED.");
                    transitionState(data, CritterFSM.ALERTED);
                    return;
                }
            }


            if (data.state == CritterFSM.ALERTED) {
                transitionState(data, CritterFSM.AWARE_STARE);
                return;
            }

            if (data.state == CritterFSM.AWARE_FEAR) {
                // Flee in the opposite direction
                float fleeSpeed = 3.0f;
                if (playerPos.x < critterPos.x) {
                    data.horizontal = fleeSpeed;
                } else {
                    data.horizontal = -fleeSpeed;
                }

                if (data.stateTimer > 3.0f) { // Example: After 3 seconds of fleeing, return to idle
                    transitionState(data, CritterFSM.IDLE_LOOK);
                }
            }

            if (data.state == CritterFSM.AWARE_STARE) {
                // Vision follows the player
                float angleToPlayer = playerPos.sub(critterPos).angleDeg();
                data.critter.setVisionAngle(angleToPlayer);

                if (!seesPlayer) {
                    transitionState(data, CritterFSM.IDLE_LOOK);
                }
            }
        }

        if (data.state == CritterFSM.START) {
            transitionState(data, random.nextBoolean() ? CritterFSM.IDLE_LOOK : CritterFSM.IDLE_WALK);
        }

        if (data.state == CritterFSM.IDLE_LOOK) {
            if (data.stateTimer > data.stateDuration) {
                transitionState(data, CritterFSM.IDLE_WALK);
            } else {
                // Look straight for now
                data.critter.setVisionAngle(data.movingRight ? 290 : 70);
                data.critter.setMovement(0);
                data.critter.applyForce();
            }
        }

        if (data.state == CritterFSM.IDLE_WALK) {
            if (data.stateTimer > data.stateDuration) {
                transitionState(data, CritterFSM.IDLE_LOOK);
            } else {
                // Walk in a direction
                data.critter.setVisionAngle(data.movingRight ? 270 : 90);
                data.critter.setMovement(data.horizontal);
                data.critter.applyForce();
            }
        }
        System.out.println("Critter state = " + data.state);
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
                data.movingRight = random.nextBoolean();
                data.horizontal = data.movingRight ? 1.0f : -1.0f;
                break;

            case ALERTED:
                data.stateDuration = 1.0f;
                data.horizontal = 0;
                break;

            case AWARE_STARE:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;

            case AWARE_FEAR:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;
        }
    }
}
