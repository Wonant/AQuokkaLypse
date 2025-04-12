package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskConstraint;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

@TaskConstraint(minChildren=0, maxChildren=0)
public class FollowRangeGuard extends LeafTask<Enemy> {

    @Override
    public void start() {
        // No initialization needed.
    }

    @Override
    public Status execute() {
        CuriosityCritter critter = (CuriosityCritter) getObject();
        if(critter.playerInFollowRange) {
            return Status.SUCCEEDED;
        }
        return Status.FAILED;
    }

    @Override
    public void end() {
        // No cleanup needed.
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        return task;
    }
}
