package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

public class AlertedTask extends LeafTask<Enemy> {

    @TaskAttribute
    public float duration;  // Duration of the alert animation.

    private float elapsed;

    @Override
    public void start() {
        elapsed = 0;

        if(getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            // play critter alerted anikmation/behavior
        }
    }

    @Override
    public Status execute() {
        float dt = Gdx.graphics.getDeltaTime();
        elapsed += dt;
        return (elapsed >= duration) ? Status.SUCCEEDED : Status.RUNNING;
    }

    @Override
    public void end() {
        // Cleanup after the alert if necessary.
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        AlertedTask alerted = (AlertedTask) task;
        alerted.duration = duration;
        return task;
    }

}
