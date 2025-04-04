package edu.cornell.cis3152.physics.platform.aibehavior;

import com.badlogic.gdx.Gdx;
import edu.cornell.cis3152.physics.AIControllerManager;
import edu.cornell.cis3152.physics.platform.*;

import com.badlogic.gdx.ai.btree.BehaviorTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

/**
 * Class for managing all the AI entities in one place
 */
public class AIManager {
    /** private mapping of id to their behavior loop*/
    private ArrayList<BehaviorLoop<Enemy>> entities;
    private int totalEntities;

    private Player player;

    private final Random random = new Random();

    private BehaviorFactory critterFactory;
    private BehaviorFactory dwellerFactory;
    private BehaviorFactory maintenanceFactory;

    public AIManager(String critterTreeFile, String dwellerTreeFile, String maintenanceTreeFile) {
        critterFactory = new BehaviorFactory<>(Gdx.files.internal(critterTreeFile));
        dwellerFactory = new BehaviorFactory<>(Gdx.files.internal(dwellerTreeFile));
        maintenanceFactory = new BehaviorFactory<>(Gdx.files.internal(maintenanceTreeFile));

        entities = new ArrayList<>();

        totalEntities = 0;
    }

    public void setPlayer(Player p) {
        player = p;
    }

    public void register(Enemy e) {
        BehaviorLoop<Enemy> behavior;
        if (e instanceof CuriosityCritter) {
            behavior = critterFactory.createActor(e);
            entities.add(behavior);
        } else if (e instanceof MindMaintenance) {
            behavior = maintenanceFactory.createActor(e);
            entities.add(behavior);
        } else if (e instanceof DreamDweller) {
            behavior = dwellerFactory.createActor(e);
            entities.add(behavior);
        }
        totalEntities++;
    }

    public void update(float dt) {
        for (BehaviorLoop<Enemy> behavior : entities) {
            behavior.update(dt);
        }
    }


}
