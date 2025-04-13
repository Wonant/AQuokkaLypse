package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import edu.cornell.cis3152.physics.platform.DreamDweller;
import edu.cornell.cis3152.physics.platform.Enemy;

public class CanShootTask extends LeafTask<Enemy> {
    @Override
    public Status execute() {
        if (getObject() instanceof DreamDweller) {
            DreamDweller dweller = (DreamDweller) getObject();
            if (!dweller.isStunned() && dweller.getShotsFired() < dweller.getMaxShots()) {
                return Status.SUCCEEDED;
            }
        }
        return Status.FAILED;
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        return task;
    }
}
