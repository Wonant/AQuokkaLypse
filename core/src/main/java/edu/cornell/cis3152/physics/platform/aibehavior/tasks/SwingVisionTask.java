package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.cis3152.physics.platform.DreamDweller;
import edu.cornell.cis3152.physics.platform.Enemy;

public class SwingVisionTask extends LeafTask<Enemy> {

    public float speed = 0.5f;
    public float range = 90f;

    private float timer = 0f;

    @Override
    public Status execute() {
        if (!(getObject() instanceof DreamDweller)) {
            return Status.FAILED;
        }

        DreamDweller dweller = (DreamDweller) getObject();

        if (dweller.isStunned() || dweller.isAwareOfPlayer()) {
            return Status.FAILED;
        }

        // timer 每次递增 delta (推荐外部更新 timer，这里做模拟)
        timer += 0.016f; // 约等于 60fps 情况下的 deltaTime

        float swing = MathUtils.sin(timer * speed);
        float angle = swing * range;
        dweller.setVisionAngle(angle);

        return Status.SUCCEEDED;
    }

    @Override
    public void start() {
        timer = 0f;
    }

    @Override
    public void reset() {
        timer = 0f;
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        if (!(task instanceof SwingVisionTask)) return task;

        SwingVisionTask copy = (SwingVisionTask) task;
        copy.speed = this.speed;
        copy.range = this.range;
        return copy;
    }
}


