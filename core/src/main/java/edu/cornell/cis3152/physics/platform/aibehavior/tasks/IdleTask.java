package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;

public class IdleTask extends LeafTask<CuriosityCritter> {

    @Override
    public void start() {

    }

    @Override
    public Status execute() {

        return status.RUNNING;
    }

    @Override
    public void end() {

    }

    @Override
    protected Task<CuriosityCritter> copyTo (Task<CuriosityCritter> task) {
        return task;
    }




}
