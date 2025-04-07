package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

public class MoveShardTask extends LeafTask<Enemy> {

    @TaskAttribute(required = true)
    public float tolerance;

    private float elapsed;

    @Override
    public void start() {
        elapsed = 0;

        if(getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            // critter move shard method here
        }
    }

    @Override
    public Status execute() {
        if(getObject() instanceof CuriosityCritter) {
            CuriosityCritter critter = (CuriosityCritter) getObject();
            // Check if the critter is carrying a shard.
            if(critter.heldShard == null) {
                return Status.FAILED;
            }
            // Get the current position of the shard.
            Vector2 shardPos = critter.getObstacle().getPosition();
            // Compare with the critter's desired target position.
            Vector2 target = critter.getWorldTarget();
            if(shardPos.epsilonEquals(target, tolerance)) {
                return Status.SUCCEEDED;
            }
            return Status.RUNNING;
        }
        return Status.FAILED;
    }

    @Override
    public void end() {
        // Cleanup if necessary.
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        MoveShardTask action = (MoveShardTask) task;
        //action.relocationDuration = relocationDuration;
        return task;
    }
}
