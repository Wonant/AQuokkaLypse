package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;

public class IdleTask extends LeafTask<CuriosityCritter> {

    @TaskAttribute
    public float speed;

    /** Internal timer tracking how long this task has been running */
    private float elapsed;

    @Override
    public void start() {
        elapsed = 0;
        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            critter.setMovement(speed);
            critter.applyForce();
        }
    }

    @Override
    public Status execute() {
        float dt = Gdx.graphics.getDeltaTime();
        elapsed += dt;

        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            if (!critter.canContinue()) {
                speed = -speed;
                critter.setMovement(speed);
                critter.applyForce();
            } else {
                critter.setMovement(speed);
                critter.applyForce();
            }
        }


        return Status.RUNNING;
    }

    @Override
    public void end() {
        getObject().setMovement(0);
        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            critter.setMovement(0);
            critter.applyForce();
        }
    }

    @Override
    protected Task<CuriosityCritter> copyTo (Task<CuriosityCritter> task) {
        IdleTask walkTask = (IdleTask) task;
        walkTask.speed = speed;
        return task;
    }




}
