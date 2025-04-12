package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

import java.util.Random;

public class StareShardTask extends LeafTask<Enemy> {
    @TaskAttribute(required = true)
    public float duration;


    private float elapsed;


    @Override
    public void start() {
        elapsed = 0;

        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            critter.setMovement(0);
            critter.applyForce();
        }

    }

    @Override
    public Status execute() {
        float dt = GdxAI.getTimepiece().getDeltaTime();
        elapsed += dt;
        System.out.println("Staring at shard for: " + elapsed);
        return (elapsed >= duration) ? Status.SUCCEEDED : Status.RUNNING;
    }

    @Override
    public void end() {

    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        StareShardTask stareTask = (StareShardTask) task;
        stareTask.duration = duration;
        return task;
    }
}
