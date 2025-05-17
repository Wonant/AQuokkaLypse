
package edu.cornell.cis3152.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;
import edu.cornell.cis3152.physics.platform.MindMaintenance;
import edu.cornell.cis3152.physics.platform.Player;
import edu.cornell.cis3152.physics.platform.DreamDweller;

import edu.cornell.gdiac.assets.AssetDirectory;

import java.util.*;

public class AIControllerManager {

    private List<EnemyAI> entities;
    private Player player;
    private Random random;
    private AssetDirectory asset_directory;
    private World world;

    public AIControllerManager(Player player, AssetDirectory directory, World world) {
        entities = new ArrayList<>();
        random = new Random();
        asset_directory = directory;
        this.player = player;
        this.world = world;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private Vector2 getPlayerPosition(Player player) {
        return new Vector2(player.getObstacle().getX(), player.getObstacle().getY());
    }


    /**  Enemy FSM states */

    private enum MaintenanceFSM {
        /**mind maintenance just spawned - will often immediately go to next state*/
        START,
        /**mind maintenance is moving to a short location nearby idly, distance randomly from set interval*/
        IDLE_WALK,
        /**mind maintenance is now alerted to player's presence, will follow and stare at them*/
        ATTACK,
        /** mind maintenance is stunned, cannot move or see (do damage to player) */
        STUNNED,
        /** mind maintenance chases player after it shoots */
        CHASING,
        /** normal turn */
        TURN,
        /** mind maintenance is alerted when player is detected from behind */
        ALERT,
        /** faster turn when mind maintenance is sus */
        ALERT_TURN,
        /** faster walk when mind maintenance is sus */
        ALERT_WALK
    }

    private enum DwellerFSM{
        /**dream dweller just spawned - will often immediately go to next state*/
        START,
        /**dream dweller is idly looking around, but not moving*/
        IDLE_LOOK,
        /** dream dweller is stunned, cannot move or see (do damage to player) */
        STUNNED,
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
    public void register(MindMaintenance entity) {
        entities.add(new MaintenanceAI(entity));
    }
    public void register(DreamDweller entity) {
        entities.add(new DwellerAI(entity));
    }

    public void register(Enemy entity) {
        if (entity instanceof MindMaintenance) {
            entities.add(new MaintenanceAI((MindMaintenance) entity));
        } else if (entity instanceof DreamDweller) {
            entities.add(new DwellerAI((DreamDweller) entity));
        }
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
            if (enemy.enemy.getClass() == MindMaintenance.class){
                MaintenanceAI maintenanceAI = (MaintenanceAI) enemy;
                updateMaintenance(maintenanceAI, dt);
            }
            else if (enemy.enemy.getClass() == DreamDweller.class) {
                DwellerAI dwellerAI = (DwellerAI) enemy;
                updateDweller(dwellerAI, dt);
            }
        }
    }

    private Vector2 getMaintenancePosition(MindMaintenance maintenance) {
        return new Vector2(maintenance.getObstacle().getX(), maintenance.getObstacle().getY());
    }

    private void updateMaintenance(MaintenanceAI data, float dt) {
        data.stateTimer += dt;
        boolean seesPlayer = data.maintenance.isAwareOfPlayer();
        boolean isStunned = data.maintenance.isStunned();

        // if the enemy is stunned, transition to the stunned state
        if (isStunned) {
            if (data.state != MaintenanceFSM.STUNNED) {
                transitionMaintenanceState(data, MaintenanceFSM.STUNNED);
            }
            data.maintenance.setMovement(0);

            if (data.stateTimer > data.stateDuration) {
                System.out.println("Maintenance stun wears off");
                data.maintenance.setStunned(false);
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
            }
        }
        else if (seesPlayer && data.state != MaintenanceFSM.ATTACK){
            transitionMaintenanceState(data, MaintenanceFSM.ATTACK);
        }
        else if (data.maintenance.isSus()){
            data.maintenance.setSus(false);
            data.maintenance.resetAttackSprite();
            transitionMaintenanceState(data, MaintenanceFSM.TURN);
        }

        if (data.state == MaintenanceFSM.START) {
            transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
        }

        else if (data.state == MaintenanceFSM.IDLE_WALK) {
            if (data.stateTimer > data.stateDuration) {
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
            } else if (data.maintenance.isSeesWall() || !data.maintenance.isSafeToWalk()) {
                transitionMaintenanceState(data, MaintenanceFSM.TURN);
                System.out.println("Turn TRIGGERED");
            } else {
                // Walk in a direction, will have already known if wall is in front
                data.maintenance.setMovement(data.movingRight? 2 : -2);
                data.maintenance.applyForce();
            }
        }

        else if (data.state == MaintenanceFSM.TURN) {
            data.maintenance.setMovement(0);
            data.maintenance.applyForce();

            if (data.stateTimer > data.stateDuration) {
                data.maintenance.getObstacle();
                data.maintenance.setTurning(false);
                data.maintenance.resetTurnSprite();
                data.maintenance.turnShift();
                data.movingRight = !data.movingRight;
                data.maintenance.setMovement(data.movingRight? 2 : -2);
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
            }
        }

        else if (data.state == MaintenanceFSM.ATTACK) {
            data.maintenance.setMovement(0);
            data.maintenance.getObstacle().setVX(0);
            if (data.stateTimer > data.stateDuration) {
                if (seesPlayer){
                    System.out.println("Reached 1");
                    data.maintenance.resetAttackSprite();
                    transitionMaintenanceState(data, MaintenanceFSM.ATTACK);
                }
                else{
                    System.out.println("Reached 2");
                    data.maintenance.resetAttackSprite();
                    data.maintenance.setAttacking(false);
                    data.maintenance.setMovement(data.movingRight? 2 : -2);
                    transitionMaintenanceState(data, MaintenanceFSM.CHASING);
                }
            }
        }

        if (data.state == MaintenanceFSM.CHASING) {
            data.maintenance.setMovement(data.movingRight? 2 : -2);
            data.maintenance.applyForce();
            if (data.stateTimer > data.stateDuration) {
                data.maintenance.setChasing(false);
                transitionMaintenanceState(data, MaintenanceFSM.IDLE_WALK);
            }
            else if (data.maintenance.isSeesWall() || !data.maintenance.isSafeToWalk()) {
                data.maintenance.setMovement(0);
                data.maintenance.getObstacle().setVX(0);
                data.maintenance.setChasing(false);
                transitionMaintenanceState(data, MaintenanceFSM.TURN);
                System.out.println("Turn TRIGGERED");
            } else {
                // Walk in a direction, will have already known if wall is in front
                data.maintenance.setMovement(data.movingRight? 12 : -12);
                data.maintenance.applyForce();
            }
        }

    }

    private void transitionMaintenanceState(MaintenanceAI data, MaintenanceFSM newState) {
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case TURN:
                data.stateDuration = 1.4f;
                data.maintenance.setTurning(true);
                data.maintenance.setSeesWall(false);
                break;
            case IDLE_WALK:
                data.stateDuration = 5.0f;
                data.horizontal = data.movingRight ? 2.0f : -2.0f;
                break;

            case ATTACK:
                data.stateDuration = 1.4f;
                data.maintenance.setAttacking(true);
                break;

            case CHASING:
                data.stateDuration = 6.0f;
                data.maintenance.setChasing(true);
                break;

            case STUNNED:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;

            case ALERT:
                data.stateDuration = 3;
                break;

            case ALERT_TURN:
                data.stateDuration = 1f;
                break;

            case ALERT_WALK:
                data.stateDuration = 6;
                data.maintenance.setChasing(true);
                break;

        }
    }


    private Vector2 getDwellerPosition(DreamDweller dweller) {
        return new Vector2(dweller.getObstacle().getX(), dweller.getObstacle().getY());
    }

    private void updateDweller(DwellerAI data, float dt) {
        data.stateTimer += dt;



        Vector2 dwellerPos = getDwellerPosition(data.dweller);
        Vector2 visionRef = new Vector2(dwellerPos.x, dwellerPos.y + 10.0f);
        boolean seesPlayer = data.dweller.isAwareOfPlayer();
        boolean isStunned = data.dweller.isStunned();

        // if the enemy is stunned, transition to the stunned state
        if (isStunned) {
            data.dweller.setMovement(0);
            if (data.state != DwellerFSM.STUNNED) {
                transitionDwellerState(data, DwellerFSM.STUNNED);
            }
            data.dweller.setMovement(0);
            data.dweller.applyForce();
            data.dweller.getObstacle().setVX(0);


            if (data.stateTimer > data.stateDuration) {
                System.out.println("Maintenance stun wears off");
                data.dweller.setActiveTexture(asset_directory);
                data.dweller.setStunned(false);
                transitionDwellerState(data, DwellerFSM.IDLE_LOOK);
            }
        }
        else{
            if (player != null) {
                Vector2 playerPos = getPlayerPosition(player);
                float distanceToPlayer = dwellerPos.dst(playerPos);
                if (seesPlayer) {
                    data.dweller.setShooting(true);
                }
                else{
                    data.dweller.setShooting(false);
                }
            }
        }


        if (data.state ==DwellerFSM.START) {
           transitionDwellerState(data, DwellerFSM.IDLE_LOOK);
        }

        if (data.state == DwellerFSM.IDLE_LOOK) {
                data.dweller.setMovement(0);
                data.dweller.applyForce();
            }
    }
    private void transitionDwellerState (DwellerAI data, DwellerFSM newState) {
        data.state = newState;
        data.stateTimer = 0;

        switch (newState) {
            case IDLE_LOOK:
                data.horizontal = 0;
                break;

            case STUNNED:
                data.stateDuration = 3.0f;
                data.horizontal = 0;
                break;
        }
    }
}
