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

    private Vector2 pickupTarget;
    private Vector2 dropTarget;
    private boolean hasShard;
    private CuriosityCritter critter;

    @Override
    public void start() {
        hasShard = false;
        if (!(getObject() instanceof CuriosityCritter)) {
            critter = null;
            return;
        }

        critter = (CuriosityCritter)getObject();

        // finding nearest dream shard in scene
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < critter.getScene().getTotalShards(); i++) {
            Vector2 p = critter.getScene().getShardPos(i);

            float d = critter.getObstacle().getPosition().dst(p);
            if (d < bestDist) {
                bestDist = d;
                pickupTarget = new Vector2(p);
            }
        }
        critter.setMovement((critter.getObstacle().getX() < pickupTarget.x) ? -2f : 2f);
        critter.applyForce();
        critter.inMoveTask = true;
    }

    @Override
    public Status execute() {
        if (critter==null) { return Status.FAILED; }

        Vector2 pos = critter.getObstacle().getPosition();

        if (!hasShard) {

            if (critter.heldShard != null) {
                System.out.println("got shard");
                hasShard = true;
                dropTarget = critter.getWorldTarget(); // computed by the give function
                System.out.println("hi" + dropTarget);
                // reverse direction
                critter.setMovement((critter.getObstacle().getX() < dropTarget.x) ? 2f : -2f);
                critter.applyForce();
            }
            return Status.RUNNING;
        } else {
            System.out.println("critter at " + pos);
            critter.setMovement((pos.x < dropTarget.x) ? 2f : -2f);
            critter.applyForce();
            if (pos.epsilonEquals(dropTarget, tolerance)) {
                critter.getScene().spawnShardAtLocation(dropTarget, critter.dropShard());
                return Status.SUCCEEDED;
            }
            return Status.RUNNING;
        }
    }

    @Override
    public void end() {
        // Cleanup if necessary.
        critter.inMoveTask = false;
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        MoveShardTask action = (MoveShardTask) task;
        //action.relocationDuration = relocationDuration;
        return task;
    }
}
