package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.DreamDweller;
import edu.cornell.cis3152.physics.platform.Enemy;

public class StunnedGuardTask extends LeafTask<Enemy> {
    @Override
    public void start(){

    }

    @Override
    public Status execute() {
        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter c = (CuriosityCritter) getObject();
            return c.isStunned() ? Status.SUCCEEDED : Status.FAILED;
        }
        else if(getObject() instanceof DreamDweller){
            DreamDweller d = (DreamDweller) getObject();
            return d.isStunned() ? Status.SUCCEEDED :Status.FAILED;
        }
        return Status.FAILED;
    }
    @Override
    public void end() {
        // Optional cleanup logic, if needed
    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        // Because there are no fields to copy, returning task is enough
        return task;
    }
}
