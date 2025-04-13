package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import edu.cornell.cis3152.physics.platform.DreamDweller;
import edu.cornell.cis3152.physics.platform.Enemy;

public class ShootSpearTask extends LeafTask<Enemy> {

    public int maxShots = 3;
    public float cooldown = 0.5f; // optional, unused here

    @Override
    public Status execute() {
        if (!(getObject() instanceof DreamDweller)) {
            return Status.FAILED;
        }
        DreamDweller dweller = (DreamDweller) getObject();
        if (dweller.isStunned()) {
            return Status.FAILED;
        }

        if (dweller.getShotsFired() >= maxShots) {
            dweller.setShooting(false);
            dweller.resetShotsFired();
            return Status.FAILED;
        }

        dweller.setShooting(true);
        dweller.incrementShotsFired();
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        ShootSpearTask copy = (ShootSpearTask) task;
        copy.maxShots = this.maxShots;
        copy.cooldown = this.cooldown;
        return copy;
    }
}
