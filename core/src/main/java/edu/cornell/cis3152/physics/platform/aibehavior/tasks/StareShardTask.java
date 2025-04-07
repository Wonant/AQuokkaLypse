package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.cis3152.physics.platform.Enemy;

public class StareShardTask extends LeafTask<Enemy> {
    @TaskAttribute(required = true)
    public float duration;

    @TaskAttribute(required = true)
    public float probability;

    private float elapsed;
    private boolean chanceSucceeded;

    @Override
    public void start() {
        elapsed = 0;
        chanceSucceeded = (MathUtils.random() < probability);
        if(chanceSucceeded) {
            // stare animation, movement zero
        }
    }

    @Override
    public Status execute() {
        if (!chanceSucceeded) {
            return Status.FAILED;
        }
        float dt = Gdx.graphics.getDeltaTime();
        elapsed += dt;
        return (elapsed >= duration) ? Status.SUCCEEDED : Status.RUNNING;
    }

    @Override
    public void end() {
        // Optionally stop the staring animation.
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        StareShardTask stareTask = (StareShardTask) task;
        stareTask.duration = duration;
        stareTask.probability = probability;
        return task;
    }
}
