package edu.cornell.cis3152.physics.platform.aibehavior.tasks;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;
import edu.cornell.cis3152.physics.platform.Player;

//staring and following the player, as long as in sensor range
public class StareTask extends LeafTask<Enemy> {

    @TaskAttribute(required = true)
    public float followSpeed;

    private float slowFactor;

    private float elapsed;

    @Override
    public void start() {
        elapsed = 0;
        CuriosityCritter critter = (CuriosityCritter) getObject();
        critter.setInAttackAnimation(true);
    }

    @Override
    public Status execute() {
        CuriosityCritter critter = (CuriosityCritter) getObject();

        // Check if the player is still in follow range.
        if (!critter.playerInFollowRange) {
            return Status.SUCCEEDED;
        }

        float critterX = critter.getObstacle().getX();
        float playerX = critter.getScene().getAvatar().getObstacle().getX();

        float movement = Math.abs(playerX - critterX) > 1.5 ? playerX > critterX ? followSpeed : -followSpeed : 0;
        critter.setMovement(movement);
        critter.applyForce();

        return Status.RUNNING;
    }

    @Override
    public void end() {
        getObject().setMovement(0);
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        StareTask action = (StareTask) task;
        action.followSpeed = followSpeed;
        return task;
    }
}
