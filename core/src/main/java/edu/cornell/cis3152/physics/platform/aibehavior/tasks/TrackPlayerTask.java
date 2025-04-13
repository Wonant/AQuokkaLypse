package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.platform.DreamDweller;
import edu.cornell.cis3152.physics.platform.Enemy;
import edu.cornell.cis3152.physics.platform.Player;

public class TrackPlayerTask extends LeafTask<Enemy> {

    @Override
    public Status execute() {
        if (!(getObject() instanceof DreamDweller)) {
            return Status.FAILED;
        }

        DreamDweller dweller = (DreamDweller) getObject();

        if (!dweller.isAwareOfPlayer() || dweller.isStunned()) {
            return Status.FAILED;
        }

        Player player = dweller.getTarget();
        if (player == null) return Status.FAILED;

        Vector2 playerPos = new Vector2(player.getObstacle().getX(), player.getObstacle().getY());
        Vector2 headPos = dweller.getHeadBody().getPosition();

        float dx = playerPos.x - headPos.x;
        float dy = playerPos.y - headPos.y;
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees - 90;

        dweller.setVisionAngle(angle);
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        return task;
    }
}

