package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.cis3152.physics.platform.Enemy;

public class ShardNearGuard extends LeafTask<Enemy> {
    @TaskAttribute(required = true)
    public boolean random;

    @Override
    public void start() {
        // No initialization needed.
    }

    @Override
    public Status execute() {
        Enemy enemy = getObject();
        if(!enemy.isDreamShardNear()) {
            return Status.FAILED;
        }
        float roll = MathUtils.random(); // float in [0,1)

        if (!random) {
            return Status.SUCCEEDED;
        }

        if (roll < 0.8) {
            return Status.SUCCEEDED;
        } else {
            return Status.FAILED;
        }
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
