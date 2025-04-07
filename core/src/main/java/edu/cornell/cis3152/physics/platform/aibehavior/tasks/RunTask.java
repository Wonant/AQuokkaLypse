package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

public class RunTask extends LeafTask<Enemy> {
    @TaskAttribute(required = true)
    public float duration;  // Duration of the run.

    @TaskAttribute(required = true)
    public float runSpeed;  // The run speed (direction may be determined externally).

    private float elapsed;

    @Override
    public void start() {
        elapsed = 0;
        CuriosityCritter critter = (CuriosityCritter) getObject();
        critter.setMovement(runSpeed);
        critter.applyForce();
    }

    @Override
    public Status execute() {
        float dt = Gdx.graphics.getDeltaTime();
        elapsed += dt;
        return (elapsed >= duration) ? Status.SUCCEEDED : Status.RUNNING;
    }

    @Override
    public void end() {
        getObject().setMovement(0);
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        RunTask runAway = (RunTask) task;
        runAway.duration = duration;
        runAway.runSpeed = runSpeed;
        return task;
    }
}
