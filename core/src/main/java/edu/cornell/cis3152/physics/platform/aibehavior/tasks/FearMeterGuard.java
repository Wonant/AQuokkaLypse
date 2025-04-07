package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import edu.cornell.cis3152.physics.platform.Enemy;

public class FearMeterGuard extends LeafTask<Enemy> {
    @Override
    public void start() {
        // No initialization needed.
    }

    @Override
    public Status execute() {
        // Replace the stub with your actual check.
        if(getObject().getScene().getAvatar().getFearMeter() > getObject().getScene().getAvatar().getMaxFearMeter() / 2) {
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
